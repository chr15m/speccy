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
  (/ 60 bpm))

(defn bpm-timeout [player]
  (* (bpm-to-period (* (player :bpm) (player :sub-ticks)))))

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

(def instrument-defaults 
  {:wave_type 2

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

(defn make-source [wave actx]
  (let [wave-buffer (-> wave .-buffer)
        buffer-length (.-length wave-buffer)
        buffer (.createBuffer actx 1 buffer-length (-> wave .-header .-sampleRate))
        buffer-writer (.getChannelData buffer 0)
        proc (.createBufferSource actx)]
    (.set buffer-writer wave-buffer)
    (aset proc "buffer" buffer)
    (.connect proc (.-destination actx))
    proc))

(defn generate-sound [definition]
  (-> definition
    (clj->js)
    (js/SoundEffect.)
    (.generate)))

(def generate-sound-mem (memoize generate-sound))

(defn evaluate-instrument [actx t instrument-definition]
  (let [result (instrument-definition t)]
    (when result
      (->
        (merge instrument-defaults
               result)
        (generate-sound-mem)
        (make-source actx)))))

(defn time-and-evaluate-instrument [])

(defn generate-samples [actx player tick]
  (doall
    (remove nil?
            (map
              (partial evaluate-instrument actx tick)
              (@player :instruments)))))

(def look-ahead 1)

(defn play [player]
  (go
    (let [actx (when (aget js/window "AudioContext") (js/window.AudioContext.))]
      (print "play")
      (loop [tick 0 last-tick-time 0]
        ;(<! (timeout-worker (* 1000 (bpm-timeout @player))))
        (<! (timeout-worker 10))
        (let [next-tick (+ last-tick-time (bpm-timeout @player))
              t (.-currentTime actx)
              next-tick-time (+ (bpm-timeout @player) last-tick-time)
              needs-update (> (+ t look-ahead) next-tick-time)
              new-tick-time (if needs-update next-tick-time last-tick-time)
              new-tick (if (and needs-update (@player :playing)) (inc tick) tick)
              samples (if (and needs-update (@player :playing)) (generate-samples actx player new-tick) [])]
          
          ;(print t (+ t look-ahead) next-tick-time)
          ;(print needs-update)

          ;(print t needs-update tick (count samples))
          ;(print (- (+ t look-ahead) next-tick-time))
          ;(print needs-update next-tick-time)

          ;(print "Audio time:" t)
          ;(print "Since:" (- t last-tick-time))
          ;(print tick (count samples))
          (doseq [s samples]
            ;(js/console.log s)
            (.start s next-tick-time))
          (recur new-tick new-tick-time)))))
  player)

(defn singleton [bpm]
  (let [player (make-player bpm)]
    [(play player) (partial add-instrument! player) (partial clear! player)]))

