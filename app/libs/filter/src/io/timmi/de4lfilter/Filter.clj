(ns io.timmi.de4lfilter.Filter
  (:gen-class
     :name  io.timmi.de4lfilter.Filter
     :methods [^:static [filter [String String String String] String]
               ^:static [filterMany [String String String String] String]])
  (:require #_[io.timmi.de4lfilter.parse :refer [#_parse transform_locations]]
            [io.timmi.de4lfilter.implementation :refer [invalid remove-around-slow merge-sensordata-by-time]]
            #_[clojure.data.json :as json]))

(def default_conf {
 :skip-validation false
 :speed-limit 5  ;; in km/h
 :neighbourhood-radius 30  ;; in seconds
})

(defn parse+merge_conf [conf]
  (let [conf_edn nil #_(if (string? conf)
                     (parse conf)
                     conf)]
       (merge default_conf conf_edn)))

(defn mock_json_write [edn]
  (println edn)
  "[{\"location\": {\"lon\": 12.3, \"lat\": 45,6}, \"speed\": 42}]")

(defn filter* [locations:docs sensors:docs meta:docs conf]
  (->> #_(map transform_locations locations:docs)
       [{:location {:lon 12.3 :lat 45.6} :speed 42 :tmp {:timestamp 123456}}]
       (remove invalid)
       (#(remove-around-slow % conf))
       (merge-sensordata-by-time sensors:docs)
       (map #(dissoc % :tmp))
       (mock_json_write)
       #_json/write-str))

(defn -filter [locations:json sensors:json meta:json & [conf_orig]]
  (let [conf (parse+merge_conf conf_orig)
        locatios:docs [] #_(parse locations:json :io.timmi.de4lfilter.parse/locations conf)
        sensors:docs {} #_(parse sensors:json :io.timmi.de4lfilter.parse/sensors conf)
        meta:docs {} #_(parse meta:json)]
       (filter* locatios:docs sensors:docs meta:docs conf)))

(defn -filterMany [locations:json sensors:json meta:json & [conf_orig]]
  (let [conf (parse+merge_conf conf_orig)
        locations:docs (apply concat [[] []] #_(parse locations:json :io.timmi.de4lfilter.parse/locationss conf))
        sensors:docs (apply concat [{} {}] #_(parse sensors:json :io.timmi.de4lfilter.parse/sensorss conf))
        meta:docs {} #_(parse meta:json)]
       (filter* locations:docs sensors:docs meta:docs conf)))
