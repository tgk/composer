(ns composer.composer
  (:refer-clojure :exclude [==])
  (:require [clojure.core.async :refer [go >! <!]]
            [clojure.core.logic :refer :all]
            [clojure.core.logic.pldb :refer :all]))

(defn scale-from-tones [tone-types]
  (take 25
        (->> tone-types
             (map {:semitone [1]
                   :tone [0 1]
                   :minor-third [0 0 1]})
             flatten
             butlast
             (cons 1)
             cycle)))

(def major-scale
  (scale-from-tones
   [:tone :tone :semitone :tone :tone :tone :semitone]))
(def harmonic-minor-scale
  (scale-from-tones
   [:tone :semitone :tone :tone :semitone :minor-third :semitone]))
(def natural-minor-scale
  (scale-from-tones
   [:tone :semitone :tone :tone :semitone :tone :tone]))
(def locrian-mode
  (scale-from-tones
   [:semitone :tone :tone :semitone :tone :tone :tone]))
(def mixolydian-mode
  (scale-from-tones
   [:tone :tone :semitone :tone :tone :semitone :tone]))

(def scale-modes
  [[:major-scale          major-scale]
   [:harmonic-minor-scale harmonic-minor-scale]
   [:natural-minor-scale  natural-minor-scale]
   [:locrian-mode         locrian-mode]
   [:mixolydian-mode      mixolydian-mode]])

(db-rel semitone note-1 note-2)

(def keys-from-c
  [:C3 :C#3 :D3 :D#3 :E3 :F3 :F#3 :G3 :G#3 :A3 :A#3 :B3
   :C4 :C#4 :D4 :D#4 :E4 :F4 :F#4 :G4 :G#4 :A4 :A#4 :B4
   :C5])

(def semitone-facts
  (reduce
   (fn [db [note-1 note-2]]
     (db-fact db semitone note-1 note-2))
   empty-db
   (partition 2 1 keys-from-c)))

(defne scaleo [base-note scale notes]
  ([note [1 . scale-rest] [note . ()]])
  ([note [1 . scale-rest] [note . notes-rest]]
     (fresh [next-note]
            (semitone note next-note)
            (scaleo next-note scale-rest notes-rest)))
  ([note [0 . scale-rest] notes]
     (fresh [next-note]
            (semitone note next-note)
            (scaleo next-note scale-rest notes))))

(defn key-restriction
  [instrument-state s1]
  (if-let [key (:key instrument-state)]
    (all (== key s1))
    succeed))

(defn scale-restriction
  [instrument-state scale-type]
  (if (:scale instrument-state)
    (all (membero [(:scale instrument-state) scale-type] scale-modes))
    succeed))

(defn cadence-restriction
  [instrument-state m7 s2 s4 s5]
  (case (:cadence instrument-state)
    :perfect   (all (== m7 s5))
    :plagal    (all (== m7 s4))
    :just-nice (all (== m7 s2))
    nil        succeed))

(defn- random-composition
  [{gaps :gaps
    :as instrument-state}]
  {:gaps (for [i (range 8)] (get gaps i 0.5))
   :melody
   (rand-nth
    (or
     (seq
      (with-db
        semitone-facts
        (run 64 [melody2]
             (fresh [melody
                     m1 m2 m3 m4 m5 m6 m7 m8
                     scale
                     s1 s2 s3 s4 s5 s6 s7 s8
                     base-note scale-type]
                    (key-restriction instrument-state s1)
                    (== melody [m1 m2 m3 m4 m5 m6 m7 m8])
                    (== scale [s1 s2 s3 s4 s5 s6 s7 s8])
                    (== m1 s1)
                    (== m8 s8)
                    (cadence-restriction instrument-state m7 s2 s4 s5)
                    (== melody2 [m1 m2 m3 m4 m5 m6 m7 m1])
                    (scale-restriction instrument-state scale-type)
                    (scaleo base-note scale-type scale)
                    (permuteo scale melody)))))
     [[]]))})

;; Loop

(defn composer-loop
  "Listens for new instrument states on instrument-state-ch and emits a
  random melody to melody-ch. The loop terminates when
  instrument-state-ch closes."
  [instrument-state-ch melody-ch]
  (go
   (loop []
     (when-let [instrument-state (<! instrument-state-ch)]
       (>! melody-ch (random-composition instrument-state))
       (recur)))))
