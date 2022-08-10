(ns macros
  (:require
     [clojure.java.io :as io]))


(defmacro file->string [x]
   (slurp (io/file x)))


(defmacro read-config []
  (let [config-file (io/file "config.edn")]
    (when (.exists config-file)
      (try
       (read-string (slurp config-file))
       (catch Exception exp (.getMessage exp))))))

(defmacro spy
  "This macro will print the line, code and result of any form before returning
   the forms' result and it will remove the printing part in production, hence
   it can be used for in-place easy logging/debbuging"
  [x]
  (let[debug-info (meta  &form)]
    `(let [x# ~x]
       (when goog.DEBUG
         (.log js/console (str "%cin file " (:file ~debug-info) ", line:" (:line ~debug-info)) "color:olive;font-size:1.1em;")
         (.log js/console (str "%c"  '~x " --->") "color:purple;font-size:1.2em;")
         (.log js/console (str "%c" (with-out-str (cljs.pprint/pprint x#) )) "color:green;font-size:1.1em;"))
       x#)))
