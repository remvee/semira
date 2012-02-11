(defproject semira "1.0.0-SNAPSHOT"

  :description "Semira sings songs."

  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/clojure-contrib "1.2.0"]

                 [compojure "0.6.5"]
                 [hiccup "0.3.6"]
                 [ring/ring-jetty-adapter "0.3.11"]
                 [ring/ring-devel "0.3.11"]
                 [ring-partial-content "0.0.1"]

                 [org/jaudiotagger "2.0.3"]]

  :dev-dependencies [[lein-git-deps "0.0.1-SNAPSHOT"]
                     [lein-clojurescript "1.1.0"]]

  :git-dependencies [["https://github.com/clojure/clojurescript.git"
                      "329708bdd0f039241b187bc639836d9997d8fbd4"]]

  ;; fool lein-clojurescript into using this particular version of cljs
  :extra-classpath-dirs [".lein-git-deps/clojurescript/src/clj"
                         ".lein-git-deps/clojurescript/src/cljs"]

  :cljs-output-to "public/js/semira.js"
  :cljs-output-dir "public/js/semira")
