(set-env!
  :dependencies '[[org.clojure/clojure                       "1.9.0-alpha5"  :scope "provided"]
                  [adzerk/boot-test                          "1.1.1"         :scope "test"]]
  :source-paths #{"src/clj" "test/clj"}
)

(require
  '[adzerk.boot-test            :refer :all])

(deftask test-it
   "Setup, compile and run the tests."
   []
   (comp
     (run-tests)
     ))
