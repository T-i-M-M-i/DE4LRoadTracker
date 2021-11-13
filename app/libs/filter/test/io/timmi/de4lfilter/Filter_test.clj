(ns io.timmi.de4lfilter.Filter-test
  (:require [clojure.test :refer [deftest testing is]]
            [io.timmi.de4lfilter.Filter :refer [-filter -filterMany]]
            [io.timmi.de4lfilter.parse :refer [parse]]
            [clojure.string :refer [join]]))

(def sample_locations
  [(slurp "resources/example/sensor_data.json1771877570382502821_locations.json")
   (slurp "resources/example/sensor_data.json1790890205533416948_locations.json")
   (slurp "resources/example/sensor_data.json2187663889163804633_locations.json")])

(def sample_sensors
  [(slurp "resources/example/sensor_data.json1771877570382502821.json")
   (slurp "resources/example/sensor_data.json1790890205533416948.json")
   (slurp "resources/example/sensor_data.json2187663889163804633.json")])

(defn count_acceleration [result]
  (->> (parse result)
       (map #(get-in % [:sensors :acceleration]))
       (map count)
       (apply +)))

(deftest filter-test
  (let [result0 (-filter (first sample_locations) (first sample_sensors) "{}")
        result1 (-filter (second sample_locations) (second sample_sensors) "{}")
        result2 (-filter (last sample_locations) (last sample_sensors) "{}")]
    (testing "Only slow values"
      (is (= result0 "[]"))
      (is (= 0 (count_acceleration result0))))
    (testing "Some not filtered values"
      (is (= 7 (count (parse result1))))
      (is (= 13 (count (parse result2))))
      (is (= 30 (count_acceleration result1)))
      (is (= 402 (count_acceleration result2))))))

(defn json_list_concat [list_of_json_strings]
  (str "["
       (join "," list_of_json_strings)
       "]"))

(deftest filterMany-test
  (let [result (-filterMany (json_list_concat sample_locations) (json_list_concat sample_sensors) "{}")]
    (testing "Filter datasets from multiple datasets at once"
      (is (= 20 (count (parse result))))
      (is (= 432 (count_acceleration result))))))

(deftest conf-test
  (let [result (-filterMany (json_list_concat sample_locations) (json_list_concat sample_sensors) "{}" {:speed-limit 10})]
    (testing "Higher Speed Limit"
      (is (= 15 (count (parse result))))
      (is (= 407 (count_acceleration result))))))

(deftest parser-test
  (testing "invalid locations"
    (is (thrown? AssertionError (-filter "[{}]" (first sample_sensors) "{}"))))
  (testing "invalid sensors"
    (is (thrown? AssertionError (-filter (first sample_locations) "{\"my sensor\": [{}]}" "{}")))))
