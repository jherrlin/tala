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
 ::users
 (fn [db]
   (->> db
        :users
        (remove #(= (:user-id %)
                    (->> db :user :user-id))))))

(re-frame/reg-sub
 ::user
  (fn [db]
   (:user db)))
