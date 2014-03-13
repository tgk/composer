(ns composer.osc
  (:require [overtone.osc :as osc]
            [clojure.core.async :refer [go >! <! chan put! close!]]))

(def ^:private toggle->scale
  {"/1/multitoggle1/1/1" :major-scale
   "/1/multitoggle1/1/2" :harmonic-minor-scale
   "/1/multitoggle1/1/3" :natural-minor-scale
   "/1/multitoggle1/1/4" :locrian-mode
   "/1/multitoggle1/1/5" :mixolydian-mode})

(defn- start-loop
  [scale-touched-ch out-ch]
  (go
   (loop [current-scale :major]
     (when-let [[scale on?] (<! scale-touched-ch)]
       (if on?
         (do
           (if (not= scale current-scale)
             (>! out-ch [:scale scale]))
           (recur scale))
         (recur current-scale))))))

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
  [port alias out-ch]
  (let [server (osc/osc-server port alias)
        scale-touched-ch (chan)
        connected-clients (atom {})]
    (start-loop scale-touched-ch out-ch)
    (doseq [[path scale] toggle->scale]
      (osc/osc-handle
       server path
       (fn [msg]
         (put! scale-touched-ch
               [scale (-> msg :args first (= 1.0))])

         ;; osc-reply doesn't seem to work with touchosc, so we have to
         ;; manually iterate over them. if more devices were connected,
         ;; we would have to send messages back based on the update
         ;; state from the main loop
         (swap! connected-clients add-touchosc-client (:src-host msg))
         (doseq [[_ client] @connected-clients]
           (doseq [[path pscale] toggle->scale]
             (osc/osc-send client path
                           (float (if (= scale pscale) 1.0 0.0))))))))
    (osc/zero-conf-on)
    (fn stop
      []
      (close! scale-touched-ch)
      (osc/zero-conf-off)
      (osc/osc-close server))))
