(ns simpleArk.ark-db0-test
  (:require [clojure.test :refer :all]
            [simpleArk.ark-value :as ark-value]
            [simpleArk.ark-value0 :as ark-value0]
            [simpleArk.log :as log]
            [simpleArk.logt :as logt]
            [simpleArk.uuid :as uuid]
            [simpleArk.uuidi :as uuidi]
            [simpleArk.ark-db :as ark-db]
            [simpleArk.ark-db0 :as ark-db0]
            [simpleArk.closer :as closer]
            [simpleArk.mapish :as mapish]))

(set! *warn-on-reflection* true)

(defmethod ark-value/eval-transaction ::hello-world!
  [ark-value n s]
  (println "Hello," s)
  (let [je-uuid (ark-value/get-latest-journal-entry-uuid ark-value)]
    (ark-value/update-property ark-value je-uuid [:index/headline] "Just for fun!")))

(defn test0
  "tests that even work with impl0"
  [ark-db]

  (println)
  (println ">>>>>>>>>>>> hello-world")
  (println)
  (def hello-je-uuid (ark-db/process-transaction! ark-db ::hello-world! "Fred"))
  (is (= :transaction ((log/get-msg ark-db) 1)))
  (println)
  (println ">>>>>>>>>>>> make-bob")
  (println)
  (def bob-uuid (uuid/random-uuid ark-db))
  (def make-bob-je-uuid
    (ark-db/process-transaction!
      ark-db
      :ark/update-rolon-transaction!
      (prn-str
        [bob-uuid
         {[:index/headline] "make bob"}
         {[:content/age] 8
          [:index/name] "Bob"
          [:content/brothers "John"] true
          [:content/brothers "Jeff"] true}])))
  (is (= :transaction ((log/get-msg ark-db) 1)))
  (let [ark-value (ark-db/get-ark-value ark-db)]
    (println :rel/modified (seq (ark-value/get-related-uuids ark-value make-bob-je-uuid :rel/modified)))
    (println :inv-rel/modified (seq (ark-value/get-related-uuids ark-value make-bob-je-uuid :inv-rel/modified)))
    (println :bob-properties (seq (ark-value/get-property-values ark-value bob-uuid)))
    (println :lookup-bob (ark-value/name-lookup ark-value "Bob"))
    (println :brothers
             (seq
               (mapish/mi-sub
                 (ark-value/get-property-values ark-value bob-uuid)
                 [:content/brothers]))))

  (println)
  (println ">>>>>>>>>>>> 4 updates to bob")
  (println)
  (ark-db/process-transaction!
    ark-db
    :ark/update-rolon-transaction!
    (prn-str
      [bob-uuid
       {[:index/headline] "bob update 1"}
       {[:index/headline] "kissing is gross!"}]))
  (is (= :transaction ((log/get-msg ark-db) 1)))
  (ark-db/process-transaction!
    ark-db
    :ark/update-rolon-transaction!
    (prn-str
      [bob-uuid
       {[:index/headline] "bob update 2"}
       {[:content/age] 9}]))
  (is (= :transaction ((log/get-msg ark-db) 1)))
  (ark-db/process-transaction!
    ark-db
    :ark/update-rolon-transaction!
    (prn-str
      [bob-uuid
       {[:index/headline] "bob update 3"}
       {[:index/headline] "who likes girls?"}]))
  (is (= :transaction ((log/get-msg ark-db) 1)))
  (ark-db/process-transaction!
    ark-db
    :ark/update-rolon-transaction!
    (prn-str
      [bob-uuid
       {[:index/headline] "bob update 4"}
       {[:index/headline] "when do I get my own mobile!"}]))
  (is (= :transaction ((log/get-msg ark-db) 1)))

  (println)
  (println ">>>>>>>>>>>> make-sam")
  (println)
  (def sam-uuid (uuid/random-uuid ark-db))
  (def make-sam-je-uuid
    (ark-db/process-transaction!
      ark-db
      :ark/update-rolon-transaction!
      (prn-str
        [sam-uuid
         {[:index/headline] "make sam"}
         {[:content/age] 10
          [:index/name] "Sam"
          [:index/headline] "I hate green eggs and ham!"}])))
  (is (= :transaction ((log/get-msg ark-db) 1)))

  (println)
  (println ">>>>>>>>>>>> destroy-bob")
  (println)
  (def destroy-bob-je-uuid
    (ark-db/process-transaction!
      ark-db
      :ark/destroy-rolon-transaction!
      (prn-str
        [bob-uuid
         {[:index/headline] "destroy bob"}])))
  (is (= :transaction ((log/get-msg ark-db) 1)))
  (let [ark-value (ark-db/get-ark-value ark-db)]
    (println :bob-properties (seq (ark-value/get-property-values ark-value bob-uuid)))
    (println :lookup-bob (ark-value/name-lookup ark-value "Bob")))

  (println)
  (println ">>>>>>>>>> select time: make-bob-je-uuid")
  (println)
  (let [ark-value (ark-db/get-ark-value ark-db)
        _ (println "total je count:" (count (seq (ark-value/get-journal-entries ark-value))))
        ark-value (ark-value/select-time ark-value make-bob-je-uuid)]
    (println "selected je count:" (count (seq (ark-value/get-journal-entries ark-value))))
    (println :bob-properties (seq (ark-value/get-property-values ark-value bob-uuid)))
    (println :lookup-bob (ark-value/name-lookup ark-value "Bob"))
    )

  (println)
  (println ">>>>>>>>>>>> journal entry headlines")
  (println)
  (let [ark-value (ark-db/get-ark-value ark-db)]
    (first (keep (fn [x] (println
                           (ark-value/get-property-value ark-value (first (key x)) [:index/headline])))
                 (seq (ark-value/get-journal-entries ark-value)))))

  (println)
  (println ">>>>>>>>>>>> all the latest headlines")
  (println)
  (let [ark-value (ark-db/get-ark-value ark-db)
        headline-index-uuid (ark-value/get-index-uuid ark-value "headline")
        content-index (ark-value/get-content-index
                           ark-value
                           headline-index-uuid)]
    (doall (map #(println (first %)) content-index)))

  (println)
  (println ">>>>>>>>>>>> bob's headlines over time")
  (println)
  (let [ark-value (ark-db/get-ark-value ark-db)]
    (first (keep #(println (val %)
                           "-"
                           (ark-value/get-property-value ark-value (first (key %)) [:index/headline]))
                 (rseq (ark-value/get-changes-by-property ark-value
                                                          bob-uuid
                                                          [:index/headline]))))))

(deftest arks
  (println "impl0 tests")
  (def ark-db ((comp
                 (ark-db/builder)
                 (ark-db0/builder)
                 (ark-value0/builder)
                 (uuidi/builder)
                 (closer/builder)
                 (logt/builder))
                {}))
  (ark-db/open-ark! ark-db)
  (try
    (test0 ark-db)
    (finally
      (closer/close-all ark-db))))