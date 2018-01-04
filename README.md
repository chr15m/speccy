eight-bit algorave livecoding

![zx-spectrum close-up](./public/img/speccy.png)

 * Generate a sound you like at [sfxr.me](http://sfxr.me/).
 * Click "Serialize" and copy the JSON definition.
 * Paste into the app's text-box to generate EDN.
 * Add to [scratch.cljs](./src/speccy/scratch.cljs) and edit to add time dependent changes.

Here's an example synth:

	; bloopy synth
	(add! (fn [t]
	  (if (= (mod t 2) 0)
	    {:wave :saw
                   :env/decay 0.25
                   :note (sequencer (sequencer t [0 1 2 3 4 5 6]) (get-scale t))
                   :volume (sequencer t [0.1 0.1 0])
                   :p_pha_offset (sequencer t [0.1 0.2 0.3 0.4 0.5])})))

 * Listen, modify, repeat.

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
