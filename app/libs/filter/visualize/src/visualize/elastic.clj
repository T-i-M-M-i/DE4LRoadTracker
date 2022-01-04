(ns visualize.elastic
  (:require [config.core :refer [env]]
            [qbits.spandex :as spandex]))

(def c (spandex/client {:hosts [(:elastic-host env)]
                        :http-client (when (and (:elastic-user env) (:elastic-password env))
                                           {:basic-auth {:user (:elastic-user env)
                                                         :password (:elastic-password env)}})}))

(defn index:delete [indexName]
  (spandex/request c
    {:method :delete
     :url (str "/" indexName)}))

(defn index:create [indexName]
  (spandex/request c
    {:method :put
     :url (str "/" indexName)
     :body {:mappings {:properties {:location {:type "geo_point"}}}}}))

(defn index:recreate [indexName]
  (try (index:delete indexName)
       (catch Exception e))
  (index:create indexName))

(defn insert [indexName doc]
  (spandex/request c
    {:method :post
     :url (str "/" indexName "/_doc/")
     :body doc}))

(defn inserts [indexName docs]
  (doall (map #(insert indexName %) docs)))

(defn search [indexName]
  (->> (spandex/request c
         {:url (str "/" indexName "/_search")})
       (#(get-in % [:body :hits :hits]))
       (map :_source)))

(defn _count [indexName]
  (->> (spandex/request c
         {:url (str "/" indexName "/_count")})
       (#(get-in % [:body :count]))))

(defn close! []
  (spandex/close! c))

(comment
  (let [indexName "orig"]
       (index:delete indexName)
       (index:create indexName)
       (insert indexName
         {:location "51.06,13.74", :name "Dresden"})
       (search indexName)
       (_count indexName)))
