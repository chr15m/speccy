(ns ^:figwheel-no-load speccy.dev
  (:require
    [speccy.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)
