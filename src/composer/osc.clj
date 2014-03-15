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
  (concat
   (for [[path scale] toggle->scale]
     [path (float (if (= scale (:scale instrument-state)) 1.0 0.0))])
   (for [gap (range 9)]
     [(str "/3/multifader2/" gap) (get-in instrument-state [:gaps gap] 0.5)])))

(defn- instrument-state-loop
  [instrument-state-ch connected-clients shutdown]
  (go
   (loop []
       (if-let [instrument-state (<! instrument-state-ch)]
         (do
           (doseq [[_ client] @connected-clients]
             (doseq [[path value] (instrument-state->osc-updates instrument-state)]
               (osc/osc-send client path value)))
           (recur))
         (shutdown)))))

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
        connected-clients (atom {})
        register-client! (fn [msg] (swap! connected-clients add-touchosc-client (:src-host msg)))]
    (doseq [[path scale] toggle->scale]
      (osc/osc-handle
       server path
       (fn [msg]
         (put! out-ch [:scale scale])
         (register-client! msg))))
    (doseq [gap (range 8)]
      (osc/osc-handle
       server (str "/3/multifader2/" gap)
       (fn [msg]
         (put! out-ch [:gap gap (-> msg :args first)])
         (register-client! msg))))
    (osc/osc-listen server
                    (fn [msg] (println "osc" msg)))
    (osc/zero-conf-on)
    (instrument-state-loop
     instrument-state-ch
     connected-clients
     (fn shutdown
       []
       (osc/zero-conf-off)
       (osc/osc-close server)))))
