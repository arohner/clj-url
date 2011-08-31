(ns clj-url.core
  (:require [clojure.string :as str]))

;; tools for working with urls, because the built in java ones suck

(defn parse
  "Takes a URL string, such as http://www.google.com, and returns a map containing any of the following keys: :protocol, :host, :port, :path, :query. URLs missing one of those parts will not contain a key for that item, i.e. 'http://wwww.google.com' will contain a :protocol and :host key, but not a :path or :query key."
  [u]
  (letfn [(parse-step [state regex part]
            
            (if-let [[_ val rest] (re-find regex (-> state :url))]
              (-> state
                  (update-in [:m] merge {part val})
                  (update-in [:url] (constantly (or rest ""))))
              state))]
    (-> {:m {} :url u}
        (parse-step #"^([^:/]+)://(.*)" :protocol)
        (parse-step #"^([^/:]+)(.*)" :host)
        (parse-step #"^:(\d+)(.*)" :port)
        (parse-step #"^(/[^?]+)(\?.*)?" :path)
        (parse-step #"^\?(.+)" :query)
        :m)))

(defn char-to-hex [^String s]
  (-> s
      (.codePointAt 0)
      (Integer/toString 16)))

(defn valid-label [label]
  (re-find #"^\p{Alnum}[-\p{Alnum}]*\p{Alnum}$" label))

(defn valid-host? [host]
  (every? valid-label (str/split host #"\.")))

(defn valid-ip-addr? [host]
  (let [labels (str/split host #"\.")]
    (and (= 4 (count labels))
         (every? #(re-find #"\d{1,3}" %) labels)
         (every? #(and (>= % 0)
                       (<= % 255)) (map #(Integer/parseInt %) labels)))))

(def validation-fns {:protocol (partial re-find #"^[-+.\p{Alpha}]+$")
                     :host #(or (valid-host? %) (valid-ip-addr? %)) 
                     :port (fn [v]
                             (or (and (integer? v) (pos? v))
                                 (re-find #"^\d*$" v)))
                     :path (partial re-find #"^[-^$_.+%!*'(),\p{Alnum}/=]*$")
                     :query (partial re-find #"^[-^$_.+%!*'(),\p{Alnum}/=]*$")})

(defn invalid-reason [m]
  (assert (map? m))
  (some (fn [[part v]]
          (when v
            (when-not ((-> validation-fns part) v)
              (format "%s '%s' is invalid" (name part) v)))) m))

(defn valid-map? [m]
  (boolean (not (invalid-reason m))))

(defn valid? [url]
  (-> (if (string? url)
        (parse url)
        url)
      (valid-map?)))

(defn escape-char [^String s]
  (str "%" (char-to-hex s)))

(defn escape-str [s]
  (when s
    (-> s
        (str/replace #" " "+")
        (str/replace #"[^\p{Alnum}$_.+!*'(),/=-]" escape-char))))

(defn escape-map [m]
  (-> m
      (update-in [:path] escape-str)
      (update-in [:query] escape-str)))

(defn emit*
  
  [m]
  (assert (map? m))
  (when-not (valid? m)
    (throw (Exception. (invalid-reason m))))
  (str (when (-> m :protocol)
         (str (-> m :protocol) "://"))
       (-> m :host)
       (when (-> m :port)
         (str ":" (-> m :port)))
       (-> m :path)
       (when (-> m :query)
         (str "?" (-> m :query)))))

(defn emit
  "Takes a map containing any of the url keys listed in parse, and returns a url string. Recognized keys: :protocol :host :port :path :query. 

  If passed a URL string, this will parse and escape any characters as necessary.

  (emit {:protocol \"http\" :host \"google.com\"}) => \"http://www.google.com\""
  [u]
  (->
   (if (string? u)
     (parse u)
     u)
   (escape-map)
   (emit*)))