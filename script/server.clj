(use 'ring.adapter.jetty
     'semira.web)

(run-jetty app {:port (Integer/parseInt (get (System/getenv) "PORT" "8080"))})
