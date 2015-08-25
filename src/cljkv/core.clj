(ns cljkv.core
  (:require [cljkv.immutable-store :as is]))

(set! *warn-on-reflection* true)

(defprotocol Cljkv
  (insert
    [_ key value ttl]
    [_ key value] "Insert into store")
  (fetch [_ key] "Get a value from the store")
  (delete [_ key] "Delete a value from the store")
  (items [_] "Number of pairs in the store"))

(defrecord MutableStore [base]
  Cljkv
  (fetch [this key]
    (when (is/expired? @base key)
       (delete this key))
    (is/fetch @base key))
  (delete [_ key]
    (swap! base is/delete key))
  (insert [_ key value]
    (swap! base is/insert key value))
  (insert [_ key value ttl-ms]
    (swap! base is/insert key value ttl-ms))
  (items [this]
    (count @base)))

(defn create-mutable-store  
  ([] (create-mutable-store (clojure.lang.PersistentHashMap/EMPTY)))
  ([seed] (->MutableStore (atom (is/create-store seed)))))
