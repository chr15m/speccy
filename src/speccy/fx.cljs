(ns speccy.fx)

(defn biquad [actx freq q destination]
  (let [f (.createBiquadFilter actx)]
    (aset f "type" "lowpass")
    (.setValueAtTime (aget f "frequency") (or freq 5000) (.-currentTime actx))
    (.setValueAtTime (aget f "Q") (or q 2) (.-currentTime actx))
    (.connect f (or destination (.-destination actx)))
    f))

