(ns fluxion
  (:require [clojure.core.async :refer [close! thread go-loop >!! <! >! chan sliding-buffer]]))

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
              (recur (time-seq now-cl) now-cl)
              (do
                (if (< now-cl next)
                  (Thread/sleep (- next now-cl)))
                (>!! ch now-cl)
                (recur now-cl rest))))))
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

(defn buffered
  "Creates aggregations by reading values from in, returning a channel
   the aggregations will be written on. If in transitions from ready
   to unready, or if t signals that an aggregation should be made
   available, produces a vector containing all the values from in, in
   the order which they were read.

   t may be a number, or a channel

   If t is a number, it will create a timer channel that signals every
   t milliseconds to yield the aggregation

   If t is a channel, every time a value is produced on the channel an
   aggregation will be produced. The value from the channel will be
   discarded."
  ([t in] (buffered t in (sliding-buffer 1000)))
  ([t in n-or-buf]
     (let [t (if (number? t) (timer t) t)
           out (chan n-or-buf)]
       (go-loop [[data port] (alts! [t in]
                                    :priority true
                                    :default nil)
                 pending []]
                (if (not= port in)
                  (>! out pending)
                  (recur (alts! [t in]
                               :priority true
                               :default nil)
                         (conj pending data))))
       out)))


(defn aggregate
  "Creates reductions against each value from in, returning a channel
   the reductions will be written on. f is initially invoked with 0
   arguments to yield an initial memo, and then as values are read
   from in, f is called with the memo and the value read. The return
   value is written on the returned channel, and treated as the new
   memo."
  ([f in] (aggregate f in (sliding-buffer 1000)))
  ([f in n-or-buf]
     (let [out (chan n-or-buf)
           initial-val (f)]
       (go-loop [vs (<! in)
                 current initial-val]
                (let [next (f current vs)]
                  (>! out next)
                  (recur (<! in) next)))
       out)))

(defn sink
  "Calls f on each value read from in."
  [f in]
  (go-loop [v (<! in)]
           (when v
             (f v)
             (recur (<! in)))))

(defn atom-sink
  "Puts each value received from in into the atom a."
  [a in]
  (sink #(reset! a %) in))
