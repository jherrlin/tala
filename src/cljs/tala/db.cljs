(ns tala.db)

(def default-db
  {:active-panel :login
   :user nil
   :users []
   :current-channel #uuid "5e865999-00af-403f-8b88-ee5d10f921e1"
   :channels {}
   :messages []
   :direct-message-reciever nil
   :direct-messages []
   :forms {}})
