(ns tala.views
  (:require
   [day8.re-frame-10x.view.event :as event]
   [goog.dom.forms :as gforms]
   [goog.i18n.DateTimeFormat]
   [re-frame.core :refer [dispatch] :as re-frame]
   [tala.events :as events]
   [tala.subs :as subs]
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
           (for [{:keys [msg-id msg datetime]} messages]
             ^{:key msg-id} [:li (str (datetime-format datetime) " | " msg)]))]))

(defn users-view []
  (let [users @(re-frame/subscribe [::subs/users])
        user @(re-frame/subscribe [::subs/user])]
    [:div
     [:h4 (str "Me: " (:user-name user))]
     [:h4 "Users:"]
     (into [:ul] ;; användare mapv istället för for
           (for [{:keys [user-id user-name]} users]
             ^{:key user-id} [:li user-name]))]))

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
  (let [internal-state (reagent.core/atom "")]
    (fn []
      [:div
       [:form {:on-submit (fn [x]
                            (.preventDefault x)
                            (dispatch [::events/login-user @internal-state]))}
        [:input {:type "text"
                 :auto-focus true
                 :value @internal-state
                 :placeholder "Pick a username"
                 :on-change #(let [value (-> % .-target .-value)]
                               (reset! internal-state value))}]
        [:br]
        [:button {:type "submit"
                  :class "button-primary"} "Start chatting"]]])))

(defn app-container []
  (case @(re-frame/subscribe [::subs/active-panel])
    :login [login-view]
    :chat [chat-view]))
