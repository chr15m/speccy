(ns speccy.core
  (:require [reagent.core :as reagent]
            [speccy.engine :refer [looper scheduler player catch-background-tab]]
            [speccy.scratch]))

;; -------------------------
;; Views

(defn home-page []
  [:div
   [:img {:src "img/speccy.png"}]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (print "mount")
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (print "init")
  (-> scheduler (.start player))
  (catch-background-tab scheduler)
  (mount-root))
