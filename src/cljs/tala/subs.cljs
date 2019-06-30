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
   (:users db)))
