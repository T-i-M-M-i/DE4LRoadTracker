(ns visualize.core
  (:gen-class)
  (:require [io.timmi.de4lfilter.parse :refer [parse+transform:file]]
            [io.timmi.de4lfilter.implementation :refer [invalid slow]]
            [io.timmi.de4lfilter.Filter :refer [-filter]]
            [visualize.elastic :refer [close! index:recreate inserts _count]]))

(def indexNames ["orig" "invalid" "slow" "filtered"])

(defn import-file-to-elastic [filename]
  (let [docs (parse+transform:file filename)
        valid (remove invalid docs)]
       (inserts "orig" docs)
       (inserts "invalid" (filter invalid docs))
       (inserts "slow" (filter slow valid))
       (inserts "filtered" (-filter docs))))

(defn stats []
  (Thread/sleep 1000)  ;; dirty workaround to await elastic transactions to finish
  (doseq [indexName indexNames]
     (println (str indexName ":") (_count indexName))))

(defn -main
  [& args]
  (doseq [indexName indexNames]
         (index:recreate indexName))
  (doall (map import-file-to-elastic args))
  (stats)
  (close!))

(comment
  (-main "../sample-data-2021-10-25-lgme-lm-g9000/sensor_data.json8014616932918936741_locations.json"))
