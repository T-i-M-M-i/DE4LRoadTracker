(ns io.timmi.de4lfilter.parse
  (:require [jsonista.core :as j]
            [io.timmi.de4lfilter.time :refer [from-local-str]]))

(defn parse [json:str]
  (j/read-value json:str j/keyword-keys-object-mapper))

(defn transform_locations [orig]
  (let [coords (:coords orig)]
       {:location {:lon (:longitude coords)
                   :lat (:latitude coords)}
        :speed (:speed coords)
        :tmp {:timestamp (from-local-str (:timestamp orig))}}))

(defn parse+transform_locations [json:str]
  (->> (parse json:str)
       (map transform_locations)))
