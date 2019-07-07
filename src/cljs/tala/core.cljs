(ns tala.core
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [tala.config :as config]
   [tala.events :as events]
   [tala.log]
   [tala.views :as views]
   [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
   ))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app-container]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (tala.log/setup-log-ws!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
