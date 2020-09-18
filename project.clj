(defproject douglass/edn-env "0.1.1"
  :description "A small library to overlay environment variables onto a config"
  :license "EPL 1.0"
  :url "https://github.com/DarinDouglass/edn-env"
  :deploy-repositories [["clojars" {:sign-releases false}]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.1"]]}
             :test {:resource-paths ["test/resources"]}})
