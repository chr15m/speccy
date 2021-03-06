(ns speccy.engine
  (:require [cljs.core.async :refer [chan <! close!]]
            [speccy.webaudio :as audio]
            sfxr
            riffwave
            seedrandom)
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;; -------------------------
;; Utility fns

(defn float-to-frequency [f]
  (+ (* f f 441 8) 0.001))

(defn frequency-to-float [f]
  (js/Math.sqrt (/ (- f 0.001) (* 8 441))))

; taken from pure-data
(defn mtof [m]
  (cond
    (<= m -1500) 0
    (> m 1499) (mtof 1499)
    :else (* (js/Math.exp (* m .0577622650)) 8.17579891564)))

(defn midi-note-to-float-convert [n m]
  [n (when (> m 0) (frequency-to-float (mtof m)))])

(defn frequency-to-float-convert [n f]
  [n (when (> f 0) (frequency-to-float f))])

(defn volume-float-or-int-convert [n f]
  [n (when (> f 0) (if (> f 1) (/ f 127) f))])

(defn wave-lookup [v]
  [:wave_type (or ({:square 0 :saw 1 :sine 2 :noise 3 :sq 0 :sw 1 :sn 2 :ns 4} v) v)])

(def key-table
  {:wave wave-lookup
   :w :wave

   :note (partial midi-note-to-float-convert :p_base_freq)
   :n :note
   :volume (partial volume-float-or-int-convert :sound_vol)
   :vol :volume
   :v :volume

   :env/attack :p_env_attack
   :env/decay :p_env_decay
   :env/sustain :p_env_sustain
   :env/punch :p_env_punch

   :e/a :p_env_attack
   :e/d :p_env_decay
   :e/s :p_env_sustain
   :e/p :p_env_punch

   :frequency (partial frequency-to-float-convert :p_base_freq)
   :frequency/limit :frequency
   :frequency/ramp :p_freq_ramp
   :frequency/ramp-delta :p_freq_dramp

   :lpf/frequency (partial frequency-to-float-convert :p_lpf_freq)
   :lpf/note (partial midi-note-to-float-convert :p_lpf_freq)
   :lpf/resonance :p_lpf_resonance
   :lpf/ramp :p_lpf_ramp

   :hpf/frequency (partial frequency-to-float-convert :p_hpf_freq)
   :hpf/note (partial midi-note-to-float-convert :p_hpf_freq)
   :hpf/ramp :p_hpf_ramp

   :phase/offset :p_pha_offset
   :phase/ramp :p_pha_ramp

   :arp/speed :p_arp_speed
   :arp/mod :p_arp_mod

   :vibrato/strength :p_vib_strength
   :vibrato/speed :p_vib_speed

   :duty :p_duty
   :duty/ramp :p_duty_ramp

   :retrigger :p_repeat_speed
   :retrig :p_repeat_speed
   :rt :p_repeat_speed})

(defn instrument-key-lookups [t [k v]]
  (if (fn? v)
    (instrument-key-lookups t [k (v t)])
    (let [lookup (key-table k)]
      (if lookup
        (if (fn? lookup)
          (lookup v)
          (if (key-table lookup)
            (instrument-key-lookups t [lookup v])
            [lookup v]))
        [k v]))))

(defn filter-instrument-keys [instrument-result t]
  (let [instrument-processed-tuples (map (fn [kv] (let [[kd vd] (instrument-key-lookups t kv)]
                                                    (if (or (not= vd (instrument-result kd)) (nil? vd))
                                                      [kd vd]
                                                      nil)))
                                         instrument-result)
        instrument-filtered-tuples (remove nil? instrument-processed-tuples)
        instrument-filtered (into {} instrument-filtered-tuples)
        instrument-updated (merge instrument-result instrument-filtered)]
    ;(print instrument-processed-tuples)
    ;(print instrument-filtered)
    ;(print (instrument-result :p_base_freq))
    ;(print (instrument-updated :p_base_freq))
    instrument-updated))

(defn from-b58 [s]
  (if s
    (js->clj ((aget js/sfxr "b58decode") s) :keywordize-keys true)
    {}))

(def from-b58-mem (memoize from-b58))

(defn default-from-b58 [instrument-result]
  (let [d (instrument-result :default)]
    (from-b58-mem d)))

;; -------------------------
;; Audio engine

(defn make-player [bpm]
  (atom {:instruments []
         :bpm bpm
         :sub-ticks 2
         :playing true
         :actx (audio/get-context)}))

(defn bpm-to-period [bpm]
  (/ 60 bpm))

(defn bpm-timeout [player]
  (* (bpm-to-period (* (player :bpm) (player :sub-ticks)))))

(defn add-instrument! [player & instrument-definition]
  (swap! player update-in [:instruments] conj instrument-definition))

(defn clear! [player]
  (swap! player assoc :instruments []))

(defn pause! [player]
  (swap! player assoc :playing false))

(defn play! [player]
  (swap! player assoc :playing true))

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
    proc))

(defn generate-sound [definition]
  (-> definition
    (clj->js)
    (js/SoundEffect.)
    (.generate)))

(def generate-sound-mem (memoize generate-sound))

(defn process-single-instrument [i t]
  (cond
    (fn? i) (i t)
    (string? i) (from-b58-mem i)
    (map? i) i
    ; TODO: feedback for the user here
    :else {}))

(defn printstrument [p]
  (print "prinstrument:" p)
  p)

(defn evaluate-instrument [actx t instrument-definition]
  ;(print "instrument-definition" instrument-definition)
  (let [instrument-results (map #(process-single-instrument % t) instrument-definition)
        ; TODO: memoize here?
        ;_ (print "instrument-results:" instrument-results)
        result (apply merge instrument-results)
        result (when result (merge instrument-defaults
                                   (default-from-b58 result)
                                   (filter-instrument-keys result t)))
        exists (and result (> (result :sound_vol) 0) (> (result :p_base_freq) 0))]
    ;(print result)
    (when exists
      (->
        result
        ;(printstrument)
        (generate-sound-mem)
        (make-source actx)
        (audio/connect actx (result :bus))))))

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
    (let [actx (@player :actx)]
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
    [(play player) (partial add-instrument! player) (partial clear! player) (partial pause! player) (partial play! player)]))

;; -------------------------
;; User helper fns

(defn sq [t s]
  (get s (mod t (count s))))

(defn at [t l s]
  (get s (mod t l)))

(defn zz [f]
  (fn [& args]
    (when (not (some #(= nil %) args))
      (apply f args))))

(defn seed [x]
  (js/Math.seedrandom x))

(defn rnd [x]
  (let [r (js/Math.random)]
    (cond (vector? x) (nth x (* r (count x)))
          (number? x) (* x r)
          :else r)))

(defn prt [& args]
  (apply print args)
  (last args))

(def -- nil)
(def --- nil)
(def ---- nil)

(def C 60)
(def C- 60)
(def C# 61)
(def D 62)
(def D- 62)
(def D# 63)
(def E 64)
(def E- 64)
(def F 65)
(def F- 65)
(def F# 66)
(def G 67)
(def G- 67)
(def G# 68)
(def A 69)
(def A- 69)
(def A# 70)
(def B 71)
(def B- 71)
