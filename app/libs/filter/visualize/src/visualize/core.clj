(ns visualize.core
  (:gen-class)
  (:require [io.timmi.de4lfilter.parse :refer [parse+transform_locations]]
            [io.timmi.de4lfilter.implementation :refer [invalid slow]]
            [io.timmi.de4lfilter.Filter :refer [-filter]]
            [visualize.elastic :refer [close! index:recreate inserts _count]]
            [clojure.string :as string]
            [jsonista.core :as j]))

(def indexNames ["orig" "invalid" "slow" "filtered"])

(defn fixup-for-elastic [docs]
  (spit "./out.json" (j/write-value-as-string docs))
  (map (fn [doc] (update doc :sensors #(count (:acceleration %))))
       docs))

(defn import-file-to-elastic [locations:filename]
  (let [sensors:filename (string/replace locations:filename #"_locations.json$" ".json")
        sensors:json (slurp sensors:filename)
        locations:json (slurp locations:filename)
        locations:docs (parse+transform_locations locations:json)
        valid (remove invalid locations:docs)]
       (inserts "orig" locations:docs)
       (inserts "invalid" (filter invalid locations:docs))
       (inserts "slow" (filter slow valid))
       (inserts "filtered" (fixup-for-elastic (-filter locations:json sensors:json)))))

(defn stats []
  (Thread/sleep 1000)  ;; dirty workaround to await elastic transactions to finish
  (doseq [indexName indexNames]
     (println (str indexName ":") (_count indexName))))

(defn -main
  [& locations-filenames]
  (doseq [indexName indexNames]
         (index:recreate indexName))
  (doall (map import-file-to-elastic locations-filenames))
  (stats)
  (close!))
