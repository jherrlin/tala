(ns tala.log
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :as async :include-macros true]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]))




(goog-define ws-log-url "ws://localhost:3449/log")
(defonce log-chan (async/chan 8))


(defn send-ws-log! [log]
  (async/put! log-chan log))


(defn setup-log-ws-send-loop [server-chan]
  (async/go-loop []
    (when-let [log-message (async/<! log-chan)]
      (async/>! server-chan log-message)
      (recur))))


(defn setup-log-ws! []
  (async/go
    (let [{:keys [ws-channel error]} (async/<! (ws-ch ws-log-url))]
      (if error
        (println "Something went wrong with the websocket!")
        (setup-log-ws-send-loop ws-channel)))))


(timbre/merge-config! {:appenders {:send-ws-log {:enabled? true,
                                                 :async? false,
                                                 :min-level nil,
                                                 :rate-limit nil,
                                                 :output-fn :inherit,
                                                 :fn (fn self [data]
                                                       (let [{:keys [output_]} data]
                                                         (send-ws-log! (force output_))))}}})
