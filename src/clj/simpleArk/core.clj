(ns simpleArk.core
  (:require [clj-uuid :as uuid]))

(defprotocol Ark-db
  (get-ark [this]
    "returns the current value of the ark")
  (register-transaction! [this transaction-name f]
    "defines a transaction,
    where f takes an ark, a new journal-entry rolon and an (edn) string,
    and then returns a revised ark")
  (process-transaction [this transaction-name s]
    "process a transaction with an (edn) string,
    returning the new journal-entry uuid"))

(defrecord Ark [get-rolon get-journal-entries create-rolon destroy-rolon update-property])

(defrecord Rolon [rolon-uuid get-rolon-values])

(defrecord Rolon-value [value-rolon journal-entry-uuid
                        get-property-values get-property-journal-entry-uuids])

(defn get-rolon
  "returns the rolon identified by the uuid, or nil"
  [ark uuid]
  ((:get-rolon ark) uuid))

(defn get-journal-entries
  "returns a sorted set of all the journal entry rolons"
  [ark]
  ((:get-journal-entries ark)))

(defn create-rolon
  "returns a revised ark with the new rolon"
  [ark rolon-uuid]
  ((:create-rolon ark) rolon-uuid))

(defn destroy-rolon
  "deletes all the classifiers of a rolon"
  [ark rolon-uuid]
  ((:destroy-rolon ark) rolon-uuid))

(defn update-property
  "update the value of a property of a rolon"
  [ark rolon-uuid property-name property-value]
  ((:update-property ark) rolon-uuid property-name property-value))

(defn get-rolon-uuid
  "returns the uuid of the rolon"
  [rolon]
  (:rolon-uuid rolon))

(defn get-rolon-values
  "returns a sorted set of all the values of a rolon"
  [rolon]
  ((:get-rolon-values rolon)))

(defn get-journal-entry-uuid
  "returns the type-1 uuid of the journal entry rolon which created this rolon value"
  [rolon-value]
  (:journal-entry-uuid rolon-value))

(defn get-property-keys
  "returns a sorted set of the keys of all the properties assigned to this or a previous rolon value"
  [rolon-value]
  ((:get-property-keys rolon-value)))

(defn get-property-values
  "returns the values of the properties, nil indicating the property is no longer present"
  [rolon-value]
  ((:get-property-values rolon-value)))

(defn get-property-journal-entry-uuids
  "returns the type 1 uuid of the journal entry rolons which changed each property"
  [rolon-value]
  ((get-property-journal-entry-uuids rolon-value)))

(defn get-value-rolon
  "returns the rolon"
  [rolon-value]
  (:value-rolon rolon-value))

(defn get-latest-rolon-value
  "returns the latest rolon value"
  [rolon]
  (val (last (get-rolon-values rolon))))

(defn get-updated-rolon-uuids
  "returns the uuids of the rolons updated by a journal-entry rolon"
  [journal-entry]
  (let [latest-rolon-value (get-latest-rolon-value journal-entry)
        updated-rolon-uuids (:descriptor:updated-rolon-uuids (get-property-values latest-rolon-value))]
    (if (nil? updated-rolon-uuids)
      (sorted-set)
      updated-rolon-uuids)))

(defn get-previous-value
  "returns the previous rolon value for the same rolon, or nil"
  [rolon-value]
  (let [journal-entry-uuid (get-journal-entry-uuid rolon-value)
        rolon (get-value-rolon rolon-value)
        rolon-values (get-rolon-values rolon)
        previous-rolon-values (rsubseq rolon-values < journal-entry-uuid)]
    (val (first previous-rolon-values))))

(defn get-index
  "returns a sorted map of lists of rolon uuids keyed by classifier value"
  [index-rolon]
  (let [latest-rolon-value (get-latest-rolon-value index-rolon)
        index (:descriptor:index (get-property-values latest-rolon-value))]
    (if (nil? index)
      (sorted-map)
      index)))
