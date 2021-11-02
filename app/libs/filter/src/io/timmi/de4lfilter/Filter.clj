(ns io.timmi.de4lfilter.Filter
  (:gen-class
     :name  io.timmi.de4lfilter.Filter
     :methods [^:static [filter [String] String]])
  (:require [io.timmi.de4lfilter.parse :refer [parse parse+transform_locations]]
            [io.timmi.de4lfilter.implementation :refer [invalid remove-around-slow merge-sensordata-by-time]]))

(defn -filter [locations:json sensors:json]
  (->> (parse+transform_locations locations:json)
       (remove invalid)
       remove-around-slow
       (merge-sensordata-by-time (parse sensors:json))
       (map #(dissoc % :tmp))))
