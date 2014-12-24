(defproject semira "1.0.0-SNAPSHOT"
  :main semira.core
  :description "Semira sings songs."
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2511" :scope "provided"]
                 [compojure "1.3.1"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring-partial-content "1.0.0"]
                 [org/jaudiotagger "2.0.3"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {:builds [{:optimizations :advanced
                        :source-paths ["src"]
                        :compiler {:output-to "public/semira.js"
                                   :output-dir "public/semira"}}]})
