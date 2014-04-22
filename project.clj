(defproject semira "1.0.0-SNAPSHOT"
  :main semira.core
  :description "Semira sings songs."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.3"]
                 [hiccup "1.0.1"]
                 [ring/ring-core "1.1.6"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [ring-partial-content "1.0.0"]
                 [org/jaudiotagger "2.0.3"]]
  :plugins [[lein-cljsbuild "0.3.0"]]
  :cljsbuild {:builds [{:optimizations :advanced
                        :source-paths ["src"]
                        :compiler {:output-to "public/semira.js"
                                   :output-dir "public/semira"}}]})
