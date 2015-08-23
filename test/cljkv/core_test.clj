(ns cljkv.core-test
  (:require [clojure.test :refer :all]
            [cljkv.core :refer :all]
            [metrics.core :refer [new-registry]]
            [metrics.timers :refer [deftimer time! percentiles]]))

(def example-data {"a" {:val "b"}
                   "c" {:val "d"}})

(defn get-percentiles [timer store times]
  "Assumes the store has numbered keys"
  (dotimes [n times]
    (time! timer (cljkv.core/fetch store key)))
  (percentiles timer))

(deftest test-mutable-store
  (testing "Getting a key that exists from the store returns the value"
    (let [store (cljkv.core/create-mutable-store example-data)]
      (is (= (cljkv.core/fetch store "a") "b"))))
  (testing "Deleting a key removes the pair from the store"
    (let [store (cljkv.core/create-mutable-store example-data)]
      (cljkv.core/delete store "a")
      (is (= (cljkv.core/fetch store "a") nil))))
  (testing "Adding a key adds a key to the store"
    (let [store (cljkv.core/create-mutable-store example-data)]
      (cljkv.core/insert store "e" "f")
      (is (= (cljkv.core/fetch store "e") "f"))))
  (testing "Adding a key with a TTL adds a TTL pair to the store"
    (let [store (cljkv.core/create-mutable-store example-data)]
      (cljkv.core/insert store "e" "f" 10)
      (is (= (cljkv.core/fetch store "e") "f"))))
  (testing "Getting a key with an expired TTL return nil and removes the record"
    (let [store (cljkv.core/create-mutable-store example-data)]
      (cljkv.core/insert store "e" "f" 0) ; TTL is in 0 seconds, expires right away
      (is (= (cljkv.core/items store) 3))
      (is (= (cljkv.core/fetch store "e") nil))
      (is (= (cljkv.core/items store) 2))))
  (testing "Many threads can operate on the same store"
    ;; This test is implemented using Clojure's futures. They are executed on another
    ;; thread.
    (let [store (cljkv.core/create-mutable-store example-data)
          f1 (future (insert store "e" "f"))
          f2 (future (insert store "g" "h"))
          f3 (future (fetch store "a"))]
      ;; Derefrence them both (they will block until done)
      @f1
      @f2
      (is (= @f3 "b"))))
  (testing "That the store can contain 10.000.000 pairs"
    (let [keys (map str (range 10000000))
          pairs (zipmap keys (map (fn [v] {:val v}) (range 10000000)))
          store (cljkv.core/create-mutable-store pairs)
          key (nth keys (rand-int 10000000))]
      (is (= (cljkv.core/items store) 10000000))
      (deftimer insert-timer-10e7)
      (let [percentiles (get-percentiles insert-timer-10e7 store 1000)
            p95 (get percentiles 0.95)
            p99 (get percentiles 0.99)]
        (is (< p95 1000000.0))
        (is (< p99 5000000.0)))))
  (testing "P95 is not larger than 1ms when retrieving and P99 is no larger than 5ms"
    ;; The store will have 1000 pairs and the test gets a random value each time
    (let [keys (map str (range 1000))
          pairs (zipmap keys (range 1000))
          store (cljkv.core/create-mutable-store pairs)]
      (deftimer insert-timer-10e3)
      (let [percentiles (get-percentiles insert-timer-10e3 store 1000)
            p95 (get percentiles 0.95)
            p99 (get percentiles 0.99)]
        (is (< p95 1000000.0))
        (is (< p99 5000000.0))))))  
