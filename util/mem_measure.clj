(ns cljkv.mem-measure
  (:import [com.carrotsearch.sizeof RamUsageEstimator])
  (:require [cljkv.core :refer :all]
            [clojure.java.io :refer [writer]]))

(defn ** [x n]
  (reduce * (repeat n x)))

(defn measure
  [pairs filename]
  (let [store (cljkv.core/create-mutable-store)
        keys (range pairs)]
    (with-open [fs (writer filename :append true)]
      (dorun (map (fn [key]
                    (insert store (str key) key)
                    (when (= (bit-and key (- key 1)) 0)
                      (let [mem-usage (RamUsageEstimator/sizeOf store)
                            csv-pair (format "%s,%s\n" key mem-usage)]
                        (.write fs csv-pair)))
                    ) keys)))))
