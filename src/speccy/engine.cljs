(ns speccy.engine
  (:require [cljs.core.async :refer [chan <! close!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;; -------------------------
;; Audio engine

(defn make-player [bpm]
  (atom {:instruments []
         :bpm bpm
         :sub-ticks 2
         :playing true}))

(defn bpm-to-period [bpm]
  (/ 60000 180))

(defn add-instrument! [player instrument-definition]
  (swap! player update-in [:instruments] conj instrument-definition))

(defn clear! [player]
  (swap! player assoc :instruments []))

; uses a webworker to run ticks even on a backgrounded tab
(let [metronome-worker-js "self.onmessage=function(e){setTimeout(function(){postMessage(e.data);},e.data.interval);};console.log('Metronome worker loaded.');"
      worker-blob (js/Blob. (clj->js [metronome-worker-js]) {:type "application/javascript"})
      worker (js/Worker. (.createObjectURL js/URL worker-blob))
      call-id (atom 0)]

  (defn make-worker-listener [id callback]
    (fn [e]
      (when (= e.data.id id)
        (callback)
        true)))

  (defn schedule-tick [callback interval]
    (let [id (swap! call-id inc)
          listener-fn (make-worker-listener id callback)]
      (.addEventListener worker
                         "message"
                         (fn [e]
                           (when (listener-fn e) (.removeEventListener worker "message" listener-fn)))
                         false)
      (.postMessage worker (clj->js {:id id :interval interval}))))

  ; this emulates async's timeout but in background worker thread
  (defn timeout-worker [interval]
    (let [c (chan)]
      (schedule-tick (fn [] (close! c)) interval)
      c)))

(def instrument-defaults {:wave_type 2

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

(defn generate-sound [definition]
  (-> definition
    (clj->js)
    (js/SoundEffect.)
    (.generate)
    (.getAudio)))

(def generate-sound-mem (memoize generate-sound))

(defn evaluate-instrument [t instrument-definition]
  (let [result (instrument-definition t)]
    (when result
      (time
        (->
          (merge instrument-defaults
                 result)
          (generate-sound-mem))))))

(defn play [player]
  (go
    (let [actx (when (aget js/window "AudioContext") (js/window.AudioContext.))]
      (print "play")
      (loop [samples [] tick 0]
        (<! (timeout-worker (* (bpm-to-period (* (@player :bpm) (@player :sub-ticks))))))
        (print tick (count samples))
        (doseq [s samples]
          (.play s))
        (if (@player :playing)
          (recur
            (doall
              (remove nil? (map (partial evaluate-instrument tick) (@player :instruments))))
            (inc tick))
          (recur [] tick)))))
  player)

(defn singleton [bpm]
  (let [player (make-player bpm)]
    [(play player) (partial add-instrument! player) (partial clear! player)]))

