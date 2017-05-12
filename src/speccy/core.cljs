(ns speccy.core
    (:require [reagent.core :as reagent]))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to Reagent"]])

(defonce t (atom 0))

(defn make-sound []
  (js/console.time "make-sound")
  (let [sound {:p_env_decay 0.1
               :p_env_sustain 0.1
               :p_lpf_ramp 0
               :p_duty_ramp 0
               :oldParams true
               :p_freq_limit 0
               :sample_rate 44100
               :p_hpf_ramp 0
               :p_freq_dramp 0
               :p_pha_offset 0
               :p_arp_speed 0
               :p_arp_mod 0
               :p_freq_ramp 0.1
               :p_pha_ramp 0
               :p_duty 1
               :p_lpf_resonance 0
               :p_repeat_speed 0.9
               :p_env_attack 0
               :p_lpf_freq 1
               :sample_size 8
               :sound_vol (get [0.1 0 0 0.1] (mod @t 4))
               :wave_type 2
               :p_vib_strength 0
               :p_base_freq (+ 0.2 (/ (mod @t 8) 16)) ;0.26126191208337196
               :p_env_punch 0
               :p_hpf_freq 0
               :p_vib_speed 0}]
    (js/console.timeEnd "make-sound")
    (-> sound (clj->js) (js/SoundEffect.) (.generate) (.getAudio))))

(defonce prev (atom nil))

(defonce player
  (js/setInterval
    (fn []
      (if @prev
        (.play @prev))
      (reset! prev (make-sound))
      (swap! t inc))
    500))

;(js/clearInterval player)

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
