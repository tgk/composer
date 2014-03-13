(ns composer.osc
  (:require [overtone.osc :as osc]
            [clojure.core.async :refer [go >! <! chan put! close!]]))

(def ^:private toggle->scale
  {"/1/multitoggle1/1/1" :major-scale
   "/1/multitoggle1/1/2" :harmonic-minor-scale
   "/1/multitoggle1/1/3" :natural-minor-scale
   "/1/multitoggle1/1/4" :locrian-mode
   "/1/multitoggle1/1/5" :mixolydian-mode})

(defn instrument-state->osc-updates
  "Returns the OSC events for bringing an OSC device in sync with
  instrument-state."
  [instrument-state]
  (for [[path scale] toggle->scale]
    [path (float (if (= scale (:scale instrument-state)) 1.0 0.0))]))

(defn- start-loop
  [scale-touched-ch out-ch]
  (go
   (loop []
     (when-let [scale (<! scale-touched-ch)]
       (>! out-ch [:scale scale])
       (recur)))))

(defn- add-touchosc-client
  [clients host]
  (if (get clients host)
    clients
    (assoc clients host (osc/osc-client host 9000))))

;; TODO: should take channel for emitting events such as
;; [:scale :major]
;; [:key :C#]
;; [:fire]
(defn start
  "Closes when instument-state-ch is closed."
  [port alias out-ch instrument-state-ch]
  (let [server (osc/osc-server port alias)
        scale-touched-ch (chan)
        connected-clients (atom {})]
    (start-loop scale-touched-ch out-ch)
    (doseq [[path scale] toggle->scale]
      (osc/osc-handle
       server path
       (fn [msg]
         (put! scale-touched-ch scale)
         (swap! connected-clients add-touchosc-client (:src-host msg)))))
    (osc/zero-conf-on)
    (go
     (loop []
       (if-let [instrument-state (<! instrument-state-ch)]
         (do
           (doseq [[_ client] @connected-clients]
             (doseq [[path value] (instrument-state->osc-updates instrument-state)]
               (osc/osc-send client path value)))
           (recur))
         (do
           (close! scale-touched-ch)
           (osc/zero-conf-off)
           (osc/osc-close server)))))))
