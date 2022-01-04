(ns io.timmi.de4lfilter.Exporter
  (:gen-class
    :name  io.timmi.de4lfilter.Exporter
    :methods [^:static [exportToGpx [String] String]
              ^:static [exportToGpxMany [String] String]])
  (:use
    [clojure.data.xml :only (emit-str)])
  (:require [io.timmi.de4lfilter.parse :refer [parse transform_locations]]
            [io.timmi.de4lfilter.conf :refer [parse+merge_conf]]
            [io.timmi.de4lfilter.implementation :refer [invalid remove-around-slow merge-sensordata-by-time]]
            [io.timmi.de4lfilter.gpx :refer [clj2gpx]]))


(defn export_to_gpx* [locations:docs]
  (->> (map transform_locations locations:docs)
       (sort-by #(get-in % [:tmp :timestamp]))
       (clj2gpx)
       (emit-str)
       ))

(defn -exportToGpx [locations:json & [conf_orig]]
  (let [conf (parse+merge_conf conf_orig)
        locations:docs (parse locations:json :io.timmi.de4lfilter.parse/locations conf)]
    (export_to_gpx* locations:docs)))

(defn -exportToGpxMany [locations:json & [conf_orig]]
  (let [conf (parse+merge_conf conf_orig)
        locations:docs (apply concat (parse locations:json :io.timmi.de4lfilter.parse/locationss conf))]
    (export_to_gpx* locations:docs)))
