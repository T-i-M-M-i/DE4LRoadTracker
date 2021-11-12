(ns io.timmi.de4lfilter.Filter
  (:gen-class
     :name  io.timmi.de4lfilter.Filter
     :methods [^:static [filter [String String String] String]
               ^:static [filterMany [String String String] String]])
  (:require [io.timmi.de4lfilter.parse :refer [parse transform_locations]]
            [io.timmi.de4lfilter.implementation :refer [invalid remove-around-slow merge-sensordata-by-time]]
            [clojure.data.json :as json]))

(def default_conf {
 :speed-limit 5  ;; in km/h
 :neighbourhood-radius 30  ;; in seconds
})

(defn filter* [locations:docs sensors:docs meta:docs conf]
  (->> (map transform_locations locations:docs)
       (remove invalid)
       (#(remove-around-slow % conf))
       (merge-sensordata-by-time sensors:docs)
       (map #(dissoc % :tmp))
       json/write-str))

(defn -filter [locations:json sensors:json meta:json & [conf]]
  (let [locatios:docs (parse locations:json)
        sensors:docs (parse sensors:json)
        meta:docs (parse meta:json)]
       (filter* locatios:docs sensors:docs meta:docs (merge default_conf conf))))

(defn -filterMany [locations:json sensors:json meta:json & [conf]]
  (let [locations:docs (apply concat (parse locations:json))
        sensors:docs (apply concat (parse sensors:json))
        meta:docs (parse meta:json)]
       (filter* locations:docs sensors:docs meta:docs (merge default_conf conf))))
