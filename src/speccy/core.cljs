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

; TODO:
;
; * key combo to insert 0x00
; * mobile friendly UI
;
; * note shortcuts like :C#4 and :A-2 (need octaves in there)
; * better error handling - figwheel like popup
; * fix error handling - catch runtime errors inside fns
; * fix missing fonts
; * note pairs (wave-lookup/key-table can probably pass back multiple key vals - test it)
;   or just accept [60 127] pairs at inst level?
; * key combo to insert --- (notes)
; * piano note insert functionality (alt-KEY to insert piano note)
;
; * UI to mute channels quickly (at the webaudio graph layer)
; * in-codemirror feedback to show position in sq/at being currently processed
; * multiplayer webrtc lol
;
; * macro for invoking instruments with player & time inserted
; * macros to fold up adding with less chars

(defonce editor-content (atom ""))

(def is-mac?
  (>= (.indexOf (aget js/navigator "userAgent") "Macintosh") 0))

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

(defn eval-str [cm-code]
  (let [compiler-state (empty-state)
        parsed-code (read-string (str "(do\n" cm-code "\n)"))]
    (print "Code to be run:")
    (print parsed-code)
    (eval compiler-state
          '(do
             (ns cljs.user)
             (require '[speccy.engine :refer [singleton make-player play clear! add-instrument! at sq zz seed rnd -- --- ---- C C- C# D D- D# E E- F F- F# G G- G# A A- A# B B-] :as sp])
             (defonce player (play (make-player 180)))
             (def sfxr (partial add-instrument! player))
             (clear! player))
          {:eval js-eval
           :context :expr}
          (partial print "player-state"))
    (try
      (eval compiler-state
            parsed-code
            {:eval js-eval
             :source-map true
             :context :expr}
            (fn [result] result))
      (catch :default e (js/alert e)))))

(defn send-it [cm]
  (let [content (.getValue cm)]
    (reset! editor-content content)
    (.setItem js/localStorage "speccy-editor" content)
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
     [:div#info
      [:p (if is-mac? "cmd" "ctrl") "-S to run"]
      [:p [:a {:href "http://sfxr.me/" :target "_new"} "sfxr.me"] " (sounds)"]
      [:p [:a {:href "https://github.com/chr15m/speccy/#quick-start"} "documentation"]]]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (print "mount")
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (print "init")
  (reset! editor-content (or (.getItem js/localStorage "speccy-editor") ""))
  (mount-root))
