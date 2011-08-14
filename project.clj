(defproject semira "1.0.0-SNAPSHOT"
  :description "Semira sings songs."
  :dependencies [[org.clojure/clojure "1.3.0-beta1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 
                 [compojure "0.6.5"]
                 [hiccup "0.3.6"]
                 [ring/ring-jetty-adapter "0.3.11"]
                 [ring/ring-devel "0.3.11"]
                 [ring-partial-content "0.0.1"]
                 
                 [org/jaudiotagger "2.0.3"]
                 
                 [lein-run "1.0.0"]

                 [hiccups "0.1.1"]]
  :run-aliases {:server ["script/server.clj"]})
