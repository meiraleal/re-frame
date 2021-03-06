(ns re-frame.core
  (:require
    [re-frame.events     :as events]
    [re-frame.subs       :as subs]
    [re-frame.fx         :as fx]
    [re-frame.router     :as router]
    [re-frame.loggers    :as loggers]
    [re-frame.registrar  :as registrar]
    [re-frame.interceptor :as interceptor :refer [base ->interceptor db-handler->interceptor fx-handler->interceptor]]))


;; --  dispatch
(def dispatch         router/dispatch)
(def dispatch-sync    router/dispatch-sync)


;; XXX move API functions up here

;; --  subscribe
(def reg-sub-raw         subs/register)
(def reg-sub             subs/register-pure)
(def subscribe           subs/subscribe)

;; --  effects
(def reg-fx       fx/register)
(def clear-fx     (partial registrar/clear-handlers fx/kind))

;; XXX add a clear all handlers:
;; XXX add a push handlers for testing purposes

;; --  middleware

; (def fx          fx/fx)
(def debug       interceptor/debug)
(def path        interceptor/path)
(def enrich      interceptor/enrich)
(def trim-v      interceptor/trim-v)
(def after       interceptor/after)
(def on-changes  interceptor/on-changes)

;; --  Events

;; usage (clear-event! :some-id)
(def clear-event!  (partial registrar/clear-handlers events/kind))    ;; XXX name with !



;; XXX note name change in changes.md

(defn reg-event-db
  "Register the given `id`, typically a kewyword, with the combination of
  `db-handler` and an interceptor chain.
  `db-handler` is a function: (db event) -> db
  `interceptors` is a collection of interceptors, possibly nested (needs flattenting).
  `db-handler` is wrapped in an interceptor and added to the end of the chain, so in the end
   there is only a chain."
  ([id db-handler]
    (reg-event-db id nil db-handler))

  ([id interceptors db-handler]
   (events/register id [base interceptors (db-handler->interceptor db-handler)])))   ;; XXX add a base-interceptor


(defn reg-event-fx
  ([id fx-handler]
   (reg-event-fx id nil fx-handler))

  ([id interceptors fx-handler]
   (events/register id [base interceptors (fx-handler->interceptor fx-handler)])))   ;; XXX add a base-interceptor


(defn reg-event-context
  ([id handler]
   (reg-event-context id nil handler))

  ([id interceptors handler]
   (events/register id [base interceptors handler])))   ;;   XXX can't just put in handler, must wrap it


;; --  Logging -----
;; Internally, re-frame uses the logging functions: warn, log, error, group and groupEnd
;; By default, these functions map directly to the js/console implementations,
;; but you can override with your own fns (set or subset).
;; Example Usage:
;;   (defn my-fn [& args]  (post-it-somewhere (apply str args)))  ;; here is my alternative
;;   (re-frame.core/set-loggers!  {:warn my-fn :log my-fn})       ;; override the defaults with mine
(def set-loggers! loggers/set-loggers!)

;; If you are writing an extension to re-frame, like perhaps
;; an effeects handler, you may want to use re-frame logging.
;;
;; usage:  (console :error "this is bad: " a-variable " and " anotherv)
;;         (console :warn "possible breach of containment wall at: " dt)
(def console loggers/console)


;; -- Event Procssing Callbacks

(defn add-post-event-callback
  "Registers a function `f` to be called after each event is procecessed
   `f` will be called with two arguments:
    - `event`: a vector. The event just processed.
    - `queue`: a PersistentQueue, possibly empty, of events yet to be processed.

   This is useful in advanced cases like:
     - you are implementing a complex bootstrap pipeline
     - you want to create your own handling infrastructure, with perhaps multiple
       handlers for the one event, etc.  Hook in here.
     - libraries providing 'isomorphic javascript' rendering on  Nodejs or Nashorn.

  'id' is typically a keyword. Supplied at \"add time\" so it can subsequently
  be used at \"remove time\" to get rid of the right callback.
  "
  ([f]
   (add-post-event-callback f f))   ;; use f as its own identifier
  ([id f]
   (router/add-post-event-callback re-frame.router/event-queue id f)))


(defn remove-post-event-callback
  [id]
  (router/remove-post-event-callback re-frame.router/event-queue id))


;; --  Deprecation Messages
;; Assisting the v0.0.7 ->  v0.0.8 tranistion.
(defn register-handler
  [& args]
  (console :warn  "re-frame:  \"register-handler\" has been renamed \"reg-event-db\"")
  (apply reg-event-db args))

(defn register-sub
  [& args]
  (console :error  "re-frame:  \"register-sub\" is deprecated. Use \"reg-sub-raw\".")
  (apply reg-sub-raw args))

