(ns tala.subs
  (:require
   [re-frame.core :as re-frame]))


(defn message-belongs-to-us? [to-user from-user message]
  (= #{(:id to-user)
       (:id from-user)}
     #{(get-in message [:to-user :id])
       (get-in message [:from-user :id])}))

(re-frame/reg-sub
 :session-id
 (fn [db]
   (:session-id db)))


(re-frame/reg-sub
 ::active-panel
 (fn [db]
   (:active-panel db)))


(re-frame/reg-sub
 ::user
 (fn [db]
   (:user db)))


(re-frame/reg-sub
 ::users
 (fn [db]
   (:users db)))


(re-frame/reg-sub
 ::channels
 (fn [db]
   (:channels db)))


(re-frame/reg-sub
 ::current-channel-id
 (fn [db]
   (:current-channel-id db)))


(re-frame/reg-sub
 ::messages
 (fn [db]
   (:messages db)))


(re-frame/reg-sub
 ::direct-message-reciever
 (fn [db]
   (:direct-message-reciever db)))


(re-frame/reg-sub
 ::channel-messages
 :<- [::messages]
 :<- [::current-channel-id]
 (fn [[messages current-channel-id] _]
   (filter (fn [m] (and (= (:m-type m) :channel-message)
                        (= (:channel-id m) current-channel-id)))
           messages)))




(re-frame/reg-sub
 ::direct-messages
 :<- [::messages]
 :<- [::direct-message-reciever]
 :<- [::user]
 (fn [[messages direct-message-reciever user] _]
   (filter #(and (= (:m-type %) :direct-message)
                 (message-belongs-to-us? direct-message-reciever user %))
           messages)))


(re-frame/reg-sub
 ::active-users
 :<- [::users]
 :<- [::user]
 :<- [::messages]
 (fn [[users user messages] _]
   (->> users
        (remove #(= (:id %)
                    (:id user)))
        (map (fn [u]
               (assoc u :direct-message-count (->> messages
                                                   (filter #(message-belongs-to-us? u user %))
                                                   count)))))))

(re-frame/reg-sub
 ::available-channels
 :<- [::channels]
 :<- [::current-channel-id]
 (fn [[channels current-channel-id]]
  (mapv
   (fn [{:keys [id] :as channel}]
     (if (= id current-channel-id)
       (assoc channel :active true)
       channel))
   channels)))




(re-frame/reg-sub
 ::forms
 (fn [db [_ k]]
   (get-in db [:forms k])))


(comment
  (def db @re-frame.db/app-db)
  (->>
       :users)
  )
