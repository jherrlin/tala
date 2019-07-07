(ns tala.log
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :as async]
            [taoensso.timbre :as timbre
             :refer [log  trace  debug  info  warn  error  fatal  report
                     logf tracef debugf infof warnf errorf fatalf reportf
                     spy get-env]]
            [taoensso.timbre.appenders.core :as appenders]
            ))




(defonce log-filename "/tmp/tala.log")
(defonce log-chan (async/chan 8))


(timbre/merge-config!
 {:timestamp-opts (assoc timbre/default-timestamp-opts
                         :timezone (java.util.TimeZone/getTimeZone "Europe/Stockholm"))
  :appenders {:println {:enabled? false}  ;; dont print logs to repl
              :spit (appenders/spit-appender {:fname log-filename
                                              :hostname_ "server"
                                              :instant java.util.Date})}})


;; log handler thread
(async/thread
  (loop []
    (when-let [log (async/<!! log-chan)]
      (spit log-filename
            (str
             (.format (java.text.SimpleDateFormat. "yy-MM-dd HH:mm:ss") (java.util.Date.))
             " client "
             log
             "\n") :append true)))
  (recur))



(defn log-handler
  [req]
  (with-channel req ws-ch
    (async/go-loop []
      (let [data (async/<! ws-ch)]
        (if data
          (let [{:keys [message]} data]
            (async/put! log-chan message)
            (recur))
          (do
            (async/close! ws-ch)))))))


(comment
  (async/put! log-chan "Hejsan")
  (info "hejsan")
  )
