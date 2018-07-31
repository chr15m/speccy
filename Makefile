STATICFILES=$(shell find public/ -type f -o -type l | grep -v public/js)
STATICDEST=$(foreach f,$(STATICFILES),$(subst public,build,$(f)))

all: public/fonts/fonts.css

build: $(STATICDEST) build/js/app.js

node_modules/.bin/goofoffline:
	npm install google-fonts-offline

public/fonts/fonts.css: node_modules/.bin/goofoffline
	cd public && ../$< "http://fonts.googleapis.com/css?family=Cutive+Mono"

build/%: public/%
	mkdir -p $(@D)
	cp -avL $< $@

build/js/app.js: src/**/**
	lein package
