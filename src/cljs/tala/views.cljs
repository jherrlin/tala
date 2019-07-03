(ns tala.views
  (:require
   [day8.re-frame-10x.view.event :as event]
   [goog.dom.forms :as gforms]
   [goog.i18n.DateTimeFormat]
   [re-frame.core :refer [dispatch] :as re-frame]
   [tala.events :as events]
   [tala.subs :as subs]
   [tala.components :as components]
   [tala.utils :refer [silent]]))

(defn datetime-format [datetiem]
  (when (inst? datetiem)
    (.format
     (goog.i18n.DateTimeFormat. "yyyy-MM-dd HH:MM")
     datetiem)))

(defn message-view []
  (let [messages @(re-frame/subscribe [::subs/messages])]
    [:div
     [:h4 "Messages"]
     (into [:ul] ;; användare mapv istället för for
           (for [{:keys [id msg datetime username]} messages]
             ^{:key id} [:li
                             (str (datetime-format datetime) " | " username " > "  msg)]))]))

(defn users-view []
  (let [users @(re-frame/subscribe [::subs/users])
        user @(re-frame/subscribe [::subs/user])]
    [:div
     [:h4 (str "Me: " (:username user))]
     [:h4 "Active users:"]
     (into [:ul] ;; användare mapv istället för for
           (for [{:keys [user-id username] :as u} users]
             ^{:key user-id} [:li
                              [:a {:href "#"
                                   :on-click #(do (dispatch [::events/direct-message-reciever u ]))}
                               username]]))]))

(defn input-view []
  (let [internal-state (reagent.core/atom "")]
    (fn []
      [:div
       [:form {:on-submit (fn [x]
                            (.preventDefault x)
                            (dispatch [::events/send-channal-msg @internal-state]))}
        [:input {:type "text"
                 :auto-focus true
                 :value @internal-state
                 :placeholder ""
                 :on-change #(let [value (-> % .-target .-value)]
                               (reset! internal-state value))}]
        [:br]
        [:button {:type "submit"
                  :class "button-primary"} "Send"]]])))

(defn chat-view []
  [:div
   [:h2 "Chat"]
   [users-view]
   [message-view]
   [input-view]])


(defn login-view []
  [:div
   [components/input-form :login-form "login" #(dispatch [::events/login-user %1])]])

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
     [components/input-form :send-direct-message "Send" #(dispatch [::events/send-direct-msg %1])]
     ]))

(defn app-container []
  (case @(re-frame/subscribe [::subs/active-panel])
    :login [login-view]
    :chat [chat-view]
    :direct-message [direct-message-view]))
