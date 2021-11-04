(ns io.timmi.de4lfilter.parse
  (:require [clojure.data.json :as json]
            [io.timmi.de4lfilter.time :refer [from-local-str]]))

(defn parse [json:str]
  (json/read-str json:str :key-fn keyword))

(defn transform_locations [orig]
  (let [coords (:coords orig)]
       {:location {:lon (:longitude coords)
                   :lat (:latitude coords)}
        :speed (:speed coords)
        :tmp {:timestamp (from-local-str (:timestamp orig))}}))

(defn parse+transform_locations [json:str]
  (->> (parse json:str)
       (map transform_locations)))
