(ns cljkv.immutable-store)

(defn insert
  "Insert a key into the datastore and return a new copy of it"
  ([store key value]
   (assoc store key {:val value}))
  ([store key value ttl-ms]
   (assoc store key {:val value
                     :ttl (+ (System/currentTimeMillis) ttl-ms)})))

(defn delete [store key]
  "Delete a key from the store"
  (dissoc store key))

(defn fetch [store key]
  "Fetch a key from the store"
  (when-let [value (get store key)]
    (get value :val)))

(defn expired? [store key]
  (if-let [value (get store key)]
    (if-let [ttl (get value :ttl)]
      (< ttl (System/currentTimeMillis))
      false)
    false))

(defn items [store]
  (count store))

(defn create-store
  ([] (clojure.lang.PersistentHashMap/EMPTY))
  ([seed] seed))
