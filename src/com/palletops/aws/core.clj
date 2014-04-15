(ns com.palletops.aws.core
  "Common services"
  (:require
   [clojure.core.async :refer [>! chan close! go]]))

(defmacro go-try
  "Provides a go macro which executes its body inside a try/catch block.
  If an exception is thrown by the body, a rex-tuple is written to the
  channel ch.  Returns the channel for the go-block.

  NB. the channel, ch, should be buffered if the caller is going to
  block on the returned channel before reading ch."
  [ch & body]
  `(go
    (try
      ~@body
      (catch Throwable e#
        (>! ~ch {:exception e#})))))


(defn service-map
  "Return a service map for a service that can be started by passing the service
  map to the go-fn.  Input to the service is via the channel, ch."
  [go-fn {:keys [api ch credentials] :or {ch (chan)} :as options}]
  (assoc options
    :chan ch
    :go-fn go-fn))

(defn start
  "Start a keypair service."
  [{:keys [api ch credentials go-ch go-fn] :as service}]
  (when-not api
    (throw
     (ex-info "Trying to start a service with no api."
              {:service service})))
  (when-not credentials
    (throw
     (ex-info "Trying to start a service with no credentials."
              {:service service})))
  (when-not ch
    (throw
     (ex-info "Trying to start a service with no input channel."
              {:service service})))
  (when go-ch
    (throw
     (ex-info "Trying to start a service that is already running."
              {:service service})))

  (let [go-ch (go-fn service)]
    (assoc service :go-ch go-ch)))


(defn started?
  "Predicate to test for a started service."
  [{:keys [go-ch] :as service}]
  go-ch)

(defn stop
  "Stop a service."
  [{:keys [ch api go-ch] :as service}]
  (when-not go-ch
    (throw
     (ex-info "Trying to stop an unstarted service."
              {:service service})))
  (close! ch)
  (dissoc service :ch :go-ch))

(defn service-result-chan
  "Return the service result channel.  The service writes value to
  this channel when it exits."
  [{:keys [go-ch] :as service}]
  go-ch)
