(ns io.timmi.de4lfilter.time
  (:require [clj-time.coerce :as c]
            [clj-time.local :as l]))

;; as internal representation a Long for ms since 1970 is used

(defn from-local-str [s]
  (c/to-long (l/to-local-date-time s)))
