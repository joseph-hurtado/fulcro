(ns untangled.components.config-spec
  (:require [com.stuartsierra.component :as component]
            [untangled.components.config :as cfg])
  (:use midje.sweet))

(facts :focused "untangled.config"
       (facts :focused "load-config"
              (fact :focused "recursively merges props into defaults"
                    (cfg/load-config {}) => {:a {:b {:c :f
                                                     :u :y}
                                                 :e 13}}

                    (provided
                      (#'cfg/get-defaults nil) => {:a {:b {:c :d}
                                                       :e {:z :v}}}
                      (#'cfg/get-props nil) => {:a {:b {:c :f
                                                        :u :y}
                                                    :e 13}}
                      ))
              (fact :focused "can take a prop path argument"
                    (cfg/load-config {:props-path "/foo/bar"})
                    => {:foo :qux}

                    (provided
                      (#'cfg/get-defaults nil) => {:foo :qux}
                      (#'cfg/get-props "/foo/bar") => {}
                      )
                    )
              (fact :focused "can take a defaults path argument"
                    (cfg/load-config {:defaults-path "/foo/bar"})
                    => {:foo :bar}

                    (provided
                      (#'cfg/get-defaults "/foo/bar") => {:foo :qux}
                      (#'cfg/get-props nil) => {:foo :bar}
                      )
                    )
              )
       (facts :focused "load-edn"
              (fact :focused "returns nil if absolute file is not found"
                    (#'cfg/load-edn "/garbage") => nil
                    )
              (fact :focused "returns nil if relative file is not on classpath"
                    (#'cfg/load-edn "garbage") => nil
                    )
              (fact :focused "can load edn from the classpath"
                    (#'cfg/load-edn "resources/defaults.edn") => {:some-key :some-default-val}
                    )
              (fact :integration :focused "can load edn from the disk"
                    (let [tmp-file (java.io.File/createTempFile "data-file" ".edn")
                          _ (spit tmp-file "{:a 1}")
                          full-path (.getAbsolutePath tmp-file)]
                      (#'cfg/load-edn full-path)) => {:a 1}
                    )
              )
       (facts :focused "get-props"
              (fact :focused "takes in a path, finds the file at that path and should return a clojure map"
                    (#'cfg/get-props "/foobar") => ..props..
                    (provided
                      (slurp "/foobar") => (str ..props..))
                    )
              (fact :focused "or if path is nil, uses a default path"
                    (#'cfg/get-props nil) => ..props..
                    (provided
                      (slurp "/usr/local/etc/config.edn") => (str ..props..))
                    )
              )
       (facts :focused "get-defaults"
              (fact :focused "takes in a path, finds the file at that path and should return a clojure map"
                    (#'cfg/get-defaults "/foobar") => ..defaults..
                    (provided
                      (slurp "/foobar") => (str ..defaults..))
                    )
              (fact :focused "or if path is nil, uses a default path"
                    (#'cfg/get-defaults nil) => ..defaults..
                    (provided
                      (slurp #"resources/defaults\.edn$") => (str ..defaults..))
                    )
              )
       )

(defrecord App [config]
  component/Lifecycle
  (start [this]
    (assoc this :config config))
  (stop [this]
    this))

(defn new-app []
  (component/using
    (map->App {})
    [:config]))

(facts :focused "untangled.components.config"
       (facts :focused "new-config"
              (fact :focused "returns a stuartsierra component"
                    (satisfies? component/Lifecycle (cfg/new-config)) => true

                    (fact :focused ".start loads the config"
                          (.start (cfg/new-config)) => (contains {:value ..cfg..})
                          (provided
                            (cfg/load-config anything) => ..cfg..)
                          )
                    (fact :focused ".stop removes the config"
                          (-> (cfg/new-config) .start .stop :config) => nil
                          (provided
                            (cfg/load-config anything) => anything)
                          )
                    )
              )
       (facts :focused "new-config can be injected through a system-map"
              (-> (component/system-map
                    :config (cfg/new-config)
                    :app (new-app))
                  .start :app :config :value) => {:foo :bar}
              (provided
                (cfg/load-config anything) => {:foo :bar})
              )
       )
