(ns speccy.core
  (:require [reagent.core :as reagent]
            [speccy.engine :refer [looper scheduler player catch-background-tab loop-defaults]]
            [speccy.scratch]))

(defn remove-defaults [m]
  (into {}
        (remove nil?
                (map
                  (fn [k]
                    (let [v (m k)]
                      (if (and v (not= v (loop-defaults k))) 
                        [k v])))
                  (keys loop-defaults)))))

(defn json-to-edn [v]
  (-> v
      (js/JSON.parse)
      (js->clj :keywordize-keys true)
      (remove-defaults)
      (str)
      (clojure.string/replace "," "\n")))

;; -------------------------
;; Views

(defn home-page []
  (let [result (reagent/atom "")]
    (fn []
      [:div
       [:img {:src "img/speccy.png"}]
       [:textarea {:rows 36
                   :placeholder "paste json sfxr def here"
                   :on-change #(reset! result (json-to-edn (-> % .-target .-value)))}]
       [:pre @result]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (print "mount")
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (print "init")
  (mount-root))
