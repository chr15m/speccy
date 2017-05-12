(ns speccy.core
  (:require [reagent.core :as reagent]
            [cljs.core.async :refer [<! chan close! put! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; -------------------------
;; Views

(defn home-page []
  [:div
   [:img {:src "img/speccy.png"}]])

;; -------------------------
;; Audio engine

(defonce actx (when (aget js/window "AudioContext") (js/window.AudioContext.)))
(defonce scheduler (js/WebAudioScheduler. {:context actx}))

(def period (/ 60 180))

(def loops (atom []))

(defn looper [instrument-definition]
  (swap! loops conj instrument-definition))

(defn catch-background-tab [scheduler]
  (.addEventListener js/document "visibilitychange"
                     (fn [ev]
                       (print (.-visibilityState js/document))
                       (if (= (.-visibilityState js/document) "visible")
                         (set! (.-aheadTime scheduler) 0.1)
                         (do
                           (set! (.-aheadTime scheduler) 2.0)
                           (.process scheduler)))
                       (print "aheadTime" (.-aheadTime scheduler)))))

(def loop-defaults {:wave_type 2

                    :p_env_attack 0
                    :p_env_decay 0
                    :p_env_sustain 0.116
                    :p_env_punch 0 

                    :p_base_freq 0.35173364

                    :p_freq_limit 0
                    :p_freq_ramp 0
                    :p_freq_dramp 0

                    :p_lpf_freq 1
                    :p_lpf_resonance 0
                    :p_lpf_ramp 0

                    :p_duty 0
                    :p_duty_ramp 0

                    :p_hpf_freq 0 
                    :p_hpf_ramp 0

                    :p_pha_offset 0
                    :p_pha_ramp 0

                    :p_arp_speed 0
                    :p_arp_mod 0

                    :p_vib_strength 0
                    :p_vib_speed 0

                    :p_repeat_speed 0

                    :sample_size 8
                    :sound_vol 0.25
                    :sample_rate 44100
                    :oldParams true})

(defn evaluate-loop [t loop-definition]
  (let [result (loop-definition t)]
    (when result
      (->
        (merge loop-defaults
               result)
        (clj->js)
        (js/SoundEffect.)
        (.generate)
        (.getAudio)))))

(defn player [e]
  (let [t (.-playbackTime e)
        args (or (.-args e) {})
        samples (or (args :samples) [])
        tick (or (args :tick) 0)]
    (print t tick (count samples))
    (doseq [s samples]
      (.play s))
    (-> scheduler (.insert
                    (+ t period)
                    player
                    {:samples
                     (doall
                       (remove nil? (map (partial evaluate-loop tick) @loops)))
                     :tick (inc tick)}))))

(looper (fn [t] {:p_env_decay 0.1
                 :p_env_sustain 0.1 ;([0.1 0.5] (mod (mod t 13) 2))
                 :p_freq_ramp 0.1 ;([0.1 0.8] (mod (mod t 13) 2))
                 :p_duty 0.5
                 :p_repeat_speed 0.9
                 :sound_vol (get [0.2 0.1 0 0.1] (mod t 4))
                 :wave_type 2 ;(mod (mod t 27) 4)
                 :p_base_freq (+ 0.2 (/ (mod t 8) 16))}))

;(-> scheduler (.start player))
;(-> scheduler (.stop true))

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
