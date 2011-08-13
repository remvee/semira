(ns semira.web
  (:require
   [semira.web.albums :as web-albums]
   [semira.web.stream :as web-stream])
  (:use
   [compojure.core :only [defroutes]]
   [ring.middleware.params :only [wrap-params]]
   [ring.middleware.file :only [wrap-file]]
   [ring.middleware.file-info :only [wrap-file-info]]
   [remvee.ring.middleware.partial-content :only [wrap-partial-content]]))

(defroutes handler
  web-albums/handler
  web-stream/handler)

(def app (-> handler
             wrap-params
             (wrap-file "public")
             wrap-file-info
             wrap-partial-content))

;; (do (require 'ring.adapter.jetty) (ring.adapter.jetty/run-jetty (var app) {:port 8080}))
