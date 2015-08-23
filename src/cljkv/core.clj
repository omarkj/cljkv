(ns cljkv.core)

(set! *warn-on-reflection* true)

(defprotocol Cljkv
  (insert
    [_ key value ttl]
    [_ key value] "Insert into store")
  (fetch [_ key] "Get a value from the store")
  (delete [_ key] "Delete a value from the store")
  (items [_] "Number of pairs in the store"))

(defrecord Store [base]
  Cljkv
  (fetch [this key]
    (when-let [res (get @base key)]
      (if-let [timeout (get res :ttl)]
        (if (> timeout (System/currentTimeMillis))
          (get res :val)
          ((delete this key)
           nil))
        (get res :val))))
  (delete [_ key]
    (swap! base dissoc key))
  (insert
    [this key value]
    (swap! base assoc key {:val value}))
  (insert [this key value ttl-ms]
    (let [timeout (+ (System/currentTimeMillis) ttl-ms)]
      (swap! base assoc key {:val value
                             :ttl timeout})))
  (items [this]
    (count @base)))

(defn create-mutable-store
  ([] (create-mutable-store (clojure.lang.PersistentHashMap/EMPTY)))
  ([seed] (->Store (atom seed))))
