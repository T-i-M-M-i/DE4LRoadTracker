(ns io.timmi.de4lfilter.parse
  (:require [jsonista.core :as j]))

(defn parse [json]
  (j/read-value json j/keyword-keys-object-mapper))

(defn transform_locations [orig]
  (let [coords (:coords orig)]
       {:location {:lon (:longitude coords)
                   :lat (:latitude coords)}
        :speed (:speed coords)
        :is_moving (:is_moving orig)}))

(defn assoc-meta [meta-data list-of-dict]
  (keep-indexed (fn [idx entry] (assoc (merge entry meta-data)
                                       :idx idx))
                list-of-dict))

(defn parse+transform [meta-data json]
  (->> (parse json)
       (map transform_locations)
       (assoc-meta meta-data)))

(defn parse+transform:file [filename]
  (->> (slurp filename)
       (parse+transform {:filename filename})))

(comment
  (->> (parse+transform:file "../sample-data-2021-10-25-lgme-lm-g9000/sensor_data.json8014616932918936741_locations.json")
       first))
