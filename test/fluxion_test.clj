(ns fluxion-test
  (:require [clojure.core.async :refer [chan <!! onto-chan]]
            [fluxion :refer :all]
            [clojure.test :refer :all]
            [clojure.data.generators :as gen]
            [clojure.test.generative :refer [defspec]]
            [clojure.test.generative.runner :as runner]))

(defspec values-from-channel-appear-in-atom
  (fn [vs] (let [ch (chan 1)
                 a (atom nil)
                 terminal (onto-chan ch vs)]
             (<!! (atom-sink a ch))
             @a))
  [^{:tag (gen/vec gen/long)} vs]
  (assert (= % (last vs))))

(defspec sink-calls-function-with-every-value
  (fn [vs] (let [ch (chan 1)
                 a (atom [])
                 terminal (onto-chan ch vs)]
             (<!! (sink #(swap! a conj %) ch))
             [@a vs]))
  [^{:tag (gen/vec gen/string)} vs]
  (assert (apply = %)))

(defspec timer-is-roughly-periodic
  (fn [interval beats]
    (let [t (timer interval)
          ticks (loop [ticks []
                       beats-to-go beats]
                  (if (< 0 beats-to-go)
                    (recur (conj ticks (<!! t))
                           (dec beats-to-go))
                    ticks))
          target-schedule (take beats (iterate (partial + interval) (first ticks)))]
      (map #(/ (Math/abs (- %1 %2)) interval) ticks target-schedule)))
  [^{:tag (gen/uniform 20 100)} interval ^{:tag (gen/uniform 2 50)} beats]
  (assert (< (/ (apply + %) (count %)) 1/10)))

(comment (runner/run 2 10000 #'values-from-channel-appear-in-atom #'sink-calls-function-with-every-value))

;; Creating multiple timers should not create multiple threads.
;;
;; Ideally we can drive the sleep loop in one thread iterating at the
;; lowest common denominator of all extant clock cycles, and then send
;; to channels modolu their multiple of that lcd tick cycle.
;;
;; Doing so means having a new potential clock client which requires a
;; faster cycle period than the current clock. We need to decouple the
;; signaling channels from the iterating process so that the iterating
;; process can be replaced.
;;
;; This means clock system needs to both manage the modulo on a per
;; client basis, and swapping clock beats at the inputs of all
;; clients' channels.
;;
;; Ideally we can reclaim clock channels from consumers who stop using
;; them. Holding weak references to the clock channels should enable
;; this (but means modulo channels need to be tolerant to the weak ref
;; having been GCed and then shutting down cleanly.