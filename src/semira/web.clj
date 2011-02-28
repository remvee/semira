(ns semira.web
  (:use [compojure.core :only [defroutes GET POST ANY]]
        [hiccup.core :only [html]]))

(defroutes app
  (GET "/" []
       {:status  200
        :headers {"Content-Type" "text/html"}
        :body    (html [:html
                        [:head [:title "SEMIRA"]]
                        [:body [:h1 "SEMIRA"]]])}))

;; (do (require 'ring.adapter.jetty) (ring.adapter.jetty/run-jetty (var app) {:port 8080))