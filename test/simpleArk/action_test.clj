(ns simpleArk.action-test
  (:require [clojure.test :refer :all]
            [simpleArk.arkValue.ark-value0 :as ark-value0]
            [simpleArk.log.log :as log]
            [simpleArk.log.logt :as logt]
            [simpleArk.uuid.uuidi :as uuidi]
            [simpleArk.arkDb.ark-db :as ark-db]
            [simpleArk.arkDb.ark-db0 :as ark-db0]
            [simpleArk.closer :as closer]
            [simpleArk.reader :as reader]
            [simpleArk.miMap :as miMap]
            [simpleArk.arkRecord :as arkRecord]
            [simpleArk.rolonRecord :as rolonRecord]
            [simpleArk.actions]
            [welcome.demo-actions]
            [simpleArk.builder :as builder]
            [welcome.demo-builds :as demo-builds]
            [simpleArk.uuid.uuid :as uuid]))

(set! *warn-on-reflection* true)

(defn hello-transaction
  [ark-db name]
  (builder/transaction!
    ark-db
    {:local/name name}
    (-> []
        (builder/build-println
          ["Hello " :local/name "!"])
        (builder/build-je-property
          [:index/headline] "Just for fun!")
        )))

(defn make-bob-transaction
  [ark-db]
  (builder/transaction!
    ark-db {}
    (-> []
        (builder/build-je-property
          [:index/headline] "make bob")
        (builder/build-gen-uuid
          :local/bob-uuid)
        (builder/build-property
          :local/bob-uuid [:content/age] 8)
        (builder/build-property
          :local/bob-uuid [:index/name] "Bob")
        (builder/build-property
          :local/bob-uuid [:content/brothers "John"] true)
        (builder/build-property
          :local/bob-uuid [:content/brothers "Jeff"] true)
        ))
  )

(defn test0
  [ark-db]

  (println)
  (println ">>>>>>>>>>>> hello-world")
  (println)

  (def hello-je-uuid
    (hello-transaction ark-db "Fred"))

  (println)
  (println ">>>>>>>>>>>> transaction names")
  (println)
  (let [ark-record (ark-db/get-ark-record ark-db)
        index-uuid (arkRecord/get-index-uuid ark-record "transaction-name")
        content-index (arkRecord/get-content-index
                        ark-record
                        index-uuid)]
    ;(mapish/debug [:content content-index])
    (doall (map #(println (first %)) content-index)))

  (println)
  (println ">>>>>>>>>>>> all the latest headlines")
  (println)
  (let [ark-record (ark-db/get-ark-record ark-db)
        headline-index-uuid (arkRecord/get-index-uuid ark-record "headline")
        content-index (arkRecord/get-content-index
                        ark-record
                        headline-index-uuid)]
    (doall (map #(println (first %)) content-index)))

  (println)
  (println ">>>>>>>>>>>> make-bob")
  (println)

  (def make-bob-je-uuid
    (make-bob-transaction ark-db))

  (is (= :transaction ((log/get-msg ark-db) 1)))
  (let [ark-value (ark-db/get-ark-record ark-db)]
    (println :rel/modified (arkRecord/get-related-uuids ark-value make-bob-je-uuid :rel/modified))
    (println :inv-rel/modified (arkRecord/get-related-uuids ark-value make-bob-je-uuid :inv-rel/modified))
    (println :lookup-bob (arkRecord/name-lookup ark-value "Bob")))

  (println)
  (println ">>>>>>>>>>>> make demo")
  (println)

  (def make-demo-uuid
    (builder/transaction!
      ark-db {}
      (-> []
          (demo-builds/build-demo)
          (builder/build-println :local)
          ))
    ))

(deftest arks
  (println "action tests")
  (def ark-db ((comp
                 (ark-db/builder)
                 (ark-db0/builder)
                 (ark-value0/builder)
                 (uuidi/builder)
                 (closer/builder)
                 (logt/builder)
                 (reader/builder))
                {}))
  (uuid/register ark-db)
  (miMap/register ark-db)
  (arkRecord/register ark-db)
  (rolonRecord/register ark-db)

  (ark-db/open-ark! ark-db)
  (try
    (test0 ark-db)
    (finally
      (closer/close-all ark-db))))
