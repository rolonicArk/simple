(ns simpleArk.builder
  #?(:clj
     (:require
       [simpleArk.ark-db :as ark-db]
       [console.server :as console]
       [simpleArk.mapish :as mapish])
     :cljs
     (:require
       [tiples.client :as tiples]
       [simpleArk.mapish :as mapish])))

#?(:clj
   (set! *warn-on-reflection* true))

(defmulti
  pretty-action
  (fn [v]
    (mapish/action-type v)))

(defmethod pretty-action :default
  [v]
  (pr-str v))

(defn build-property
  [actions rolon-uuid path value]
  (conj actions [(first path) rolon-uuid path value]))

(defn build-je-property
  [actions path value]
  (conj actions [(first path) :je path value]))

(defmethod pretty-action :property
  [[kw rolon-uuid path value]]
  (str "property (uuid " rolon-uuid ")." (pr-str path) " <= " (pr-str value)))

(defn build-relation
  [actions kw label uuid-a uuid-b value]
  (conj actions [kw label uuid-a uuid-b value]))

(defmethod pretty-action :relation
  [[kw label uuid-a uuid-b value]]
  (let [rt (cond
             (mapish/bi-rel? kw) "<->"
             (mapish/rel? kw) "->"
             (mapish/inv-rel? kw) "<-")]
    (str "relation (" kw ")." label " (uuid " uuid-a ") " rt " (uuid " uuid-b ")" value)))

(defn build-locate-first
  [actions local-kw index-kw value]
  (conj actions [:read-index-uuid local-kw index-kw value]))

(defn build-gen-uuid
  [actions s]
  (conj actions [:gen-uuid s]))

(defn build-println
  [actions s]
  (conj actions [:println s]))

(defn build-invalid
  [actions]
  (conj actions [:invalid]))

(defn build-exception
  [actions msg]
  (conj actions [:exception msg]))

(defn build-replace-map
  [actions m prefix rolon-uuid]
  (conj actions [:replace-map m prefix rolon-uuid]))

#?(:clj
   (defn transaction!
     ([ark-db local actions]
      (transaction! ark-db nil local actions))
     ([ark-db user-uuid local actions]
      (let [je-uuid
            (ark-db/process-transaction!
              ark-db
              user-uuid
              :actions-transaction!
              (pr-str [local actions]))]
        (console/notify-colsole)
        je-uuid)))
   :cljs
   (defn transaction!
     [local actions]
     (tiples/chsk-send!
       [:console/process-transaction
        {:tran-keyword :actions-transaction!
         :tran-data    (pr-str [local actions])}])))
