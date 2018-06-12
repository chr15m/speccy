(defproject speccy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [reagent "0.8.0"]
                 [cljsjs/codemirror "5.31.0-0"]
                 [cljsjs/parinfer "3.11.0-0"]
                 [cljsjs/parinfer-codemirror "1.4.1-2"]]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-figwheel "0.5.16"]]

  :min-lein-version "2.5.0"

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :resource-paths ["public"]

  :figwheel {:http-server-root "."
             :nrepl-port 7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
             :css-dirs ["public/css"]}

  :cljsbuild {:builds {:app
                       {:source-paths ["src" "env/dev/cljs"]
                        :compiler
                        {:main "speccy.dev"
                         :output-to "public/js/app.js"
                         :output-dir "public/js/out"
                         :asset-path   "js/out"
                         :source-map true
                         :optimizations :none
                         :install-deps true
                         :npm-deps {"codemirror" "5.38.0"}
                         :foreign-libs [{:file "https://raw.githubusercontent.com/chr15m/jsfxr/master/riffwave.js"
                                         :provides ["riffwave"]}
                                        {:file "https://raw.githubusercontent.com/chr15m/jsfxr/master/sfxr.js"
                                         :provides ["sfxr"]}]
                         :pretty-print  true}
                        :figwheel
                        {:on-jsload "speccy.core/mount-root"}}
                       :release
                       {:source-paths ["src" "env/prod/cljs"]
                        :compiler
                        {:output-to "build/js/app.js"
                         :output-dir "public/js/release"
                         :asset-path   "js/out"
                         :optimizations :advanced
                         :install-deps true
                         :npm-deps {"codemirror" "5.38.0"}
                         :foreign-libs [{:file "https://raw.githubusercontent.com/chr15m/jsfxr/master/riffwave.js"
                                         :provides ["riffwave"]}
                                        {:file "https://raw.githubusercontent.com/chr15m/jsfxr/master/sfxr.js"
                                         :provides ["sfxr"]}]
                         :pretty-print false}}}}

  :aliases {"package" ["do" "clean" ["cljsbuild" "once" "release"]]}

  :profiles {:dev {:source-paths ["src" "env/dev/clj"]
                   :dependencies [[binaryage/devtools "0.9.7"]
                                  [figwheel-sidecar "0.5.16"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [com.cemerick/piggieback "0.2.2"]]}})
