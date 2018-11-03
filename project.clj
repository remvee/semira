(defproject semira "1.0.0-SNAPSHOT"
  :description "Semira sings songs."
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [compojure "1.6.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [hiccup "1.0.5"]
                 [ring-partial-content "1.0.0"]
                 [org/jaudiotagger "2.0.3"]
                 [org.clojure/core.async "0.4.474"]
                 [cljs-http "0.1.45"]
                 [reagent "0.8.1"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.17"]
            [deraen/lein-sass4clj "0.3.1"]
            [lein-exec "0.3.7"]]

  :figwheel {:css-dirs ["generated/public"]}

  :profiles {:dev     {:dependencies [[figwheel-sidecar "0.5.17"]
                                      [cider/piggieback "0.3.10"]
                                      [ring/ring-mock "0.3.2"]]
                       :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}
             :uberjar {:aot        :all
                       :prep-tasks ["compile-sass" "compile" ["cljsbuild" "once" "prod"]]}}

  :main semira.core
  :uberjar-name "semira.jar"
  :resource-paths ["resources" "generated"]

  :sass {:target-path  "generated/public"
         :source-paths ["sass"]
         :source-map   true}

  :aliases { "compile-sass" ["exec" "-e" ;; https://github.com/Deraen/sass4clj/issues/18#issuecomment-412299327
                             "(println (:out (clojure.java.shell/sh \"lein\" \"sass4clj\" \"once\")))"]}

  :cljsbuild {:builds {:prod {:source-paths ["src"]
                              :compiler     {:optimizations :advanced
                                             :output-to     "generated/public/semira.js"
                                             :output-dir    "generated/public/semira"}}
                       :dev  {:source-paths ["src"]
                              :figwheel     true
                              :compiler     {:output-to  "generated/public/semira-dev.js"
                                             :output-dir "generated/public/semira-dev"
                                             :asset-path "semira-dev"
                                             :main       "semira.frontend"}}}})
