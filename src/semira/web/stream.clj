;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web.stream
  (:require [compojure.core :as compojure]
            [semira
             [models :as models]
             [stream :as stream]]))

(compojure/defroutes handler
  (compojure/GET "/stream/:model/:id.:ext" [model id ext :as request]
                 (let [object ((get {"track" models/track-by-id
                                     "album" models/album-by-id}
                                    model models/track-by-id) id)
                       type ({"mp3" "audio/mpeg"
                              "ogg" "audio/ogg"} ext)
                       in (stream/get object type)
                       len (stream/length object type)]
                   {:status 200
                    :headers (merge {"Content-Type" type}
                                    (if len {"Content-Length" (str len)}))
                    :body in})))
