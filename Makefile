deps: public/lib/sfxr.js public/lib/riffwave.js

public/lib/sfxr.js:
	curl https://raw.githubusercontent.com/chr15m/jsfxr/master/sfxr.js > $@

public/lib/riffwave.js:
	curl https://raw.githubusercontent.com/chr15m/jsfxr/master/riffwave.js > $@

# public/lib/web-audio-scheduler.min.js:

