(ns composer.instrument-state
  (:require [clojure.core.async :refer [go >! <!]]))

(defn- update-instrument-state
  "Updates the instrument state based on the update. The recognised
  updates are

  [:scale scale-keyword]."
  [state update]
  (println "update" update)
  (case (first update)
    :scale (let [[_ scale] update]
             (if (= scale (:scale state))
               (dissoc state :scale)
               (assoc state :scale scale)))
    :gap   (let [[_ gap size] update]
             (assoc-in state [:gaps gap] size))
    state))

(defn instrument-state-loop
  "Listens for updates on update-ch and emits the latest state on
  emit-state-ch. The loop terminates if update-ch is closed. Recognised
  updates at the moment are

  [:scale scale-keyword].

  I might add a schema to the updates that come in."
  [update-ch emit-state-ch]
  (go
   (loop [state {}]
     (when-let [update (<! update-ch)]
       (let [updated-state (update-instrument-state state update)]
         (>! emit-state-ch updated-state)
         (recur updated-state))))))