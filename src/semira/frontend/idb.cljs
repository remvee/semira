(ns semira.frontend.idb
  (:refer-clojure :exclude [get count])
  (:require [cljs.core.async :as async]))

(def idb (or (.-indexedDB js/window)
             (.-webkitIndexedDB js/window)
             (.-mozIndexedDB js/window)
             (.-OIndexedDB js/window)
             (.-msIndexedDB js/window)))

(defn available? []
  (and idb (.-open idb)))

(defn open [name stores & [version]]
  (let [out (async/chan)
        req (.open idb name (or version 1))]
    (set! (.-onsuccess req)
          #(async/put! out (.-result req)))
    (set! (.-onupgradeneeded req)
          #(let [db (.-result req)]
             (doseq [store stores]
              (let [name (:name store)
                    opts (dissoc store :name)]
                (.createObjectStore db name (clj->js opts))))))
    out))

(def readonly "readonly")
(def readwrite "readwrite")

(defn- tx [db store-name & [mode]]
  (.transaction db (clj->js [store-name]) mode))

(defn get [db store-name key & [not-found]]
  (let [out (async/chan)
        req (-> db
                (tx store-name readonly)
                (.objectStore store-name)
                (.get key))]
    (set! (.-onsuccess req)
          #(if-let [val (.-result req)]
             (async/put! out val)
             (async/close! out)))
    out))

(defn count [db store-name]
  (let [out (async/chan)
        req (-> db
                (tx store-name readonly)
                (.objectStore store-name)
                (.count))]
    (set! (.-onsuccess req)
          #(async/put! out (.-result req)))
    out))

(defn put [db store-name key val]
  (-> db
      (tx store-name readwrite)
      (.objectStore store-name)
      (.put val key)))

(defn delete [db store-name key]
  (-> db
      (tx store-name readwrite)
      (.objectStore store-name)
      (.delete key)))
