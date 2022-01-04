(ns io.timmi.de4lfilter.conf
  (:require [io.timmi.de4lfilter.parse :refer [parse transform_locations]]))

(def default_conf {
                   :skip-validation false
                   :speed-limit 5  ;; in km/h
                   :neighbourhood-radius 30  ;; in seconds
                   })

(defn parse+merge_conf [conf]
  (let [conf_edn (if (string? conf)
                   (parse conf)
                   conf)]
    (merge default_conf conf_edn)))

