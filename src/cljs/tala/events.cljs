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

(defn send-msg
  [msg]
  (async/put! send-chan msg))

(defn- send-msgs
  [svr-chan]
  (async/go-loop []
    (when-let [msg (async/<! send-chan)]
      (async/>! svr-chan msg)
      (recur))))


(defmulti handle-message (fn [data] (:m-type data)))

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
        (re-frame/dispatch [::add-message {:datetime (js/Date.)
                                           :msg (str "User joined: " (get-in data [:username]))
                                           :msg-id (medley/random-uuid)}]))
      (s/explain k data))))

(defmethod handle-message :user-left [data]
  (let [k ::message-spec/new-user]
    (if (s/valid? k data)
      (do
        (re-frame/dispatch [::remove-user data])
        (re-frame/dispatch [::add-message {:datetime (js/Date.)
                                           :msg (str "User left: " (get-in data [:username]))
                                           :msg-id (medley/random-uuid)}]))
      (s/explain k data))))


(defmethod handle-message :default [data]
  (js/console.log "john-debug handle-message default:" data))



;; bryt ut till ett egen ns
;; multimethod här istället för case
;; spec som validerar messages
(defn- receive-msgs
  [svr-chan]
  (async/go-loop []
    (if-let [{:keys [message]} (<! svr-chan)]
      (do
        (js/console.log "john-debug receive-msgs:" message)
        (handle-message message)
        #_(case (:m-type message)
          :init-user (handle-server-message message)
          :new-user (do (re-frame/dispatch [::add-user (:data message)])
                        (re-frame/dispatch [::add-message {:datetime (js/Date.)
                                                            :msg (str "User joined: " (get-in message [:data :user-name]))
                                                            :msg-id (medley/random-uuid)}]))
          :user-left (do (re-frame/dispatch [::remove-user (:data message)])
                         (re-frame/dispatch [::add-message {:datetime (js/Date.)
                                                            :msg (str "User left: " (get-in message [:data :user-name]))
                                                            :msg-id (medley/random-uuid)}]))
          :channel-message (re-frame/dispatch [::add-message (:data message)])
          :test (js/console.log "john-debug:" (clj->js (:data message))))
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


(re-frame/reg-cofx
  ::uuid
  (fn [cofx _]
    (assoc cofx :uuid (medley/random-uuid))))

(re-frame/reg-cofx
  ::user
  (fn [{:keys [db] :as cofx} _]
    (assoc cofx :user (:user db))))

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
 ::forms
 (fn [db [_ k value]]
   (assoc-in db [:forms k] value)))

(re-frame/reg-event-db
 ::add-user
 (fn [db [_ user]]
   (-> db
       (update :users conj user))))

(re-frame/reg-event-db
 ::add-message
 (fn [db [_ user]]
   (-> db
       (update :messages conj user))))

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
 ::send-channal-msg
 [(re-frame/inject-cofx ::uuid)
  (re-frame/inject-cofx ::now)
  (re-frame/inject-cofx ::user)]
 (fn [{:keys [db now uuid user] :as cofx} [_ message]]
   (let [data {:msg message
               :msg-id uuid
               :datetime now
               :user-id (:user-id user)}]
     {::ws-send {:m-type :channel-message :data data}})))


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
        ::ws-send {:m-type :init-user :data data}
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
