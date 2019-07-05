(ns tala.components
  (:require [re-frame.core :refer [dispatch] :as re-frame]
            [tala.events :as events]
            [tala.subs :as subs]))


(defn input-form [k btn-text placeholder on-submit]
  (let [value (re-frame/subscribe [::subs/forms k])]
    (fn [k btn-text placeholder on-submit]
      [:form {:align "center"
              :on-submit (fn [x]
                           (.preventDefault x)
                           (on-submit @value)
                           (dispatch [::events/forms k nil]))}
       [:input {:style {:width "100%"}
                :type "text"
                :auto-focus true
                :value @value
                :placeholder placeholder
                :on-change #(let [v (-> % .-target .-value)]
                              (dispatch [::events/forms k v]))}]
       [:button {:type "submit"
                 :class "button-primary"} btn-text]])))
