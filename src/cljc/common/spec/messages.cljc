(ns common.spec.messages
  (:require [clojure.spec.alpha :as s]
            [medley.core]))



(s/def ::id medley.core/uuid?)
(s/def ::m-type #{:direct-message :channel-message :server-message})
(s/def ::datetime inst?)
(s/def ::from-user medley.core/uuid?)
(s/def ::to-user medley.core/uuid?)
(s/def ::msg string?)

(s/def ::direct-message
  (s/keys
   :req-un [::id
            ::m-type
            ::datetime
            ::from-user
            ::to-user
            ::msg]))



(comment
  (s/valid? ::direct-message {:id (medley.core/random-uuid)
                              :m-type :direct-message
                              :datetime  #?(:clj (java.util.Date.)
                                            :cljs (js/Date.))
                              :from-user (medley.core/random-uuid)
                              :to-user (medley.core/random-uuid)
                              :msg "Hejsan"})

  ;; Använd det såhär
  ;; user> (require '[common.spec.messages :as messages])
  ;; nil
  ;; user> (doc ::messages/direct-message)

  )
