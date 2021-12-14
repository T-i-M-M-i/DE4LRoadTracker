(ns io.timmi.de4lfilter.geo)

(defn distance_in_km
  "Returns a rough estimate of the distance between two coordinate points, in kilometers. Works better with smaller distance"
  [{lat1 :lat lon1 :lon} {lat2 :lat lon2 :lon}]
  (let [deglen 110.25
        x (- lat2 lat1)
        y (* (Math/cos lat2) (- lon2 lon1))]
    (* deglen (Math/sqrt (+ (* y y) (* x x))))))
