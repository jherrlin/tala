(ns tala.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-panel
 (fn [db]
   (:active-panel db)))

(re-frame/reg-sub
 ::messages
 (fn [db]
   (:messages db)))

(re-frame/reg-sub
 ::direct-message-reciever
 (fn [db]
   (:direct-message-reciever db)))

(re-frame/reg-sub
 ::direct-messages
 (fn [db]
   (->> db
        :direct-messages
        (filter #(= #{(get-in db [:user :id])
                      (get-in db [:direct-message-reciever :id])}
                    #{(get-in % [:to-user :id])
                      (get-in % [:from-user :id])})))))


(comment
  (def db @re-frame.db/app-db)
  (->>
       :users)
  )


(re-frame/reg-sub
 ::users
 (fn [db]
   (->> db
        :users
        (remove #(= (:user-id %)
                    (->> db :user :user-id)))
        (map (fn [x] (assoc x :direct-message-count (->> db
                                                         :direct-messages
                                                         (filter (fn [m]
                                                                   (= (:id x)
                                                                      (get-in m [:from-user :id]))))
                                                         count)))))))

(re-frame/reg-sub
 ::user
  (fn [db]
   (:user db)))

(re-frame/reg-sub
 ::forms
 (fn [db [_ k]]
   (get-in db [:forms k])))
