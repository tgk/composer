(ns composer.overtone
  "The overtone namespace is excluded from the system reloading as
  overtone doesn't work on nrepl boot."
  (:require [clojure.tools.namespace.repl :refer [disable-reload!]]
            [clojure.core.async :refer [go >! <!]]
            [overtone.core :as overtone :refer :all]
            [overtone.osc.util :refer :all]))

(disable-reload!)

(defn overtone-loop
  "Dummy overtone loop - we can't load the server on nrepl boot, so this
  is the dummy standin until the live environment is up and
  running. After that, you'll have to execute the forms in this
  namespace."
  [melody-ch]
  :loop-does-nothing)

(comment

  (boot-internal-server)

  (definst triangle-wave
    [freq 440 attack 0.01 sustain 0.1 release 0.4 vol 0.4]
    (* (env-gen (lin attack sustain release) 1 1 0 1 FREE)
       (lf-tri freq)
       vol))

  (defn play-note
    [music-note]
    (-> music-note note midi->hz triangle-wave))

  (defn chord-progression-atom
    "Reads and plays the current melody in melody-atom. The melody-atom
    should contain a map with a :melody key, under which a sequence of
    notes should be stored."
    [metro beat-num melody-atom]
    (doseq [[beat note] (zipmap (range) (:melody @melody-atom))]
      (at (metro (+ beat beat-num)) (play-note note)))
    (apply-at
     (metro (+ (count (:melody @melody-atom)) beat-num))
     chord-progression-atom
     metro
     (+ (count (:melody @melody-atom)) beat-num) melody-atom []))

  (defn overtone-loop
    "Starts an overtone server and listens for melodies on
    melody-ch. Never closes down the server - it takes too long - but
    does run stop to silence melodies when the loop terminates."
    [melody-ch]
    (let [melody-atom (atom [])
          metro (metronome 100)]
      (chord-progression-atom metro (metro) melody-atom)
      (go
       (loop []
         (let [melody (<! melody-ch)]
           (if melody
             (do
               (reset! melody-atom melody)
               (recur))
             (overtone/stop)))))))
  )
