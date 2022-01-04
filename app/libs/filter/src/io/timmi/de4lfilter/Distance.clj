(ns io.timmi.de4lfilter.Distance
  (:gen-class
    :name  io.timmi.de4lfilter.Distance
    :methods [^:static [distance [String] Double]
              ^:static [distanceMany [String] Double]])
  (:require [io.timmi.de4lfilter.parse :refer [parse transform_locations]]
            [io.timmi.de4lfilter.conf :refer [parse+merge_conf]]
            [io.timmi.de4lfilter.implementation :refer [invalid remove-around-slow merge-sensordata-by-time]]
            [io.timmi.de4lfilter.geo :refer [distance_in_km]]))

(defn path_distance_in_m* [locations:docs]
  (->> (map transform_locations locations:docs)
       (sort-by #(get-in % [:tmp :timestamp]))
       (reduce
         (fn [a b]
           (let [prevPoint (get-in a [:prev :location])
                 dist (if (some? prevPoint) (distance_in_km prevPoint (:location b)) 0)]
             {:prev b :dist (+ (:dist a) dist)})
           )
         {:dist 0})
       (:dist)
       (* 1000)))

(defn -distance [locations:json & [conf_orig]]
  (let [conf (parse+merge_conf conf_orig)
        locations:docs (parse locations:json :io.timmi.de4lfilter.parse/locations conf)]
    (path_distance_in_m* locations:docs)))

(defn -distanceMany [locations:json & [conf_orig]]
  (let [conf (parse+merge_conf conf_orig)
        locations:docs (apply concat (parse locations:json :io.timmi.de4lfilter.parse/locationss conf))]
    (path_distance_in_m* locations:docs)))
