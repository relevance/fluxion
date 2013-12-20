(ns simple
  "Some simple examples of building fluxions."
  (:require [fluxion :as f]))

;; An aggregated counter
(defn counter-flow
  [in]
  (let [a (atom nil)
        out (->> in
                 (buffered 1000)
                 (aggregate +)
                 (-<
                  (atom-sink a)
                  (sink #(log/info %))))]
    (async/tap mult atom-sink)
    (async/tap mult log-sink)
    [out a]))

(defn simple-atom-counter
  "Returns a channel which when written to will reset the atom a to
  the number of values written to that channel."
  [in]
  (let [a (atom nil)
        out (->> in
                 (buffered 1000)
                 (aggregate (fn [m _] (inc m)))
                 (atom-sink a))]
    a))

(defn jmx-counter
  [name]
  (let [a (atom nil)
        out (->> (sample-on (timer 100) #(jmx-get name))
                 (-<
                  (atom-sink a)
                  (log-info)))]
    a))
