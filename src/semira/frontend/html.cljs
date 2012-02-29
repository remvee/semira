(ns semira.frontend.html
  (:require [clojure.string :as string]
            [goog.dom :as gdom]
            [goog.events :as gevents]))

(def gevent-type goog.events.EventType)

(defn- stringify-keys [m]
  (reduce (fn [m [k v]] (assoc m (name k) v)) {} m))

(defn- normalize [tag args]
  (let [[name props] (rest (re-matches #"([^.#]+)([.#].*)?" tag))
        props (if props (re-seq #"[.#][^.#]+" props))
        id (if props (first (map #(subs % 1) (filter #(= "#" (first %)) props))))
        classes (if props (string/join " " (map #(subs % 1) (filter #(= "." (first %)) props))))
        attrs (stringify-keys (merge {:class classes, :id id}
                                     (first (filter map? args))))
        text (first (filter string? args))]
    [name attrs text]))

(defn- build-node [tag args]
  (let [[tag attrs text] (normalize (name tag) args)
        e (gdom/createDom tag (.-strobj attrs))]
    (if text (gdom/setTextContent e text))
    (if (:onclick attrs) (gevents/list e (. gevent-type -CLICK) (:onclick attrs)))
    e))

(defn build [v]
  (cond (vector? v)
        (let [[parent children] [(build-node (first v)
                                             (take-while #(or (map? %) (string? %)) (rest v)))
                                 (drop-while #(or (map? %) (string? %)) (rest v))]
              children (map build children)]
          (doseq [child (flatten children)] (gdom/appendChild parent child))
          parent)

        (seq? v)
        (map build v)

        (string? v)
        (gdom/createTextNode v)
        
        :else
        (throw (str "unexpected input for html/build: " (pr-str v)))))
