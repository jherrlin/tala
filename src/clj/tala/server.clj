(ns tala.server
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :as async]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [config.core :refer [env]]
            [medley.core :refer [random-uuid dissoc-in]]
            [org.httpkit.server :as hk]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :refer [resource-response]]
            [tala.log]
            [taoensso.timbre :as timbre
             :refer [log  trace  debug  info  warn  error  fatal  report
                     logf tracef debugf infof warnf errorf fatalf reportf
                     spy get-env]]
            [taoensso.timbre.appenders.core :as appenders]
            )
  (:gen-class))


(info "Server starting...")


(defonce main-chan (async/chan))
(defonce main-mult (async/mult main-chan))
(def app-state (atom {:users []
                      :channels [{:id #uuid "5e865999-00af-403f-8b88-ee5d10f921e1"
                                  :name "default"}
                                 {:id (random-uuid)
                                  :name "channel 1"}
                                 {:id (random-uuid)
                                  :name "channel 2"}]}))








(comment
  @app-state

  ;; Send message to alla clients
  (async/put! main-chan {:id (random-uuid)
                         :m-type :channel-message
                         :user-id (random-uuid)
                         :username "Server"
                         :datetime (java.util.Date.)
                         :msg "Hey from server!"})

  ;; Add/remove reciever
  (def -app-state (atom {:available-recievers {}}))
  (def new-reciever (let [id (random-uuid)]
                      {id {:id id
                           :type :user
                           :name (apply str (take 10 (repeatedly (fn [] (char (+ (rand 26) 97))))))}}))
  (swap! -app-state update :available-recievers merge new-reciever)  ;; add
  (swap! -app-state update :available-recievers dissoc #uuid "3a4d2231-d2e6-405c-a8cb-842a22a83d3e") ;; remove
  @-app-state





  )



(defn user-by-ws-ch [ws-ch]
  (->> @app-state
       :users
       (filter #(= (:ws-ch %) ws-ch))
       first))


(defn user-by-id [user-id]
  (->> @app-state
       :users
       (filter #(= (:user-id %) user-id))
       first))



(defn ws-handler
  [req]
  (with-channel req ws-ch
    (let [client-tap (async/chan)]
      (async/tap main-mult client-tap)
      (async/go-loop []
        (async/alt!
          client-tap ([message]
                        (if message
                          (do
                            (async/>! ws-ch message)  ;; Send to all clients
                            (recur))
                          (async/close! ws-ch)))
          ws-ch ([{:keys [message]}]
                 (if message
                   (do
                     (clojure.pprint/pprint message)
                     (case (:m-type message)
                       :init-user->server (do
                                            (async/>! ws-ch {:m-type :server->init-user
                                                             :channels (->> @app-state
                                                                            :channels)
                                                             :users (->> @app-state
                                                                         :users
                                                                         (mapv #(dissoc % :ws-ch)))})
                                            (swap! app-state update :users conj (assoc message :ws-ch ws-ch))
                                            (async/>! main-chan (assoc message :m-type :new-user)))
                       :direct-message (do
                                         (println message)
                                         (if-let [user (user-by-id (get-in message [:to-user :user-id]))]
                                             (async/>! (:ws-ch user) message)
                                             (println "User not found!")))
                       :channel-message (async/>! main-chan message))
                     (recur))
                   (do
                     (let [user (->> @app-state :users (filter #(= (:ws-ch %) ws-ch)) first)]
                       (async/untap main-mult client-tap)
                       (async/>! main-chan (-> user (dissoc :ws-ch) (assoc :m-type :user-left)))
                       (swap! app-state update :users (fn [x] (remove #(= % user) x))))))))))))





(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/ws" [] ws-handler)
  (GET "/log" [] tala.log/log-handler)
  (resources "/"))


(def dev-handler (-> #'routes wrap-reload))


(def handler routes)


(defn -main [& [port]]
  (hk/run-server handler {:port (or (Integer. port) 8080)}))
