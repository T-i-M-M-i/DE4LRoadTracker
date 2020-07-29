(ns io.timmi.clojureActivity
  (:import
   (android.util Log)
   (io.timmi.de4lroadtracker)
   )
  (:require
   [org.httpkit.client :as http]
   [clojure.tools.nrepl.server :as repl])
  (:gen-class
   :name "io.timmi.clojureActivity.MyActivity"
   :exposes-methods {onCreate superOnCreate}
   :extends androidx.appcompat.app.AppCompatActivity
   :prefix "some-"))

(defn fetch [url]
  (http/get url))

(defonce this-atom (atom nil))

(defn some-onCreate [^io.timmi.clojureActivity.MyActivity this ^android.os.Bundle bundle]
  (.superOnCreate this bundle)
  (.setContentView this io.timmi.de4lroadtracker.R$layout/activity_main)
  (Log/i "clojure" "...")
  (reset! this-atom this)
  (try
    (do
      (Log/i "repl clojure" "...")
      (repl/start-server :bind "127.0.0.1" :port 6888))
    (catch Exception e
      (Log/i "error " "clojure repl server")))
  #_(.start (Thread. (fn []
                       (let [data (fetch "http://www.yahoo.co.jp")]
                         (Log/i "clojure" (:body @data))))))
  )