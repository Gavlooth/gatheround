(ns utils
  (:require-macros [macros :as m])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    #_[goog.style :as gstyle]
    #_[goog.dom :as gdom]
    [goog.object :as gobj]
    [fipp.edn :as fipp]
    #_["fast-jwt" :as fjwt]
    [cljs.core.async.interop :refer-macros [<p!]]
    [cljs.core.async :as async :refer [go]])
  (:import
    [goog.date DateTime]
    [goog.async Debouncer]))


(goog-define WITH-CORS false)

(declare clog)
(declare dlog)
(declare oget)
(declare oget-in)
(declare oset!)
(declare oset-in!)

(defn clj->json [s]
  (.stringify js/JSON (clj->js  s)))


(defn form-data [x]
  (if (map? x)
    (let [form (js/FormData.)]
      (doseq [[k v] (seq x)]
        (.append form (name k) v))
      form)
    (let [form (js/FormData. x)]
      form)))


(defn format-request [body & {:keys [mode token  content-type request response method]
                              :or {method "post"
                                   mode "cors"
                                   response "text"
                                   content-type "application/json"}}]

 (clog content-type {:color "tomato"})
 (let [ content-type (cond
                       (not request)  content-type
                       (= "json" (name request))  "application/json"
                       (= "form" (name request))  "application/x-www-form-urlencoded"
                       (= "application/x-www-form-urlencoded" (name request))  "application/x-www-form-urlencoded"
                       (= "text" (name request))  "application/json"
                       (= "text/plain" (name request))  "application/json"

                       :else  content-type)
       body (cond
              (= "application/json" content-type) (.stringify js/JSON (clj->json body))
              (= "application/x-www-form-urlencoded"  content-type) (js/URLSearchParams. (form-data body)) ;; URLSearchParams not necessary
              :else body)]

  (m/spy (clj->js (m/spy  {:headers
                           (merge (when token  {:auth-token  token})
                                  {:content-type content-type})
                           :mode mode
                           :body body
                           :method method
                           :response (name response)})))))

(defn js-fetch [url obj]
  (async/go
      (let [response-type (oget obj "response")
            response (<p! (js/fetch url  obj))
            body    (case  response-type
                           "text"  (<p!  (.text response))
                           "json"  (<p! (.json response))
                           "blob"  (<p! (.blob response))
                           "form-data" (.formData response)
                           "array-buffer" (.arrayBuffer response)
                           "cljs" (js->clj (<p! (.json response))
                                           :keywordize-keys true)
                           "clj"  (js->clj (<p! (.json response))
                                           :keywordize-keys true)
                           (<p! (.-body response)))]

        (try
         (if
           (.-ok response)
           {:headers  (walk/keywordize-keys (into {} (js->clj (es6-iterator-seq (.entries (.-headers response))))))
            :status (.-status response)
            :status-text  (.-statusText response)
            :body body}
           (throw (str "Server responded with " (js->clj (.-headers  response)))))
         (catch js/Error err  (clog (str "ERROR: " err) {:pretty-print true :color :tomato}))))))


(defn make-walk-handler
 ([{:keys [key-handler value-handler]}]
  (cond (and key-handler value-handler)
        (fn [x]
         (MapEntry. (key-handler (key x))
                    (value-handler (val x)) nil))
   value-handler  (fn [x]
                   (MapEntry. ((key x))
                              (value-handler (val x)) nil))
   key-handler    (fn [x]
                    (MapEntry. (key-handler (key x))
                               (val x)  nil))
   :else          (fn [x]
                    (MapEntry. (key x)
                               (val x)  nil)))))


(defn transform-collection [m* & {:keys [key-handler value-handler collection-handler] :or {collection-handler identity}}]
  (let [map-handler (make-walk-handler {:key-handler key-handler :value-handler value-handler})]
    (walk/postwalk (fn [x]
                     (cond (map-entry? x) (map-handler x)
                           (map? x)       (clj->js x)
                           (coll? x)      (clj->js (into [] collection-handler  x))
                           :else         x)) m*)))


(defn transform-map [m* & {:keys [key-handler value-handler collection-handler] :or {collection-handler identity}}]
  (let [map-handler (make-walk-handler {:key-handler key-handler :value-handler value-handler})]
    (walk/postwalk (fn [x]
                     (if (map-entry? x)
                         (map-handler x)
                         x))
                   m*)))

(defn fetch [url data & {:as options}]
  (let [datum (transform-map   (merge (when WITH-CORS {:mode "cors"})
                                      {:method "post"
                                       :Access-Control-Allow-Origin "*"
                                       :response "text"}
                                      data
                                      options)
                             :key-handler #(str/lower-case (name %)))]
    (js-fetch url (clj->js datum))))


;; (def public-key (m/file->string "jwt_RS256_key.pub"))

(def lower-case (fnil str/lower-case ""))


(def js-log (js* "console.log"))

(def name* (fnil name ""))

(def is-NaN? #(.isNaN js/Number %))

(defn query-selector [query & {:keys [all]}]
  (if all  #_(true? all)
    (seq (.querySelectorAll js/document query))
    (.querySelector js/document query)))


(defn form->clj [element]
    (js->clj (.fromEntries js/Object (.entries (js/FormData.  (query-selector element))))))

(defn any-NaN? [col]
  (boolean (some is-NaN? col)))


(defn set-timeout [f s]
  (js/setTimeout f s))


(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))


(defn oget-in [object the-keys]
  (let [valid-keys (mapv name* the-keys)]
    (gobj/getValueByKeys object (apply array valid-keys))))


(defn oget
  ([object the-key](gobj/get object (name* the-key)))
  ([object the-key not-found] (or (gobj/get object (name* the-key) not-found))))


(defn ovalues
  ([object] (gobj/getValues object))
  ([object key-seq] (map (partial oget object) key-seq)))


(defn oset! [object the-key the-val]
  (gobj/set object (name* the-key ) the-val)
  object)


(defn oset-in! [object the-keys the-value]
  (let [length-1 (dec (count the-keys))
        butlast-keys  (subvec the-keys 0 length-1)
        last-key (name* (last the-keys))]
    (try
      (do (gobj/set (oget-in object butlast-keys) last-key the-value)
          object)
      (catch js/Object e (.log js/console"Error: " e  {:color "red"})))))


(def radians->degrees #(* 57.29578 %))


(def degrees->radians #(/ % 57.29578))


(def cos #(.cos js/Math (degrees->radians %)))


(def sin #(.sin js/Math (degrees->radians %)))


(def atan2 #(.atan2 js/Math (degrees->radians %1) (degrees->radians %2)))


(defn dlog
  "Like clog bellow but google compiler will remove whith advance o
   ptimizations. Excellent for development"
  [ & more]
  (when goog.DEBUG
    (apply clog more)))
;; :font-weight "500" :font-size "0.6em")))

(defn clog
  "Use it when you want to always emmit a message to
   console. Pass a map with options :color :font :background-color
   after whatever the arguments to be printed are. The options can
   also be symbols or srings aside of keywords. Alternatively pass
   {:pretty-print true} to use clojure's pretty printer.  "
  [& args]
  (if (some-> args last map?)
    (let [butlast-args   (butlast  args)
          {:keys [color background-color font-weight font-size json pretty-print]} (last args)
          with-options? (or color background-color font-weight font-size json pretty-print)]
      (if with-options?
        (let [css-options (str "color:" (name* color )\;
                               "background-color:" (name* background-color)\;
                               "font-weight:" (name* font-weight)\;
                               "font-size:" (name* font-size))]
          (js-log (apply str \%\c
                         (cond pretty-print (mapv
                                              #(with-out-str
                                                 (fipp/pprint %))
                                              butlast-args)
                               :default butlast-args))
                  css-options))
        (apply js-log args)))
   (apply js-log args)))


(defn location! [href]
  (oset-in! js/window ['location 'href] href)

 (defn location []
  (oget-in js/window ['location 'href])))


;;Date functions

;;How many milliseconds in one day
(def MILIDAY 86400000)

(def months ["January" "February" "March" "April" "May" "June" "July" "August"
             "September" "October" "November" "December"])



(defn snake->kebab [x]
 (str/replace  (name* x) #"_" "-"))


(defn get-month [obj]
  (get months (dec (.getMonth ^js obj))))


(defn epoch [x]
 (.getTime x))


(defn current-epoch []
   (epoch (DateTime.)))


(defn trim-history [] (.pushState js/history (oget js/history  'state) "" "/#"))

;; #_(oget-in js/document ['location 'protocol])
;; #_(oget-in js/document ['location 'host])
;; #_ document.location.href

(defn set-hash! [s & {:keys [trim] :or {trim true}}]
 (when trim (trim-history))
 (oset!  js/document.location "hash" s))

(defn attach-event [t λ]
  (.addEventListener js/window (name t) λ))

(defn dispatch-event [event]
  (.dispatchEvent js/window (js/Event. (name event))))


#_(defn make-jwt-checker [public-key & handlers]
    (let [decode (fjwt/createVerifier #js {:key public-key :algorithm "RS256"})]
      (fn [token]
        (try
          (when token
            (decode token))
          (catch js/Object exp (clog "error: "
                                     (oget exp 'code) "-- "
                                     (oget exp 'message)
                                     {:color :tomato})
                               (doseq [handler handlers]
                                 (when handler (handler))))))))

(defn initialize-hash  [ & _]
  (let [url-hash (oget-in js/document ['location 'hash])
        pathname  (oget-in js/document ['location 'pathname])]
    (cond (not (clojure.string/blank? url-hash)) (.pushState js/history #js {} "" (str "/" url-hash))
          (= "/" pathname )  (do
                              (.pushState js/history #js {} "" "/#/index")
                              (dispatch-event :hashchange))

          :else (do (.pushState js/history #js {} "" (str "/#"  pathname))
                    (dispatch-event :hashchange)))))

