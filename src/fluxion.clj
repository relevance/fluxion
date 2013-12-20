(ns fluxion
  (:require [clojure.core.async :refer [close! go-loop >!! <! >! chan sliding-buffer]]))

(defn timer
  "Create a periodic timer that emits a message on a channel at
   periodic times in the future. Specifically, it will output
   at t_n = t_0 + n*interval, where n is a monotonically increasing
   integer.

   The message contains nothing meaningful.

   In case of system pauses or clock resets (e.g., due to garbage
   collection, virtualization, or leap seconds) some t_n's may
   be skipped.

   If the system clock is observed to move backwards (e.g., leaving
   daylight savings time) the sequence of times will be recomputed
   with the new time as t_0. This may cause some timestamps to
   be emitted twice, but periodicity should remain the same.

   Note that a backwards clock jump less than the interval
   cannot be detected.

   The short form creates a channel with a sliding-buffer of 1."
  ([interval]
     (timer interval (sliding-buffer 1)))
  ([interval n-or-buf]
     (let [ch (chan n-or-buf)
           time-seq (fn [t0] (iterate #(+ interval %) t0))]
       (thread
        (loop [last-cl  (System/currentTimeMillis)
               plan     (time-seq last-cl)]
          (let [now-cl        (System/currentTimeMillis)
                [next & rest] (drop-while #(< % now-cl) plan)]
            (if (< now-cl last-cl)
              (recur (time-seq now-cl) now-cl))
            (if (< now-cl next)
              (Thread/sleep (- next now-cl)))
            (>!! ch now-cl)
            (recur now-cl rest))))
       ch)))

(defn sample-on
  "Call the sampling function f whenever an input arrives on in.
   The value of the input is discarded.
   Returns a new channel that will emit the return values from f."
  ([f in] (sample-on f in (sliding-buffer 1)))
  ([f in n-or-buf]
     (let [output (chan n-or-buf)]
       (go-loop [v (<! in)]
                (if v
                  (do 
                    (>! output (f))
                    (recur (<! in)))
                  (close! output)))
       output)))

;; gating function that opens/closes channel valve by another channel providing hi/lo signals
;; reify a whole fluxion processor and return one discrete thing (easy button)
;; higher-level function to build a sampler for any arbitrary JMX metric
;; aggregate inputs using reductions moral equivalent against a memo, an arbitrary fn, and the collections arriving on an input channel
;; buffer that accumulates inputs until a certain time has elapsed from the first input or the input channel becomes unready
;; 
