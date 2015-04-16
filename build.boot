(set-env!
 :source-paths    #{"src/cljs" "less" "src/clj"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs      "0.0-2814-0" :scope "test"]
                 [adzerk/boot-cljs-repl "0.1.9"      :scope "test"]
                 [adzerk/boot-reload    "0.2.4"      :scope "test"]
                 [pandeiro/boot-http    "0.6.1"      :scope "test"]
                 [org.omcljs/om "0.8.6"]
                 [sablono "0.3.4"]
                 [boot-garden "1.2.5-1" :scope "test"]
                 [deraen/boot-less "0.2.1" :scope "test"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 ])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[boot-garden.core    :refer [garden]]
 '[deraen.boot-less    :refer [less]])

(deftask build []
  (comp (speak)
        (cljs)
        (garden :styles-var 'my-tenzing-app.styles/screen
:output-to "css/garden.css")
        (less)
        (sift   :move {#"less.css" "css/less.css" #"less.main.css.map" "css/less.main.css.map"})))

(deftask run []
  (comp (serve)
        (watch)
        (cljs-repl)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced
                       ;; pseudo-names true is currently required
                       ;; https://github.com/martinklepsch/pseudo-names-error
                       ;; hopefully fixed soon
                       :pseudo-names true}
                      garden {:pretty-print false}
                      less   {:compression true})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none
                       :unified-mode true
                       :source-map true}
                 reload {:on-jsload 'my-tenzing-app.app/init}
                      less   {:source-map  true})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))
