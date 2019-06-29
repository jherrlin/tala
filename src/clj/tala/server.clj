(ns tala.server
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :as async]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [config.core :refer [env]]

            [medley.core :refer [random-uuid]]
            [org.httpkit.server :as hk]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :refer [resource-response]])
  (:gen-class))


(defonce main-chan (async/chan))
(defonce main-mult (async/mult main-chan))
(defonce app-state (atom {:users {}
                          :channels {}}))


(defn ws-handler
  [req]
  (with-channel req ws-ch
    (let [clients-chan (async/chan)]
      (async/tap main-mult clients-chan)
      (async/go-loop []
        (async/alt!
          clients-chan ([message]
                        (if message
                          (do
                            (async/>! ws-ch message)  ;; Send to all clients
                            (recur))
                          (async/close! ws-ch)))
          ws-ch ([{:keys [message]}]
                 (if message
                   (let [m-type (:m-type message)
                         msg (:msg message)]
                     (case m-type
                       :init-user (do
                                    (swap! app-state assoc-in [:users] msg)
                                    (async/>! ws-ch (merge {:m-type :init-user}
                                                           @app-state))
                                    (async/>! main-chan {:m-type :new-user
                                                         :msg msg}))))
                   (do
                     (async/untap main-mult clients-chan)
                     (async/>! main-chan {:m-type :user-left
                                          :msg "client-id"})))))))))



(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/ws" [] ws-handler)
  (resources "/"))


(def dev-handler (-> #'routes wrap-reload))


(def handler routes)


(defn -main [& [port]]
  (hk/run-server handler {:port (or (Integer. port) 8080)}))
