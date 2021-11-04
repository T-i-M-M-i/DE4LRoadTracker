(defproject io.timmi.de4lfilter "0.1.0-SNAPSHOT"
  :description "de4l roadtracker privacy filter"
  :url "https://github.com/T-i-M-M-i/DE4LRoadTracker"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.8.0"]  ;; with Android as target, we may rely on Java 7, but not on Java 8
                 [org.clojure/data.json "2.4.0"]
                 [clj-time "0.15.2"]] ;; on Android we can't rely on java.time (requires Java 8)
  :profiles {:uberjar {:aot :all}})
