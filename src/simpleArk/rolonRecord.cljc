(ns simpleArk.rolonRecord
  (:require #?(:clj [simpleArk.reader :as reader]
               :cljs [cljs.reader :as reader])))

#?(:clj
   (set! *warn-on-reflection* true))

(defrecord Rolon-record [rolon-uuid])

(defn load-rolon [m]
  (into (->Rolon-record (:rolon-uuid m)) (:changes-by-property m)))

#?(:clj
   (defn register
     [component-map]
     (reader/register-tag-parser! component-map 'simpleArk.rolonRecord.Rolon-record load-rolon))
   :cljs
   (reader/register-tag-parser! "simpleArk.rolonRecord.Rolon-record" load-rolon))
