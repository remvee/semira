;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend
  (:require [reagent.core :as reagent]
            [semira.frontend.audio :as audio]
            [semira.frontend.components :as components]
            [semira.frontend.shortcuts :as shortcuts]
            [semira.frontend.sync :as sync]
            [semira.frontend.title :as title]))

(enable-console-print!)

(defn debug? []
  (re-find #"debug" (-> js/document .-location .-search)))

(defonce do-setup
  (do
    (audio/setup!)
    (sync/setup!)
    (shortcuts/setup!)
    (title/setup!)))

(reagent/render-component [components/main-component :debug (debug?)]
                          (.getElementById js/document "container"))
