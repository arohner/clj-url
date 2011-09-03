(ns clj-url.test-core
  (:require [clj-url.core :as url])
  (:use clojure.test))

(deftest parse-correctly
  (are [url result] (= (url/parse url) result)
       "www.google.com" {:host "www.google.com"}
       "http://www.google.com" {:host "www.google.com" :protocol "http"}
       "http://www.google.com:80" {:host "www.google.com" :protocol "http" :port "80"}
       "http://www.google.com:80/maps" {:host "www.google.com" :protocol "http" :port "80" :path "/maps"}
       "http://www.google.com:80/maps?id=foo" {:host "www.google.com" :protocol "http" :port "80" :path "/maps" :query "id=foo"}
       "/jira/secure/Dashboard.jspa" {:path "/jira/secure/Dashboard.jspa"}
       "/jira/secure/Dashboard.jspa?id=5" {:path "/jira/secure/Dashboard.jspa" :query "id=5"}
       "/foo?id={0}" {:path "/foo" :query "id={0}"} ;; parsing doesn't escape
       "jdbc:postgresql://localhost:5432/shouter" {:path "/shouter", :port "5432", :host "localhost", :protocol "jdbc:postgresql"}))

(deftest valid-hostnames
  (are [u] (= true (url/valid? u))
       "www.google.com"
       "io9.com"
       "3com.com"
       "localhost"
       "192.168.1.1"
       "foo-bar.com"))

(deftest invalid-hostnames
  (are [u] (= false (url/valid? u))
       "foo+bar.com"
       "www.-foo.com"
       "www.foo-.com"
       "1.2.3"
       "1.2.3.4.5"
       "1.25678.3.4"))

(deftest escape-correctly
  (are [url result] (= (url/emit url) result)
       "www.google.com" "www.google.com"
       "www.google.com/path?q=foo" "www.google.com/path?q=foo"
       "/path?q=foo-bar" "/path?q=foo-bar"
       "/jira/secure/Dashboard.jspa" "/jira/secure/Dashboard.jspa"
       "/foo?id={0}" "/foo?id=%7b0%7d"
       "/foo?q=foo bar" "/foo?q=foo+bar"))

