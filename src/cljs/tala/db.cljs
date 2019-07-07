(ns tala.db
  (:require [medley.core :as medley]))

(def default-db
  {:session-id (medley/random-uuid)
   :active-panel :login
   :user nil
   :users []
   :current-channel-id #uuid "5e865999-00af-403f-8b88-ee5d10f921e1"
   :channels []
   :messages []
   :direct-message-reciever nil
   :forms {}})
