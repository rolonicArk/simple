(ns simpleArk.miMap
  (:require [simpleArk.mapish :as mapish]
            [simpleArk.reader :as reader])
  (:import (clojure.lang Reversible
                         Seqable
                         ILookup
                         IPersistentCollection
                         Associative
                         IPersistentVector
                         PersistentTreeMap)))

(set! *warn-on-reflection* true)

(declare ->MI-map)

(defn new-MI-map
  [& keyvals]
  (let [m (apply sorted-map-by mapish/vec-comp keyvals)]
    (->MI-map m nil nil nil nil)))

(defn load-map
  [data-map]
  (let [m (into (sorted-map-by mapish/vec-comp) data-map)]
    (->MI-map data-map nil nil nil nil)))

(defn register
  [component-map]
  (reader/register-tag-parser! component-map 'miMap/MI-map load-map))

(deftype MI-map [^PersistentTreeMap sorted-map start-test start-path end-test end-path]
  ILookup
  (valAt [this key]
    (if (mapish/in-range key start-test start-path end-test end-path)
      (get sorted-map key)
      nil))
  (valAt [this key not-found]
    (if (mapish/in-range key start-test start-path end-test end-path)
      (get sorted-map key not-found)
      not-found))
  Seqable
  (seq [this]
    (cond
      (empty? sorted-map)
      nil
      (or start-path end-path)
      (let [start-test (if start-path
                         start-test
                         >=)
            start-path (if start-path
                         start-path
                         (key (first sorted-map)))
            end-test (if end-path
                       end-test
                       <=)
            end-path (if end-path
                       end-path
                       (key (last sorted-map)))]
        (subseq sorted-map start-test start-path end-test end-path))
      :else
      (seq sorted-map)))
  IPersistentCollection
  (count [this]
    (count (seq this)))
  (cons [this o]
    (if (instance? java.util.Map$Entry o)
      (assoc this (key o) (val o))
      (if (instance? IPersistentVector o)
        (if (not= (count o) 2)
          (throw (IllegalArgumentException. "Vector arg to map conj must be a pair"))
          (assoc this (o 0) (o 1)))
        (let [s (seq o)]
          (reduce
            (fn [mi me] (assoc mi (key me) (val me)))
            this
            s)))))
  Associative
  (containsKey [this path]
    (and (mapish/in-range path start-test start-path end-test end-path)
             (contains? sorted-map path)))
  (entryAt [this path]
    (if (mapish/in-range path start-test start-path end-test end-path)
      (.entryAt sorted-map path)
      nil))
  (assoc [this path value]
    (if (mapish/in-range path start-test start-path end-test end-path)
      (->MI-map
        (assoc sorted-map path value)
        start-test start-path end-test end-path)
      this))
  Reversible
  (rseq [this]
    (cond
      (empty? sorted-map)
      nil
      (or start-path end-path)
      (let [start-test (if start-path
                         start-test
                         >=)
            start-path (if start-path
                         start-path
                         (key (first sorted-map)))
            end-test (if end-path
                       end-test
                       <=)
            end-path (if end-path
                       end-path
                       (key (last sorted-map)))]
        (rsubseq sorted-map start-test start-path end-test end-path))
      :else
      (rseq sorted-map)))
  mapish/MI
  (mi-sub
    [this prefix]
    (let [[s-test s-path e-test e-path]
          (mapish/mi-munge prefix start-test start-path end-test end-path)]
      (if (and
            (= s-test start-test)
            (= e-test end-test)
            (= 0 (compare s-path start-path))
            (= 0 (compare e-path end-path)))
        this
        (->MI-map sorted-map s-test s-path e-test e-path))))
  (mi-sub
    [this stest spath etest epath]
    (let [[s-test s-path e-test e-path]
          (mapish/mi-munge start-test start-path end-test end-path stest spath etest epath)]
      (if (and
            (= s-test start-test)
            (= e-test end-test)
            (= 0 (compare s-path start-path))
            (= 0 (compare e-path end-path)))
        this
        (->MI-map sorted-map s-test s-path e-test e-path)))))

(defmethod print-method MI-map [this ^java.io.Writer w]
  (.write w "#miMap/MI-map { ")
  (reduce (fn [_ i] (.write w (pr-str (key i) (val i)))) nil this)
  (.write w "}"))

