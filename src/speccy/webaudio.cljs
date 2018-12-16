(ns speccy.webaudio)

(defn get-context []
  (when (aget js/window "AudioContext") (js/window.AudioContext.)))

(defn connect [node actx destination]
  (.connect node (or destination (.-destination actx)))
  node)
