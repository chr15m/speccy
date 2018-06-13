(ns speccy.core
  (:require [reagent.core :as reagent]
            [cljsjs.codemirror]
            [cljsjs.codemirror.mode.clojure]
            [cljsjs.codemirror.addon.edit.matchbrackets]
            [cljsjs.codemirror.addon.edit.closebrackets]
            [cljsjs.parinfer]
            [cljsjs.parinfer-codemirror]
            [cljs.tools.reader :refer [read-string]]
            [cljs.js :refer [empty-state eval js-eval]]
            [speccy.engine :refer [instrument-defaults]]
            ;[speccy.scratch]
            ))

(defonce editor-content (atom ""))

(defn remove-defaults [m]
  (into {}
        (remove nil?
                (map
                  (fn [k]
                    (let [v (m k)]
                      (if (and v (not= v (instrument-defaults k))) 
                        [k v])))
                  (keys instrument-defaults)))))

(defn json-to-edn [v]
  (-> v
      (js/JSON.parse)
      (js->clj :keywordize-keys true)
      (remove-defaults)
      (str)
      (clojure.string/replace "," "\n")))

(defn beep [b]
  (print "Beep:" b))

(defn eval-str [s]
  (eval ;(empty-state)
        {'beep beep}
        (read-string s)
        {:eval js-eval
         :source-map true
         :context :expr}
        (fn [result] result)))

(defn send-it [cm]
  (js/console.log (.getValue cm))
  (eval-str (str "(do" (.getValue cm) ")")))

(defn component-codemirror []
  (reagent/create-class
    {:component-did-mount (fn [component]
                            (let [cm (js/CodeMirror (reagent/dom-node component)
                                                    (clj->js
                                                      {:lineNumbers true
                                                       :matchBrackets true
                                                       :autofocus true
                                                       :extraKeys {"Ctrl-S" send-it
                                                                   "Enter" false}
                                                       ;:value @value-atom
                                                       :theme "erlang-dark"
                                                       :autoCloseBrackets true
                                                       :mode "clojure"}))]
                              (js/parinferCodeMirror.init cm)))
     :reagent-render (fn [] [:div#editor])}))

;; -------------------------
;; Views

(defn home-page []
  (fn []
    [:div
     [component-codemirror]
     [:p#synthlink "Get synth defs from " [:a {:href "http://sfxr.me/" :target "_new"} "sfxr.me"]]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (print "mount")
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (print "init")
  (mount-root))
