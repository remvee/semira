;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.web
  (:require
   [semira.web.albums :as web-albums]
   [semira.web.stream :as web-stream])
  (:use
   [compojure.core :only [defroutes]]
   [ring.middleware.params :only [wrap-params]]
   [ring.middleware.file :only [wrap-file]]
   [ring.middleware.file-info :only [wrap-file-info]]
   [ring.middleware.partial-content :only [wrap-partial-content]]))

(defroutes handler
  web-albums/handler
  web-stream/handler)

(def app (-> handler
             wrap-params
             (wrap-file "public")
             wrap-file-info
             wrap-partial-content))
