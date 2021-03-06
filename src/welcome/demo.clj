(ns welcome.demo
  (:require [tiples.server :as tiples]
            [profile.server]
            [console.server :as console]
            [tiples.users :as users]
            [simpleArk.arkDb.ark-db :as ark-db]
            [simpleArk.builder :as builder]
            [welcome.demo-builds :as demo-builds]
            [simpleArk.sub.sub :as sub]
            [welcome.demo-actions]))

(users/add-capability! :welcome)
(users/add-capability! :profile)
(users/add-capability! :contacts)
(users/add-capability! :console)

(ark-db/open-ark! users/ark-db)

(builder/transaction!
  users/ark-db
  {}
  (-> []
      (builder/build-je-property [:index/headline] "Build demo data")
      (demo-builds/build-demo)
      ))

(console/initializer)

(def handler tiples/routes)

(defmethod users/get-common :welcome [capability-kw] {})

(sub/subscribe! users/ark-db :console console/notify-colsole)
