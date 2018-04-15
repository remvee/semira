;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web.stream
  (:require [compojure.core :as compojure]
            [semira.mime-type :refer [type-by-ext]]
            [semira.models :as models]
            [semira.stream :as stream]))

(def retry-after-seconds 5)

(compojure/defroutes bare-handler
  (compojure/GET "/stream/:id.:ext" {:keys            [albums open length]
                                     {:keys [id ext]} :params}
                 (when-let [track (models/track-by-id albums id)]
                   (when-let [type (type-by-ext ext)]
                     (let [in  (open track type)
                           len (length track type)]
                       (if (= ::stream/busy in)
                         {:status  503 ;; Service Unavailable
                          :headers {"Retry-After" (str retry-after-seconds)}
                          :body    "busy"}
                         {:status  200
                          :headers (into {"Content-Type" type}
                                         (when len {"Content-Length" (str len)}))
                          :body    in}))))))

(def handler
  (fn [req]
    (bare-handler (assoc req :albums (models/albums), :open stream/open, :length stream/length))))
