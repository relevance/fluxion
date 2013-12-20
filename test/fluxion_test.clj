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

#_(defspec timer-is-roughly-periodic
  (fn [interval beats]
    (let [t (timer interval)
          ;; accumulate beats events in a vec
          ;; get the n-1 intervals between the beats
          ;; map to deltas from interval to expected interval
          ]
      ;; return collection of errors
      ))
  [^{:tag (gen/uniform 20 100)} interval ^{:tag (gen/uniform 2 50)} beats]
  (assert ())

)

(comment (runner/run 2 10000 #'values-from-channel-appear-in-atom #'sink-calls-function-with-every-value))
