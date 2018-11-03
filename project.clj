(defproject semira "1.0.0-SNAPSHOT"
  :description "Semira sings songs."
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [compojure "1.6.0"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [hiccup "1.0.5"]
                 [ring-partial-content "1.0.0"]
                 [org/jaudiotagger "2.0.3"]
                 [org.clojure/core.async "0.3.443"]
                 [cljs-http "0.1.44"]
                 [reagent "0.7.0"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.3-2"]
            [deraen/lein-sass4clj "0.3.1"]
            [lein-exec "0.3.7"]]

  :figwheel {:css-dirs ["generated/public"]}

  :profiles {:dev     {:dependencies [[figwheel-sidecar "0.5.14"]
                                      [com.cemerick/piggieback "0.2.2"]
                                      [ring/ring-mock "0.3.1"]]}
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
                                             :output-dir "generated/public/semira-dev"}}}}

  :repl-options {:init    (do (use 'figwheel-sidecar.repl-api) (start-figwheel!))
                 :init-ns semira.core})
