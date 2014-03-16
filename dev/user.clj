(ns user
  (:use [clojure.pprint]
        [clojure.repl])
  (:require [composer.system :refer [*system*] :as system]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            ;; load useful namespaces
            [clojure.set :as set]))

(defn go
  []
  (system/start-system (select-keys *system* [:melody])))

(defn reset
  []
  (system/stop-system)
  (refresh :after 'user/go))

(defn system
  []
  *system*)
