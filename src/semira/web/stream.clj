(ns semira.web.stream
  (:require
   [semira.models :as models]
   [semira.stream :as stream]
   [compojure.core :as compojure]))

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