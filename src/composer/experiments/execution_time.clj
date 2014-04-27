(ns composer.experiments.execution-time
  (:require [composer.composer :refer (compositions)]))

(def configuration-A
  {:name    :A
   :key     nil
   :scale   nil
   :cadence nil})

(def configuration-B
  {:name    :B
   :key     nil
   :scale   :major-scale
   :cadence nil})

(def configuration-C
  {:name    :C
   :key     :C3
   :scale   :major-scale
   :cadence nil})

(def configuration-D
  {:name    :D
   :key     :C3
   :scale   :major-scale
   :cadence :perfect})

(def configurations
  [configuration-A
   configuration-B
   configuration-C
   configuration-D])

(defn- avg
  [& elms]
  (float (/ (apply + elms) (count elms))))

;; http://rosettacode.org/wiki/Standard_deviation#Clojure
(defn- std-dev
  [samples]
  (let [n (count samples)
	mean (/ (reduce + samples) n)
	intermediate (map #(Math/pow (- %1 mean) 2) samples)]
    (Math/sqrt
     (/ (reduce + intermediate) n))))

(defn- time-execution
  ([f]
     (let [begin (System/currentTimeMillis)
           result (f)
           end (System/currentTimeMillis)]
       {:time (- end begin)
        :result result}))
  ([f samples]
     (let [results (doall (for [i (range samples)]
                            (time-execution f)))]
       {:time (apply avg (map :time results))
        :min (apply min (map :time results))
        :max (apply max (map :time results))
        :std-dev (std-dev (map :time results))
        :result (:result (first results))})))

(defn- timings
  [configuration limits samples]
  (doall
   (for [limit limits]
     (let [experiment (fn [] (count (compositions configuration limit)))
           result (time-execution experiment samples)]
       (merge configuration
              result
              {:limit limit})))))

(defn- power-range
  [base end]
  (for [i (range end)] (int (Math/pow base i))))

(defn- print-results
  [results]
  (println)
  (doseq [result results]
    (println (name (:name result))
             (:limit result)
             (:time result)
             (:min result)
             (:max result)
             (:std-dev result))))

(defn -main
  [& [end samples]]
  (let [end (Integer/parseInt end)
        samples (Integer/parseInt samples)
        results (doall
                 (apply
                  concat
                  (for [c configurations]
                    (timings c (power-range 2 end) samples))))]
    (println "end =" end)
    (println "samples =" samples)
    (print-results results)))
