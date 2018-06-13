all: public/fonts/fonts.css

node_modules/.bin/goofoffline:
	npm install google-fonts-offline

public/fonts/fonts.css: node_modules/.bin/goofoffline
	cd public && ../$< "http://fonts.googleapis.com/css?family=Cutive+Mono"
