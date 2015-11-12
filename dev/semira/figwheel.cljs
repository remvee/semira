(ns semira.figwheel
  (:require [figwheel.client :as figwheel :include-macros true]))

(defn ^:export run [run & args]
  (figwheel/watch-and-reload
   :websocket-url "ws://localhost:3449/figwheel-ws"
   :jsload-callback #(apply run args))
  (apply run args))
