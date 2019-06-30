(ns tala.views
  (:require
   [day8.re-frame-10x.view.event :as event]
   [goog.dom.forms :as gforms]
   [re-frame.core :refer [dispatch] :as re-frame]
   [tala.events :as events]
   [tala.subs :as subs]
   [tala.utils :refer [silent]]))

(defn message-view []
  (let [messages @(re-frame/subscribe [::subs/messages])]
    [:div
     [:h4 "Messages"]]))

(defn users-view []
  (let [users @(re-frame/subscribe [::subs/users])]
    [:div
     [:h4 "Users"]
     (into [:ul] ;; användare mapv istället för for
           (for [{:keys [user-id user-name]} users]
             ^{:key user-id} [:li user-name]))]))

(defn chat-view []
  [:div
   [:h2 "Chat"]
   [users-view]
   [message-view]])

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
