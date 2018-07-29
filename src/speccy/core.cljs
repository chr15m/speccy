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
            [speccy.engine :refer [instrument-defaults]]))

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

(defn eval-str [s]
  (let [compiler-state (empty-state)]
    (eval compiler-state
          '(do
             (require '[speccy.engine :refer [singleton sequencer make-player play clear! add-instrument!]])
             (defonce player (play (make-player 180)))
             (clear! player))
          {:eval js-eval
           :context :expr}
          (partial print "player-state"))
    (eval compiler-state
          (read-string s)
          {:eval js-eval
           :source-map true
           :context :expr}
          (fn [result] result))))

(defn send-it [cm]
  (let [content (.getValue cm)]
    (reset! editor-content content)
    (try
      (eval-str content)
      (catch :default e (js/alert e)))))

(aset (.-commands js/CodeMirror) "save" send-it)

(defn component-codemirror []
  (reagent/create-class
    {:component-did-mount (fn [component]
                            (let [cm (js/CodeMirror (reagent/dom-node component)
                                                    (clj->js
                                                      {:lineNumbers true
                                                       :matchBrackets true
                                                       :autofocus true
                                                       :value @editor-content
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
