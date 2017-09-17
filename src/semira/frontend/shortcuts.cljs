;; Copyright (c) Remco van 't Veer. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution.  By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.  You must not remove this notice, or any other, from
;; this software.

(ns semira.frontend.shortcuts
  (:require [clojure.browser.event :as event]
            [goog.events.KeyCodes :as keycodes]
            [semira.frontend.audio :as audio]))

(def keycode-bindings
  {keycodes/PAUSE audio/play-pause
   keycodes/SPACE audio/play-pause
   keycodes/N audio/next
   keycodes/RIGHT audio/next
   keycodes/P audio/prev
   keycodes/LEFT audio/prev})

(defn setup! []
  (event/listen (.-body js/document) "keydown"
                (fn [ev]
                  (when-not (= "INPUT" (-> ev .-target .-tagName))
                    (when-let [f (get keycode-bindings (.-keyCode ev))]
                      (f)
                      (.preventDefault ev))))))
