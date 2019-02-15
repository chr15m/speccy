eight-bit algorave clojurescript livecoding

![zx-spectrum close-up](./public/img/speccy.png)

[Watch the demo video](https://youtu.be/IZtCSfd9G_A).

# Quick start

Minimal example to paste into the editor:

```clojure
(sfxr {:wave :square :note #(at % 16 {0 60 8 65})})
```

More complex example:

```clojure
(sfxr "1111128F2i1nMgXwxZ1HMniZX45ZzoZaM9WBtcQMiZDBbD7rvq6mBCATySSmW7xJabfyy9xfh2aeeB1JPr4b7vKfXcZDbWJ7aMPbg45gBKUxMijaTNnvb2pw"
      {:duty #(/ (mod % 256) 256)
       :note #(at % 16 {0 24
			3 24
			4 60
			5 48
			6 24
			8 24})})
```

To make your own synths:

 * Generate a sound you like at [sfxr.me](http://sfxr.me/).
 * Click the "Copy" button to get the synth definition.
 * Paste this code into the editor: `(sfxr "...")` replacing the ellipsis with your copied definition.
 * Hit `ctrl-S` to send your instrument to the synthesizer.
 * Listen, modify, repeat.

# Helper functions

 * `sq` is a basic sequencer. Args: `position` (e.g. time), `list`. Looks up the value wrapped by the array length.

For example:

```clojure
#(sq % [1 1 0 0])
```

Will yield: `1 1 0 0 1 1 0 0 1 1 0 0 ...`.

 * `at` is another basic sequencer. Args: `position` (e.g. time), `wrap-length` when to loop (e.g. sequence length), `values` is a hash-map of note-positions yielding what should happen at that point in time.

for example:

```clojure
#(at % 8 {2 60
	  7 55})
```

will yield: `nil nil 60 nil nil nil nil 55 nil nil 60 nil nil nil nil 55 nil ...`. 

 * `rnd` is a random number generator.
   * Called with no arguments it returns a float between 0 and 1. e.g. `(rnd)`
   * Called with an integer argument it returns an integer between zero and the argument. e.g. `(rnd 5)`
   * Called with a vector it returns a single randomly selected element from the vector. e.g. `(rnd [1 1 2 3 5 8])`

 * `seed` seeds the random number generator to make it deterministic.

 * `zz` wraps a function such that the function is only called if none of the arguments to it are nil. e.g. `((zz +) 1 nil 2)` will yeild nil.

# Set global BPM

```clojure
(swap! player assoc :bpm 125)
```

# Instrument parameters

```clojure
	:wave ; oscillator waveform (:square :saw :sine :noise)

	:volume ; (0 -> 1)
	
	:note ; oscillator pitch as (midi note number)
	:frequency ; oscillator pitch (Hz)
	:frequency/limit ; i do not know (Hz)
	:frequency/ramp ; freq change speed (-1 -> 1)
	:frequency/ramp-delta ; freq change acceleration (-1 -> 1)
```

### Envelope

```clojure
	:env/attack ; envelope attack time (0 -> 1)
	:env/sustain ; adsr envelope sustain time (0 -> 1)
	:env/punch ; adsr envelope sustain height (0 -> 1)
	:env/decay ; adsr envelope decay time (0 -> 1)
```

### Low pass filter

```clojure
	:lpf/frequency ; cutoff pitch (Hz)
	:lpf/note ; cutoff pitch (midi note)
	:lpf/resonance ; resonance (0 -> 1)
	:lpf/ramp ; pitch change rate (-1 -> 1)
```

### High pass filter

```clojure
	:hpf/frequency ; cutoff pitch (Hz)
	:hpf/note ; cutoff pitch (midi note)
	:hpf/ramp ; pitch change rate (-1 -> 1)
```

### Oscillator phase

```clojure
	:phase/offset ; (0 -> 1)
	:phase/ramp ; phase change rate (-1 -> 1)
```

### Arpeggiator

```clojure
	:arp/speed ; arpeggiation speed (0 -> 1)
	:arp/mod ; arpeggiation multiple (-1 -> 1)
```

### Vibrato

```clojure
	:vibrato/depth ; amount of vibrato (0 -> 1)
	:vibrato/frequency ; vibrato pitch (Hz)
	:vibrato/note ; vibrato pitch (midi note)
```

### Oscillator duty cycle (square only)

```clojure
	:duty ; (0 -> 1)
	:duty/ramp ; duty change rate (-1 -> 1)
```

### Repeat note

```clojure
	:retrigger ; speed of retrigger (0 -> 1)
```
