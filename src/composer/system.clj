(ns composer.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.flow :as flow]
            [clojure.core.async :refer [go >! <! chan put! close!
                                        sliding-buffer]]

            [composer.osc              :as osc]
            [composer.instrument-state :as instrument-state]
            [composer.composer         :as composer]
            [composer.overtone         :as overtone]))

(defonce ^:dynamic *system* nil)

(defn- print-loop
  [prefix ch]
  (go
   (loop []
     (when-let [msg (<! ch)]
       (println prefix msg)
       (recur)))))

(def system
  (flow/flow

   :osc-listener ([osc-port osc-alias osc-out-ch]
                    (osc/start osc-port osc-alias osc-out-ch))

   :instrument-state-loop ([osc-out-ch instrument-state-ch]
                             (instrument-state/instrument-state-loop
                              osc-out-ch instrument-state-ch))

   :composer-loop ([instrument-state-ch melody-ch]
                     (composer/composer-loop
                      instrument-state-ch
                      melody-ch))

   :overtone-loop ([melody-ch]
                     (overtone/overtone-loop melody-ch))))

(defn start
  [& [options]]
  (flow/run system
            (merge
             {:osc-port            44100
              :osc-alias           "composer"
              :osc-out-ch          (chan 64)
              :instrument-state-ch (chan (sliding-buffer 1))
              :melody-ch           (chan (sliding-buffer 1))}
             options)))

(defn- safe-close!
  [ch]
  (when ch (close! ch)))

(defn stop
  [system]
  (when-let [osc-listener (:osc-listener system)]
    (osc-listener))
  (safe-close! (:osc-out-ch system))
  (safe-close! (:instrument-state-ch system))
  (safe-close! (:melody-ch system)))

(defn start-system
  "Creates a current system from config and starts the components."
  [& [options]]
  (log/info "Starting system")
  (alter-var-root #'*system* (constantly (start options)))
  (log/info "Up and running"))

(defn stop-system
  "Stops all compoenents of the current system."
  []
  (log/info "Stopping system")
  (alter-var-root #'*system* stop)
  (log/info "Done"))
