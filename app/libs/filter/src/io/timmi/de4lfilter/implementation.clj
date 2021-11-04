(ns io.timmi.de4lfilter.implementation)

(def env {
 :speed-limit 5  ;; in km/h
 :neighbourhood-radius 30  ;; in seconds
})  ;; mock yogthos/config

(defn invalid [doc]
  (< (:speed doc) 0))

(defn slow [doc]
  (< (:speed doc) (:speed-limit env)))

(defn neighbours? [doc1 doc2]
  (< (Math/abs (- (get-in doc1 [:tmp :timestamp]) (get-in doc2 [:tmp :timestamp])))
     (* 1000 (:neighbourhood-radius env))))

(defn remove-after-slow [docs & {:keys [reverse?] :or {reverse? true}}]
  (let [;; depending on `reverse?` we append at beginning or end of the vector
        con (if reverse?
                (fn [remaining-docs doc] (cons doc remaining-docs))
                conj)
        [_ remaining-docs]  ;; the accumulated values of our reducer
          (reduce (fn [[last-slow remaining-docs] doc]
                      ;; the current doc can be in one of 3 states:
                      (if (slow doc)
                          ;; 1. slow
                          [doc (con remaining-docs doc)]
                          (if (and last-slow (neighbours? doc last-slow))
                              ;; 2. in neighbourhood of slow
                              [last-slow remaining-docs]
                              ;; 3. outside of slow neighbourhood
                              [nil (con remaining-docs doc)])))
                  [nil []]  ;; initially we don't have a `last-slow` item
                  docs)]
       remaining-docs))

(defn split-at-slow [docs]
  (let [[_ splitted-docs]
          (reduce (fn [[predecessor-slow? splitted-docs] doc]
                      (if (slow doc)
                          [true splitted-docs]
                          (if predecessor-slow?
                              [false (conj splitted-docs [doc])]
                              [false (conj (into [] (butlast splitted-docs))
                                           (conj (last splitted-docs) doc))])))
                  [true []]
                  docs)]
       splitted-docs))

(defn merge+relative-time
  "Time will be in seconds since beginning of the track.
   Entries of the same track can be correlated by a random trackid."
  [splitted-docs]
  (->> splitted-docs
       (map (fn [docs]
            (let [trackid (java.util.UUID/randomUUID)
                  offset (get-in (first docs) [:tmp :timestamp])]
                 (map (fn [doc]
                          (assoc doc :trackid trackid
                                     :time (/ (- (get-in doc [:tmp :timestamp])
                                                 offset)
                                              1000)))
                      docs))))
       (apply concat)))

(defn remove-around-slow
  "This function removes all docs with slow neighbours"
  [docs]
  (->> docs
       remove-after-slow  ;; remove successors, keep slow (needed in next step), reverse order
       remove-after-slow  ;; remove predecessors, reverse order back to original
       split-at-slow      ;; get rid of the slow entries and split
       merge+relative-time))

(defn ->time-relative-to [locations:doc]
  (fn [sensors:doc]
      (-> sensors:doc
          (assoc :time (+ (/ (- (:timestamp sensors:doc)
                                (get-in locations:doc [:tmp :timestamp]))
                             1000)
                          (:time locations:doc)))
          (dissoc :timestamp))))

(defn merge-sensordata-by-time [sensors:docs locations:docs]
  (cons (first locations:docs)
        ;; instead of map we could implement it more efficient using reduce on time sorted locations:docs
        (map (fn [[predecessor doc]]
                 (let [start (get-in predecessor [:tmp :timestamp])
                       end (get-in doc [:tmp :timestamp])
                       sensors (reduce (fn [sensors [sensor entries]]
                                           (let [filtered (if-not (= (:trackid predecessor) (:trackid doc))
                                                                     []
                                                                     (->> entries
                                                                          (drop-while #(< (:timestamp %) start))
                                                                          (take-while #(<= (:timestamp %) end))
                                                                          (map (->time-relative-to doc))))]
                                                (if (empty? filtered)
                                                    sensors
                                                    (assoc sensors sensor filtered))))
                                       {}
                                       sensors:docs)]
                      (assoc doc :sensors sensors)))
             (partition 2 1 locations:docs))))
