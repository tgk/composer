(ns composer.instrument-state
  (:require [clojure.core.async :refer [go >! <!]]))

(defn- update-instrument-state
  "Updates the instrument state based on the update. The recognised
  updates are

  [:scale scale-keyword]."
  [state update]
  (apply assoc state update))

(defn instrument-state-loop
  "Listens for updates on update-ch and emits the latest state on
  emit-state-ch. The loop terminates if update-ch is closed. Recognised
  updates at the moment are

  [:scale scale-keyword].

  I might add a schema to the updates that come in."
  [update-ch emit-state-ch]
  (go
   (loop [state {:scale :major}]
     (when-let [update (<! update-ch)]
       (let [updated-state (update-instrument-state state update)]
         (>! emit-state-ch updated-state)
         (recur updated-state))))))
