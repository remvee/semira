(defproject semira "1.0.0-SNAPSHOT"
  :main semira.core
  :description "Semira sings songs."
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [hiccup "1.0.5"]
                 [ring-partial-content "1.0.0"]
                 [org/jaudiotagger "2.0.3"]]

  :plugins [[lein-cljsbuild "1.1.0"]]

  :cljsbuild {:builds {:prod {:source-paths ["src"]
                              :compiler {:optimizations :advanced
                                         :output-to "resources/public/semira.js"
                                         :output-dir "resources/public/semira"}}}})
