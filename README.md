Installing
==========
[clj-url "1.0.0"]

Usage
=====

`(clj-url.core/parse "http://www.google.com")` => `{:host "www.google.com", :protocol "http"}`. Parse will return a map containing one or more of the following keys: :protocol, :host, :port, :path, :query. 

`(clj-url.core/emit {:host "www.google.com", :protocol "http"}) => "http://www.google.com". Emit takes a map containing one or more of the keys listed above, and returns a string. Emit will automatically escape invalid characters, replace strings with '+', etc. 