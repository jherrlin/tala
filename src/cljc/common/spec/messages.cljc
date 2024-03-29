(ns common.spec.messages
  (:require [clojure.spec.alpha :as s]
            [medley.core]))


(s/def ::id medley.core/uuid?)
(s/def ::session-id medley.core/uuid?)
(s/def ::m-type #{:init-user->server :server->init-user :new-user :user-left
                  :direct-message :channel-message :server-message})
(s/def ::datetime inst?)
(s/def ::from-user ::id)
(s/def ::to-user ::id)
(s/def ::name string?)
(s/def ::msg string?)
(s/def ::username string?)
(s/def ::user-id ::id)
(s/def ::channel-id ::id)
(s/def ::user
  (s/keys :req-un [::id
                   ::username]))
(s/def ::users (s/coll-of ::user :kind vector?))
(s/def ::from-user ::user)
(s/def ::to-user ::user)
(s/def ::channel
  (s/keys :req-un [::id
                   ::name]))
(s/def ::channels (s/coll-of ::channel :kind vector?))



(s/def ::direct-message
  (s/keys
   :req-un [::id
            ::session-id
            ::m-type
            ::datetime
            ::from-user
            ::to-user
            ::msg]))

(s/def ::init-user->server
  (s/keys :req-un [::id
                   ::session-id
                   ::m-type
                   ::user-id
                   ::username
                   ::datetime]))

(s/def ::server->init-user
  (s/keys :req-un [::m-type
                   ::users
                   ::channels]))

(s/def ::new-user
  (s/keys :req-un [::id
                   ::session-id
                   ::user-id
                   ::m-type
                   ::username
                   ::datetime]))

(s/def ::user-left
  (s/keys :req-un [::id
                   ::session-id
                   ::user-id
                   ::m-type
                   ::username
                   ::datetime]))

(s/def ::new-channel
  (s/keys :req-un [::id
                   ::session-id
                   ::m-type
                   ::name
                   ::datetime]))


(s/def ::channel-message
  (s/keys :req-un [::id
                   ::session-id
                   ::m-type
                   ::user-id
                   ::channel-id
                   ::username
                   ::datetime
                   ::msg]))





(comment


  (s/explain ::direct-message {:id (medley.core/random-uuid)
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
