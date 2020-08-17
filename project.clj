(defproject semira "1.0.0-SNAPSHOT"
  :description "Semira sings songs."
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.30"]
                 [compojure "1.6.1"]
                 [ring/ring-jetty-adapter "1.8.1"]
                 [hiccup "1.0.5"]
                 [ring-partial-content "2.0.1"]
                 [net.jthink/jaudiotagger "2.2.5"]
                 [org.clojure/core.async "1.3.610"]
                 [cljs-http "0.1.46"]
                 [reagent "0.10.0"]]
  :repositories {"jaudiotagger-repository" "https://dl.bintray.com/ijabz/maven"}

  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-figwheel "0.5.20"]
            [deraen/lein-sass4clj "0.3.1"]
            [lein-exec "0.3.7"]]

  :figwheel {:css-dirs ["generated/public"]}

  :profiles {:dev     {:dependencies [[figwheel-sidecar "0.5.20"]
                                      [cider/piggieback "0.5.0"]
                                      [ring/ring-mock "0.4.0"]]
                       :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}
             :uberjar {:aot        :all
                       :prep-tasks ["compile-sass"
                                    "compile"
                                    ["cljsbuild" "once" "prod"]]}}

  :main semira.core
  :uberjar-name "semira.jar"
  :resource-paths ["resources" "generated"]

  :sass {:target-path  "generated/public"
         :source-paths ["sass"]
         :source-map   true}

  :aliases { "compile-sass" ["exec" "-e" ;; https://github.com/Deraen/sass4clj/issues/18#issuecomment-412299327
                             "(println (:out (clojure.java.shell/sh \"lein\" \"sass4clj\" \"once\")))"]}

  :cljsbuild {:builds [{:id           "prod"
                        :source-paths ["src"]
                        :compiler     {:optimizations :advanced
                                       :output-to     "generated/public/semira.js"
                                       :output-dir    "generated/public/semira"}}
                       {:id           "dev"
                        :source-paths ["src"]
                        :figwheel     true
                        :compiler     {:output-to  "generated/public/semira-dev.js"
                                       :output-dir "generated/public/semira-dev"
                                       :asset-path "semira-dev"
                                       :main       "semira.frontend"}}]})
