;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.core
  (:gen-class)
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [semira.models :as models]
            [semira.web :as web]))

(defonce server-atom (atom nil))

(defn stop! []
  (when-let [server @server-atom]
    (.stop server)
    (reset! server-atom nil)))

(defn start! []
  (stop!)
  (let [host (get (System/getenv) "HOST")
        port (Integer/parseInt (get (System/getenv) "PORT" "8080"))]
    (reset! server-atom
            (run-jetty #'web/app
                       {:host host, :port port, :join? false}))
    (models/scan-if-empty)))

(defn -main [& _]
  (start!))
