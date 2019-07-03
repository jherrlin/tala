(ns tala.events
  (:require
   [chord.client :refer [ws-ch]]
   [cljs.core.async :as async :include-macros true]
   [clojure.spec.alpha :as s]
   [common.spec.messages :as message-spec]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   [medley.core :as medley]
   [re-frame.core :as re-frame]
   [tala.db :as db]))


(goog-define ws-url "ws://localhost:3449/ws")
(defonce send-chan (async/chan))
(declare handle-message)


;; Websocket stuff
(defn send-msg
  [msg]
  (async/put! send-chan msg))


(defn- send-msgs
  [svr-chan]
  (async/go-loop []
    (when-let [msg (async/<! send-chan)]
      (async/>! svr-chan msg)
      (recur))))

;; bryt ut till ett egen ns
;; multimethod här istället för case
;; spec som validerar messages
(defn- receive-msgs
  [svr-chan]
  (async/go-loop []
    (if-let [{:keys [message]} (<! svr-chan)]
      (do
        (handle-message message)
        (recur))
      (println "Websocket closed"))))


(defn setup-websockets! []
  (async/go
    (let [{:keys [ws-channel error]} (async/<! (ws-ch ws-url))]
      (if error
        (println "Something went wrong with the websocket!")
        (do
          (send-msgs ws-channel)
          (receive-msgs ws-channel))))))


(defmulti handle-message (fn [data] (:m-type data)))


(defmethod handle-message :default [data]
  (js/console.log "handle-message :default:" data))


(defmethod handle-message :server->init-user [data]
  (let [k ::message-spec/server->init-user]
    (if (s/valid? k data)
      (re-frame/dispatch [::add-users (:users data)])
      (s/explain k data))))


(defmethod handle-message :new-user [data]
  (let [k ::message-spec/new-user]
    (if (s/valid? k data)
      (do
        (re-frame/dispatch [::add-user data])
        (re-frame/dispatch [::add-message {:id (medley/random-uuid)
                                           :datetime (js/Date.)
                                           :msg (str "User joined: " (get-in data [:username]))
                                           :username "Server"}]))
      (s/explain k data))))


(defmethod handle-message :user-left [data]
  (let [k ::message-spec/new-user]
    (if (s/valid? k data)
      (do
        (re-frame/dispatch [::remove-user data])
        (re-frame/dispatch [::add-message {:id (medley/random-uuid)
                                           :datetime (js/Date.)
                                           :msg (str "User left: " (get-in data [:username]))
                                           :username "Server"}]))
      (s/explain k data))))


(defmethod handle-message :channel-message [data]
  (let [k ::message-spec/channel-message]
    (if (s/valid? k data)
      (re-frame/dispatch [::add-message data])
      (s/explain k data))))


(defmethod handle-message :direct-message [data]
  (let [k ::message-spec/direct-message]
    (if (s/valid? k data)
      (re-frame/dispatch [::add-direct-message data])
      (s/explain k data))))




(defmethod handle-message :default [data]
  (js/console.log "john-debug handle-message default:" data))


(re-frame/reg-cofx
  ::uuid
  (fn [cofx _]
    (assoc cofx :uuid (medley/random-uuid))))


(re-frame/reg-cofx
  ::user
  (fn [{:keys [db] :as cofx} _]
    (assoc cofx :user (:user db))))

(re-frame/reg-cofx
  ::to-user
  (fn [{:keys [db] :as cofx} _]
    (assoc cofx :to-user (:direct-message-reciever db))))

(re-frame/reg-cofx
 ::now
 (fn [coeffects _]
   (assoc coeffects :now (js.Date.))))   ;; add :now key, with value


(re-frame/reg-fx
 ::ws-init
 (fn [_]
   (js/console.log "john-debug: ::ws-init")
   (setup-websockets!)))


(re-frame/reg-fx
 ::ws-send
 (fn [msg]
   (send-msg msg)))


(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            db/default-db))


(re-frame/reg-event-db
 ::add-message
 (fn [db [_ message]]
   (update db :messages conj message)))

(re-frame/reg-event-db
 ::add-direct-message
 (fn [db [_ message]]
   (update db :direct-messages conj message)))

(re-frame/reg-event-db
 ::forms
 (fn [db [_ k value]]
   (assoc-in db [:forms k] value)))

(re-frame/reg-event-db
 ::add-user
 (fn [db [_ user]]
   (-> db
       (update :users conj user))))

(re-frame/reg-event-db
 ::active-panel
 (fn [db [_ active-panel]]
   (-> db
       (assoc :active-panel active-panel))))

(re-frame/reg-event-db
 ::direct-message-reciever
 (fn [db [_ user]]
   (-> db
       (assoc :direct-message-reciever user)
       (assoc :active-panel :direct-message))))

(re-frame/reg-event-db
 ::remove-user
 (fn [db [_ user]]
   (update db :users (fn [x] (remove #(= (:user-id %) (:user-id user)) x)))))


(re-frame/reg-event-db
 ::add-users
 (fn [db [_ users]]
   (if (empty? users)
     db
     (update db :users concat users))))

(re-frame/reg-event-fx
 ::send-direct-msg
 [(re-frame/inject-cofx ::uuid)
  (re-frame/inject-cofx ::now)
  (re-frame/inject-cofx ::user)
  (re-frame/inject-cofx ::to-user)]
 (fn [{:keys [db now uuid user to-user] :as cofx} [_ message]]
   (let [data {:id uuid
               :m-type :direct-message
               :from-user user
               :to-user to-user
               :datetime now
               :msg message}]
     (if (s/valid? ::message-spec/direct-message data)
       {::ws-send data
        :db (update db :direct-messages conj data)}
       (do (s/explain ::message-spec/direct-message data)
         {:db db})))))

(re-frame/reg-event-fx
 ::send-channal-msg
 [(re-frame/inject-cofx ::uuid)
  (re-frame/inject-cofx ::now)
  (re-frame/inject-cofx ::user)]
 (fn [{:keys [db now uuid user] :as cofx} [_ message]]
   (let [data {:id uuid
               :m-type :channel-message
               :user-id (:user-id user)
               :username (:username user)
               :datetime now
               :msg message}]
     (if (s/valid? ::message-spec/channel-message data)
       {::ws-send data}
       (s/explain ::message-spec/channel-message data)))))


(re-frame/reg-event-fx
 ::login-user
 [(re-frame/inject-cofx ::uuid)
  (re-frame/inject-cofx ::now)]
 (fn [{:keys [db now uuid] :as cofx} [_ username]]
   (let [data {:id uuid
               :m-type :init-user->server
               :user-id uuid
               :username username
               :datetime now}]
     (if (s/valid? ::message-spec/init-user->server data)
       {::ws-init nil
        ::ws-send data
        :db (assoc db :user data :active-panel :chat)}
       (do
         (s/explain ::message-spec/init-user->server data)
         {:db db})))))


(re-frame/reg-event-fx
 ::send-to-channel
 [(re-frame/inject-cofx ::uuid)
  (re-frame/inject-cofx ::now)
  (re-frame/inject-cofx ::user-id)]
 (fn [{:keys [db now uuid] :as cofx} [_ message]]
   (let [msg {uuid {:message-id uuid
                    :user-id (->> db :user vals first :user-id)
                    :channel-id (:current-channel db)
                    :message message}}]
     {::ws-send {:m-type :channel-message
                 :msg msg}})))


(comment
  (->> @re-frame.db/app-db
       :users)
  )
