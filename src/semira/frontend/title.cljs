(ns semira.frontend.title
  (:require [cljs.core.async :as async]
            [clojure.string :as string]
            [semira.frontend.audio :as audio])
  (:require-macros [cljs.core.async.macros :as async]))

(def app-name "SEMIRA")

(defn titlize [{:keys [artist album title]}]
  (->> [title album artist app-name]
       (map #(if (coll? %) (string/join " / " %) %))
       (filter (complement string/blank?))
       (string/join " - ")))

(defn set-title! [state]
  (let [{:keys              [current-track paused]
         {:keys [playlist]} :queue} state
        text (if current-track
               (cond-> (titlize (into playlist current-track))
                 paused (#(str "(" % ")")))
               app-name)]
    (set! (.-title js/document) text)))

(defn setup! []
  (async/go-loop [state nil]
    (let [new-state (audio/state)]
      (when-not (= state new-state)
        (set-title! new-state))

      (async/<! (async/timeout 500))
      (recur new-state))))
