(ns tala.views
  (:require
   [day8.re-frame-10x.view.event :as event]
   [goog.dom.forms :as gforms]
   [goog.i18n.DateTimeFormat]
   [re-frame.core :refer [dispatch] :as re-frame]
   [tala.events :as events]
   [tala.subs :as subs]
   [tala.components :as components]))


(defn datetime-format [datetiem]
  (when (inst? datetiem)
    (.format
     (goog.i18n.DateTimeFormat. "yyyy-MM-dd HH:MM")
     datetiem)))


(defn channels-view []
  (let [channels (re-frame/subscribe [::subs/available-channels])]
    [:div
     [:h4 "Available channels"]
     [into [:ul]
      (for [{:keys [id name active] :as channel} @channels]
        ^{:key id} [:li {:style {:background-color (when active "lightgrey")}}
                    [:a {:href "#"
                             :on-click #(dispatch [::events/change-channel id])}
                         name]])]]))


(defn users-view []
  (let [users @(re-frame/subscribe [::subs/active-users])
        user @(re-frame/subscribe [::subs/user])]
    [:div {:style {:order 0
                   :flex-basis "auto"}}
     [:h4 "Active users"]
     (into [:ul] ;; användare mapv istället för for
           (for [{:keys [user-id username direct-message-count] :as u} users]
             ^{:key user-id} [:li [:a {:href "#"
                                       :on-click #(dispatch [::events/direct-message-reciever u])}
                                   (str username (when (< 0 direct-message-count)
                                                   (str " (" direct-message-count ")")))]]))]))


(defn message-view []
  (let [messages (re-frame/subscribe [::subs/channel-messages])]
    [:div {:style {:order 1
                   :flex-basis "auto"
                   :justify-content "center"}}
     [:h4 {:align "center"} "Messages"]
     (into [:ul {:style {:list-style "none"
                         :padding-inline-start 0}}] ;; användare mapv istället för for
           (for [{:keys [id msg datetime username]} @messages]
             ^{:key id} [:li (str (datetime-format datetime) " | " username " > "  msg)]))
     [components/input-form :channel-input-form "Send" "Channel message" #(dispatch [::events/send-channal-msg %1])]]))


(defn user-details []
  (let [user @(re-frame/subscribe [::subs/user])]
    [:div {:style {:order 2
                   :flex-basis "auto"}}
     [:h4 "User"]
     [:p (:username user)]
     [:p (datetime-format (:datetime user))]]))



(defn chat-view []
  [:div {:style {:display "flex"
                 :flex-direction "row"
                 :justify-content "space-around"
                 :flex-wrap "nowrap"}}
   [users-view]
   [message-view]
   [channels-view]
   [user-details]])


(defn login-view []
  [components/input-form :login-form "login" "Enter username" #(dispatch [::events/login-user %1])])


(defn direct-message-view []
  (let [direct-message-reciever @(re-frame/subscribe [::subs/direct-message-reciever])
        direct-messages (re-frame/subscribe [::subs/direct-messages])]
    [:div
     [:a {:href "#"
          :on-click #(dispatch  [::events/active-panel :chat])} "Back"]
     [:h2 "Direct messages with: " (:username direct-message-reciever)]
     (into [:ul] ;; användare mapv istället för for
           (for [{:keys [id from-user to-user msg datetime]} @direct-messages]
             ^{:key id} [:li
                         (str (datetime-format datetime) " | " (:username from-user) " -> " (:username to-user) " | " msg)]))
     [components/input-form :send-direct-message "Send" "Direct message" #(dispatch [::events/send-direct-msg %1])]]))


(defn app-container []
  (case @(re-frame/subscribe [::subs/active-panel])
    :login [login-view]
    :chat [chat-view]
    :direct-message [direct-message-view]))
