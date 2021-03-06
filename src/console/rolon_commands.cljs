(ns console.rolon-commands
  (:require
    [hoplon.core :as h]
    [javelin.core :as j]
    [simpleArk.mapish :as mapish]
    [simpleArk.uuid.uuid :as suuid]
    [simpleArk.arkRecord :as arkRecord]
    [console.client :as client]))

(defn list-all-micro-properties!
  [ark-record]
  (client/display-history!
    (-> []
        (client/add-prompt)
        (client/add-display ">")
        (client/add-display "list paths of all micro-properties\n" client/command-prefix-style)))
  (client/clear-output!)
  (let [uuid (suuid/create-uuid @client/selected-rolon)
        properties (arkRecord/get-changes-by-property ark-record uuid)
        display (client/add-display [] "all micro-property paths of ")
        display (client/display-value display ark-record uuid)
        display (client/add-display display ":\n\n")]
    (client/display-output!
      (reduce
        (fn [display [path _]]
          (-> display
              (client/add-display "=" client/micro-property-style client/micro-property-click path)
              (client/add-display " ")
              (client/display-path ark-record path)
              (client/add-display "\n\n")))
        display properties))))

(defn list-modified-micro-properties!
  [ark-record]
  (client/display-history!
    (-> []
        (client/add-prompt)
        (client/add-display ">")
        (client/add-display "list modified micro-properties\n" client/command-prefix-style)))
  (client/clear-output!)
  (let [uuid (suuid/create-uuid @client/selected-rolon)
        properties (arkRecord/get-changes-by-property ark-record uuid)
        display (client/add-display [] "modified micro-properties of ")
        display (client/display-value display ark-record uuid)
        display (client/add-display display ":\n\n")]
    (client/display-output!
      (reduce
        (fn [display [path value]]
          (let [[[k] v] (first value)
                st (suuid/rolon-key (suuid/create-uuid @client/selected-time))]
            (if (= k st)
              (-> display
                  (client/add-display "=" client/micro-property-style client/micro-property-click path)
                  (client/add-display " ")
                  (client/display-property ark-record path v)
                  (client/add-display "\n\n"))
              display)))
        display properties))))

(defn list-modifying-transactions!
  [ark-record]
  (client/display-history!
    (-> []
        (client/add-prompt)
        (client/add-display ">")
        (client/add-display "list modifying transactions\n" client/command-prefix-style)))
  (client/clear-output!)
  (let [uuid (suuid/create-uuid @client/selected-rolon)
        all-properties (arkRecord/get-property-values ark-record uuid)
        properties (mapish/mi-sub all-properties [:inv-rel/modified])
        display (client/add-display [] "transactions that modifified ")
        display (client/display-value display ark-record uuid)
        display (client/add-display display "\n\n")]
    (client/display-output!
      (reduce
        (fn [display [path value]]
          (let [k (second path)
                u (arkRecord/get-journal-entry-uuid ark-record k)
                display (client/display-value display ark-record u)
                headline (arkRecord/get-property-value
                           ark-record
                           u
                           [:index/headline])
                display (if (some? headline)
                          (client/add-display display (str " - " headline))
                          display)
                display (client/add-display display "\n")]
            display))
        display properties))))

(defn do-rolon-commands
  []
  (h/div
    (h/div
      (h/span
        (h/strong "Selected Rolon: "))
      (h/span
        (h/text
          (if (= "" client/selected-rolon)
            "none"
            (client/pretty-value client/my-ark-record (suuid/create-uuid client/selected-rolon)))))
      )

    (h/div
      :css {:display "none"}
      :toggle (j/cell= (and
                         (not= "" client/selected-rolon)
                         (some? (arkRecord/get-property-value
                                  client/my-ark-record
                                  (suuid/create-uuid client/selected-rolon)
                                  [:index/headline]))))
      (h/text
        (str
          "headline: "
          (if (not= "" client/selected-rolon)
            (arkRecord/get-property-value
              client/my-ark-record
              (suuid/create-uuid client/selected-rolon)
              [:index/headline])))))

    (h/div
      :css {:display "none"}
      :toggle (j/cell= (not= "" client/selected-rolon))

      (h/button
        :style "background-color:MistyRose"
        :click (fn []
                 (reset! client/display-mode 0)
                 (client/add-prompt!)
                 (client/add-history! ">")
                 (client/add-history! "clear rolon selection\n" client/command-prefix-style)
                 (reset! client/selected-rolon ""))
        "clear rolon selection")

      (h/button
        :css {:display "none" :background-color "MistyRose"}
        :toggle (j/cell= (not (suuid/journal-entry-uuid? (suuid/create-uuid client/selected-rolon))))
        :click (fn []
                 (reset! client/display-mode 0)
                 (list-modifying-transactions! @client/my-ark-record))
        "list modifying transactions")

      (h/button
        :style "background-color:MistyRose"
        :click (fn []
                 (reset! client/display-mode 0)
                 (list-all-micro-properties! @client/my-ark-record))
        "list paths of all micro-properties")

      (h/button
        :css {:display "none" :background-color "MistyRose"}
        :toggle (j/cell= (and
                           (not (suuid/journal-entry-uuid? (suuid/create-uuid client/selected-rolon)))
                           (not= "" client/selected-time)))
        :click (fn []
                 (reset! client/display-mode 0)
                 (list-modified-micro-properties! @client/my-ark-record))
        "list modified micro-properties")
      )
    ))
