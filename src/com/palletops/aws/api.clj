(ns com.palletops.aws.api
  "An api for aws"
  (:require
   [clojure.tools.logging :as logging :refer [log trace tracef warnf]]
   [com.palletops.awaze.ec2 :as ec2 :refer [ec2]]
   [com.palletops.awaze.s3 :as s3 :refer [s3]]
   [clojure.core.async :refer [chan close! go put! thread >! >!! <! <!!]]))

(defn debugf
  [fmt & args]
  (log 'aws.api :debug nil (apply format fmt args)))

(defn errorf
  [fmt & args]
  (log 'aws.api :error nil (apply format fmt args)))

(defn exceptionf
  [e fmt & args]
  (log 'aws.api :error e (apply format fmt args)))

;;; ## API processor

;;; We have a processor for the API so that we can respect rate limits, etc.
;;; TODO: ensure rate limits are scoped correctly.

(defmulti process-aws-request
  (fn [{:keys [client]}] client))

(defmethod process-aws-request :ec2
  [{:as request}]
  (try
    (ec2 request)
    (catch Throwable e
      (logging/debugf e "process-aws-request error")
      {:exception e})))

(defmethod process-aws-request :s3
  [{:as request}]
  (try
    (s3 request)
    (catch Throwable e
      (logging/debugf e "process-aws-request error")
      {:exception e})))

(defn dispatch
  "Process a request."
  [{:keys [client] :as request}]
  (debugf "dispatch %s" (pr-str (dissoc request :credentials)))
  (let [result (process-aws-request request)]
    (tracef "dispatch reply %s" (pr-str result))
    (if result
      result
      {:return true})))

(defn dispatch-request
  [{:keys [reply-channel] :as request}]
  (tracef "dispatch-request %s"
          (pr-str (dissoc request :credentials :reply-channel)))
  (try
    (if reply-channel
      (put! reply-channel (dispatch (dissoc request :reply-channel)))
      (errorf "processor request without reply-channel %s" request))
    (catch Throwable e
      (exceptionf
       e "Unexpected processor exception %s" (.getMessage e))
      (put! reply-channel {:exception e})))
  (tracef "dispatch-request completed"))

(defn processor
  "Starts a processor for AWS API calls."
  [ch]
  (go
    (debugf "api processor starting")
    (try
      (loop [request (<! ch)]
        (tracef "processor %s" (pr-str request))
        (when request
          (dispatch-request request)
          (trace "processor looping")
          (recur (<! ch))))
      (catch Throwable e
        (exceptionf
         e "Unexpected api processor exception, terminated")))
    (debugf "api processor finished")))

;;; ## API

;;; The api is returned by this function.  An api instance needs to be passed to
;;; each api function.

(defn start
  "Start an instance of the aws api.  Takes a map with an optional
  `:channel` for the input channel, a `:thread-count` that determines
  the number of threads to run.  Returns a map with the input options
  and `:result-channels` with a sequence of channels, one for the result
  of each thread.  The return map is suitable for passing to `submit`."
  [{:keys [thread-count channel]
    :or {thread-count 1
         channel (chan 2)}
    :as options}]
  {:thread-count thread-count
   :channel channel
   :result-channels
   (doall (repeatedly thread-count #(thread (processor channel))))})

(defn stop
  "Stop an instance of the aws api."
  [{:keys [channel]}]
  (close! channel))

(defn submit
  "Submit a request map to an api instance.  Can be used to submit a map as
  returned by pallet-amazonica.

  Returns a channel on which the result will be written."
  ([{:keys [channel] :as api} request reply-channel]
     {:pre [(map? request)]}
     (put! channel (assoc request :reply-channel reply-channel))
     reply-channel)
  ([api request]
     (submit api request (chan))))

(defn execute
  [{:keys [channel] :as api} request]
  (let [result (<!! (submit api request))]
    ;; (debugf "execute result %s" (pr-str result))
    (when-let [e (and (map? result) (:exception result))]
      (throw e))
    result))
