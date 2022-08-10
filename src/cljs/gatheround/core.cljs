(ns gatheround.core
  (:require-macros [macros :as m])
  (:require
    [thump.core]
    [thump.react :refer [hiccup-element]]
    [helix.hooks :as hooks]
    [helix.core :refer [<>  defnc $ provider]]
    [helix.dom :as d]
    ["react" :as react]
    ["react-dom" :as rdom]
    [goog.dom.dataset :as gdataset]
    [clojure.string :as str]
    [clojure.core.async :as async]
    [cljs.pprint :refer [cl-format]]
    [utils :as u]
    #_[clojure.core.async :as async]))


(def config (m/read-config))

(println config)


#_(defn list-of-vehicles [vehicles !fleet-map search-term]
    (into [:div] (mapv #(vector :div {:style  {:width "12rem" :height "3rem"}} %) state))
    (into [:ul {:class "vehicle-list in-fleet is-hoverable"}]
          (map (partial list-item @!fleet-map search-term) vehicles)))


(def hardcoded {:intro "event 1" :middle "event 2" :end "event 3"})

(defnc List-item []
 #h/e [:div {:style "width:12rem;height:3rem"}  "lorem ipsum"]) 



(defnc Main-component []
  (let [ [{:keys [intro middle end]:as state} set-state] (hooks/use-state  ["event 1" "event 2" "event 3"])
         a-component    (apply $ (into [ :div] (mapv #($ :div  % ) (range 1 5))))
         container (atom "")]
         
    #h/e [:div {:class "central-container"} [:div {:on-click (fn [ event ] (when-not (clojure.string/blank? @container)
                                                                             (set-state conj @container) 
                                                                             (reset! container "")
                                                                             (u/oset! (u/query-selector  "#theinput") :value "")))}
                                                                                 
                                                  "add event"]                    
           
          [:input {:id "theinput" :type "text" :on-change ( fn [_] (print (reset! container (.-value (u/query-selector  "#theinput")))))}]

          [:div {:on-click (fn [ event])} 
                "delete event"
             (apply $ (into [ :div] (vec (map-indexed #($ :div {:data-index  % 
                                                                :on-click (fn [event]  (set-state (fn [s] (vec (concat  (take % s )  (drop  (inc %) s))))))}
                                                               %2) state))))
           #_(mapv #($ [:div  %] ) (range 1 5))]
          #_(apply  $  (m/spy))]))
            
            ;; [:div {:style  {:width "12rem" :height "3rem"}} middle]
            ;; [:div {:style  {:width "12rem" :height "3rem"}} end]]]))
            ;;

#_(defnc Flexiana-component []
    (let [[{:keys [string-1 string-2  is-scrambled]:as state} set-state] (hooks/use-state {:string-1 ""  :string-2 "" :is-scrambled ""})]
      #h/e [:div {:class "central-container"}
            [:div "is scrambled?"]
            [:div {:class "display-container"} [:span is-scrambled]]
            [:form {:on-submit #(.preventDefault %)
                    :class "pure-form search-form"}

              [:label {:for "string-1"} "input first string"]
              [:input {:name "string-1" :type "text", :class "pure-input-rounded"
                       :on-change #(set-state assoc :string-1 (.. % -target -value))}]

              [:label {:for "string-2"} "input second string"]
              [:input {:name "string-2" :type "text", :class "pure-input-rounded"
                       :on-change #(set-state assoc :string-2 (.. % -target -value))}]]
            [:input {:class "button-f pure-button submit-button"
                     :type "button"
                     :value "Send strings"
                     :on-click
                     (let [{{:keys [host port]} :remote-host } config
                           uri (m/spy (cl-format nil "http://~a:~a/gatheround" host port))]
                      (fn [_]
                        (when-not (or (str/blank? string-2) (str/blank? string-1))
                         (async/take! (u/js-fetch uri (u/format-request (dissoc  state :is-scrambled) :content-type "application/json"))
                                      (fn [x]  (set-state assoc :is-scrambled (m/spy (:body x))))))))}]]))




(defn ^:export app [& _] nil
   (print "test")
   (rdom/render ($ Main-component)
                (u/query-selector  "div#gatheround-webapp")))
