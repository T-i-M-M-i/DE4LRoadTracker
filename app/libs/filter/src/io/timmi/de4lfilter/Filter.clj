(ns io.timmi.de4lfilter.Filter
  (:gen-class
     :name  io.timmi.de4lfilter.Filter
     :methods [^:static [filter [String] String]])
  (:require [io.timmi.de4lfilter.implementation :refer [invalid remove-around-slow]]))

(defn -filter [location-json]
  (->> location-json
       (remove invalid)
       remove-around-slow))
