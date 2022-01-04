(ns io.timmi.de4lfilter.gpx
  (:use
    [clojure.data.xml :only (sexp-as-element)])
  (:require
    (clojure [xml :as x])
    ))

(defn date->iso [dt]
  (. (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss.SSSZ") format dt))

(defn timestamp->iso [ts]
  (. (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss.SSSZ") format (java.util.Date. ts)))

(defn clj2gpx [data]
  (sexp-as-element [:gpx {
                          :version            "1.0"
                          :creator            "io.timmi.de4lfilter"
                          :xmlns:xsi          "http://www.w3.org/2001/XMLSchema-instance"
                          :xmlns              "http://www.topografix.com/GPX/1/0"
                          :xsi:schemaLocation "http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd"}
                    [:time (date->iso (java.util.Date.))]
                    [:trk [:trkseg
                           (map
                             #(let [loc (:location %)
                                   ts (get-in % [:tmp :timestamp])]
                               (vector :trkpt
                                       (select-keys loc [:lat :lon])
                                       [:ele (:ele loc)]
                                       [:time (timestamp->iso ts)])) data)]]]))
