(ns io.timmi.de4lfilter.parse
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [io.timmi.de4lfilter.time :refer [from-local-str]]))

(s/def :location/timestamp string?)
(s/def ::longitude number?)
(s/def ::latitude number?)
(s/def ::speed number?)
(s/def ::coords (s/keys :req-un [::longitude ::latitude ::speed]))
(s/def ::locations (s/coll-of (s/keys :req-un [:location/timestamp ::coords])))
(s/def ::locationss (s/coll-of ::locations))

(s/def :sensor/timestamp number?)
(s/def ::value sequential?)
(s/def ::sensordata (s/keys :req-un [:sensor/timestamp ::value]))
(s/def ::sensors (s/map-of keyword? (s/coll-of ::sensordata)))
(s/def ::sensorss (s/coll-of ::sensors))

(defn parse
 ([json:str]
  (if json:str
      (json/read-str json:str :key-fn keyword)))
 ([json:str spec conf]
  (let [parsed (parse json:str)]
       (if-not (:skip-validation conf)
               (when-not (s/valid? spec parsed)
                 (println (s/explain spec parsed))
                 (throw (AssertionError. (str "Parsing " spec " failed")))))
       parsed)))

(defn transform_locations [orig]
  (let [coords (:coords orig)]
       {:location {:lon (:longitude coords)
                   :lat (:latitude coords)}
        :speed (:speed coords)
        :tmp {:timestamp (from-local-str (:timestamp orig))}}))
