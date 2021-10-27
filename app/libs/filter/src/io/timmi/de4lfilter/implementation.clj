(ns io.timmi.de4lfilter.implementation
  (:require [config.core :refer [env]]))

(defn invalid [doc]
  (< (:speed doc) 0))

(defn slow [doc]
  (< (:speed doc) (:speed-limit env)))

(defn neighbours? [doc1 doc2]
  (< (Math/abs (- (:idx doc1) (:idx doc2)))
     (:neighbourhood-radius env)))

(defn remove-after-slow [docs & {:keys [reverse?] :or {reverse? true}}]
  (let [;; depending on `reverse?` we append at beginning or end of the vector
        con (if reverse?
                (fn [remaining-docs doc] (cons doc remaining-docs))
                conj)
        [last-slow remaining-docs]  ;; the accumulated values of our reducer
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

(defn remove-around-slow
  "This function removes all docs with slow neighbours"
  [docs]
  (->> docs
       remove-after-slow  ;; remove successors, keep slow (needed in next step), reverse order
       remove-after-slow  ;; remove predecessors, reverse order back to original
       (remove slow)))
