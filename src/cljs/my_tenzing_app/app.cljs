(ns my-tenzing-app.app
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [>! <! chan]]))

(def app-model
  (atom
   {
    ;; Parent component
    :user "alice"
    :room "Company-wide chat"

    :friends [{:user "bob" :image "bob.jpg"}
              {:user "carl" :image "carl.jpg"}]

    :messages-panel
    {:messages [{:from "alice" :message "how are you?"}
                {:from "bob" :message "I'm fine thanks!"}]}

    :input-panel
    {:message-being-typed ""}

    }))

;; See all the messages so far

(defn messages-component [data owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (when-let [c (om/get-state owner :channel)]
        (go-loop []
          (when-let [msg (<! c)]
            (om/transact! data :messages (fn [old] (conj old msg)))
            )
          (recur))))

    om/IRender
    (render [this]
      (html
       [:div
        [:h2 "Messages"]
        [:table
         (for [{:keys [from message]} (reverse (take 5 (reverse (:messages data))))]
           [:tr
            [:td (str from ">")]
            [:td message]])]
        ]))))

;; Now for allowing the user to send messages

(defn send-message [data owner]
  (let [msg {:from "me"
             :message (:message-being-typed @data)}]
    (go (>! (om/get-state owner :channel) msg)))

  (om/update! data :message-being-typed "")
  (.focus (. js/document (getElementById "myinput"))))

(defn input-component [data owner]
  (reify
    om/IRender
    (render [this]
      (html
       [:div
        [:p "Please enter your message:"]
        [:input#myinput {:type "text"
                         :value (:message-being-typed data)
                         :on-key-down (fn [ev]
                                        (when (= (.-keyCode ev) 13)
                                          (send-message data owner)))

                         :on-change
                         (fn [ev]
                           (let [value (.-value (.-target ev))]
                             (om/update! data :message-being-typed value)))}]

        [:button {:on-click (fn [_]
                              (send-message data owner))}
         "Send!"]
        ]))))

(defn chatapp
  "This parent component is also responsible for receiving messages from the chat server"
  [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:channel (chan 10)})

    om/IWillMount
    (will-mount [this]
      (let [es (new js/EventSource "http://localhost:3001/events/events")]
        (.addEventListener
         es "message"
         (fn [ev]
           (go (>! (om/get-state owner :channel)
                   {:from "someone" :message (.-data ev)}))
           ))
        (om/set-state! owner :es es)))

    om/IWillUnmount
    (will-unmount [this]
      (when-let [es (om/get-state owner :es)]
        (.close es)))

    om/IRender
    (render [this]
      (html
       [:div
        [:h1 "Hello " (:user data)]
        (om/build messages-component (:messages-panel data) {:state {:channel (om/get-state owner :channel)}})
        (om/build input-component (:input-panel data) {:state {:channel (om/get-state owner :channel)}})
        ]))))


(defn init []
  (om/root chatapp app-model
           {:target (. js/document (getElementById "container"))}))
