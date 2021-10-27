(defproject visualize "0.1.0-SNAPSHOT"
  :description "test of the de4l roadtracker privacy filter"
  :url "https://github.com/T-i-M-M-i/DE4LRoadTracker"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]  ;; spandex requires a not too deprecated version
                 [yogthos/config "1.1.8"]
                 [metosin/jsonista "0.3.4"]
                 [cc.qbits/spandex "0.7.10"]]
  :source-paths ["src" "../src"]
  :main ^:skip-aot visualize.core)
