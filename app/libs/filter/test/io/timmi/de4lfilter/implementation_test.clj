(ns io.timmi.de4lfilter.implementation-test
  (:require [clojure.test :refer [deftest testing is]]
            [io.timmi.de4lfilter.implementation :refer [split-at-slow]]))

(deftest split-at-slow-test
  (is (= (split-at-slow [{:speed 23} {:speed 24}
                         {:speed 0} {:speed 1}
                         {:speed 42} {:speed 43}]
                        {:speed-limit 5} ))
         [[{:speed 23} {:speed 24}] [{:speed 42} {:speed 43}]]))
