(ns my-tenzing-app.app
  (:use-macros [cljs.core.async.macros :refer (go go-loop)])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer (chan >! <!)]))

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

(defn messages-component [data owner]
  (reify
    om/IRender
    (render [this]
      (html
       [:div
        [:h2 "Messages"]
        [:table
         (for [{:keys [from message]} (:messages data)]
           [:tr
            [:td (str from ">")]
            [:td message]])]
        ]))))

(defn input-component [data owner]
  (reify
    om/IRender
    (render [this]
      (html
       [:div
        [:p "Plesae enter you r message:"]
        [:input {:type "text"
                 :value (:message-being-typed data)
                 :on-change
                 (fn [ev]
                   (let [value (.-value (.-target ev))]
                     (.log js/console value)
                     (om/update! data :message-being-typed value)))}
         ]
        [:button {:on-click (fn [_])} "Send!"]
        ]))))

(defn chatapp [data owner]
  (reify
    om/IRender
    (render [this]
      (html
       [:div
        [:h1 "Hello " (:user data)]
        (om/build messages-component (:messages-panel data))
        (om/build input-component (:input-panel data))
        ]))))


(defn init []
  (om/root chatapp app-model
           {:target (. js/document (getElementById "container"))}))
