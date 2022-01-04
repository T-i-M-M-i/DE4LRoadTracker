(ns io.timmi.de4lfilter.Filter
  (:gen-class
     :name  io.timmi.de4lfilter.Filter
     :methods [^:static [filter [String String String String] String]
               ^:static [filterMany [String String String String] String]])
  (:require [io.timmi.de4lfilter.parse :refer [parse transform_locations]]
            [io.timmi.de4lfilter.conf :refer [parse+merge_conf]]
            [io.timmi.de4lfilter.implementation :refer [invalid remove-around-slow merge-sensordata-by-time]]
            [clojure.data.json :as json]))

(defn wrap-meta
  "Returns nil or a json:str with meta+data"
  [meta:docs filteredData]
  (if-not (empty? filteredData)
          (json/write-str {:meta meta:docs
                           :data filteredData})))

(defn filter* [locations:docs sensors:docs meta:docs conf]
  (->> (map transform_locations locations:docs)
       (remove invalid)
       (#(remove-around-slow % conf))
       (merge-sensordata-by-time sensors:docs)
       (map #(dissoc % :tmp))
       (wrap-meta meta:docs)))

(defn -filter [locations:json sensors:json meta:json & [conf_orig]]
  (let [conf (parse+merge_conf conf_orig)
        locations:docs (parse locations:json :io.timmi.de4lfilter.parse/locations conf)
        sensors:docs (parse sensors:json :io.timmi.de4lfilter.parse/sensors conf)
        meta:docs (parse meta:json)]
       (filter* locations:docs sensors:docs meta:docs conf)))

(defn -filterMany [locations:json sensors:json meta:json & [conf_orig]]
  (let [conf (parse+merge_conf conf_orig)
        locations:docs (apply concat (parse locations:json :io.timmi.de4lfilter.parse/locationss conf))
        sensors:docs (apply concat (parse sensors:json :io.timmi.de4lfilter.parse/sensorss conf))
        meta:docs (parse meta:json)]
       (filter* locations:docs sensors:docs meta:docs conf)))
