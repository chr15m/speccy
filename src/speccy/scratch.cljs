(ns speccy.scratch
  (:require [speccy.engine :refer [looper scheduler]]))

;(-> scheduler (.start player))
;(-> scheduler (.stop true))

(looper (fn [t] {:p_env_decay 0.1
                 :p_env_sustain 0.1 ;([0.1 0.5] (mod (mod t 13) 2))
                 :p_freq_ramp 0.1 ;([0.1 0.8] (mod (mod t 13) 2))
                 :p_duty 0.5
                 :p_repeat_speed 0.9
                 :sound_vol (get [0.2 0.1 0 0.1] (mod t 4))
                 :wave_type 2 ;(mod (mod t 27) 4)
                 :p_base_freq (+ 0.2 (/ (mod t 8) 16))}))
