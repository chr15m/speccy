eight-bit algorave livecoding

![zx-spectrum close-up](./public/img/bg.png)

# Quick start

Example to paste into the editor:

	(sfxr "1111128F2i1nMgXwxZ1HMniZX45ZzoZaM9WBtcQMiZDBbD7rvq6mBCATySSmW7xJabfyy9xfh2aeeB1JPr4b7vKfXcZDbWJ7aMPbg45gBKUxMijaTNnvb2pw"
	      {:duty #(/ (mod % 256) 256)
	       :note #(at % 16 {0 24
	                        3 24
	                        4 60
	                        5 48
	                        6 24
	                        8 24})})

To make your own synths:

 * Generate a sound you like at [sfxr.me](http://sfxr.me/).
 * Click the "Copy" button to get the synth definition.
 * Paste this code into the editor: `(sfxr "...")` replacing the ellipsis with your copied definition.
 * Hit `ctrl-S` to send your instrument to the synthesizer.
 * Listen, modify, repeat.

# Helper functions

 * `sq` is a basic sequencer. Args: `position` (e.g. time), `list`. Looks up the value wrapped by the array length.

For example:

	#(sq % [1 1 0 0])

Will yeild: `1 1 0 0 1 1 0 0 1 1 0 0 ...`.

 * `at` is another basic sequencer. Args: `position` (e.g. time), `wrap-length` when to loop (e.g. sequence length), `values` is a hash-map of note-positions yeilding what should happen at that point in time.

for example:

	#(at % 8 {2 60
	          7 55})

will yeild: `nil nil 60 nil nil nil nil 55 nil nil 60 nil nil nil nil 55 nil ...`. 

# Instrument parameters

	:wave - oscillator waveform (:square :saw :sine :noise)

	:volume - (0 -> 1)
	
	:note - oscillator pitch as (midi note number)
	:frequency - oscillator pitch (Hz)
	:frequency/limit - i do not know (Hz)
	:frequency/ramp - freq change speed (-1 -> 1)
	:frequency/ramp-delta - freq change acceleration (-1 -> 1)

### Envelope

	:env/attack - envelope attack time (0 -> 1)
	:env/sustain - adsr envelope sustain time (0 -> 1)
	:env/punch - adsr envelope sustain height (0 -> 1)
	:env/decay - adsr envelope decay time (0 -> 1)

### Low pass filter

	:lpf/frequency - cutoff pitch (Hz)
	:lpf/note - cutoff pitch (midi note)
	:lpf/resonance - resonance (0 -> 1)
	:lpf/ramp - pitch change rate (-1 -> 1)

### High pass filter

	:hpf/frequency - cutoff pitch (Hz)
	:hpf/note - cutoff pitch (midi note)
	:hpf/ramp - pitch change rate (-1 -> 1)

### Oscillator phase

	:phase/offset - (0 -> 1)
	:phase/ramp - phase change rate (-1 -> 1)

### Arpeggiator

	:arp/speed - arpeggiation speed (0 -> 1)
	:arp/mod - arpeggiation multiple (-1 -> 1)

### Vibrato

	:vibrato/depth - amount of vibrato (0 -> 1)
	:vibrato/frequency - vibrato pitch (Hz)
	:vibrato/note - vibrato pitch (midi note)

### Oscillator duty cycle (square only)

	:duty (0 -> 1)
	:duty/ramp - duty change rate (-1 -> 1)

### Repeat note

	:retrigger - speed of retrigger (0 -> 1)
