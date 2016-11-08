(ns simpleArk.pub0-test
  (:require [clojure.test :refer :all]
            [simpleArk.ark-db :as ark-db]
            [simpleArk.pub :as pub]
            [simpleArk.pub0 :as pub0]
            [clojure.core.async :as async]))

(set! *warn-on-reflection* true)

(deftest pub0
  (let [c ((comp
             (ark-db/builder)
             (pub0/builder))
            {})
        rsp-chan (async/chan 10)]
    (ark-db/init-ark! c "_")
    (pub/publish! c "x" [[rsp-chan 1]])
    (println (async/<!! rsp-chan) (ark-db/get-ark-value c))
    (pub/publish! c "z" [[rsp-chan 2] [rsp-chan 3]])
    (println (async/<!! rsp-chan) (ark-db/get-ark-value c))
    (println (async/<!! rsp-chan) (ark-db/get-ark-value c))))