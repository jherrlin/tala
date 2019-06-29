(ns tala.events
  (:require
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   [medley.core :as medley]
   [re-frame.core :as re-frame]
   [tala.db :as db]
   [tala.websocket :as websocket]))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            db/default-db))

(re-frame/reg-cofx
  ::uuid
  (fn [cofx _]
    (assoc cofx :uuid (medley/random-uuid))))


(re-frame/reg-cofx
 ::now
 (fn [coeffects _]
   (assoc coeffects :now (js.Date.))))   ;; add :now key, with value


(re-frame/reg-fx
 ::ws-init
 (fn [_]
   (js/console.log "john-debug: ::ws-init")
   (websocket/setup-websockets!)))

(re-frame/reg-fx
 ::ws-send
 (fn [msg]
   (websocket/send-msg msg)))


(re-frame/reg-event-fx
 ::login-user
 [(re-frame/inject-cofx ::uuid)
  (re-frame/inject-cofx ::now)]
 (fn [{:keys [db now uuid] :as cofx} [_ username]]
   (let [msg {uuid {:user-name username
                    :user-id uuid}}]
     {::ws-init nil
      ::ws-send {:m-type :init-user
                 :msg msg}
      :db (assoc db :user msg :active-panel :chat)})))


(comment
  @re-frame.db/app-db
  )
