# edn-env
A small library to overlay environment variables onto a config

## Why?
This library is influenced greatly by my use of [`cprop`](https://github.com/tolitius/cprop) (which is a wonderful library, you should check it out). Praises aside, `cprop` [makes some assumptions](https://github.com/tolitius/cprop/blob/master/src/cprop/source.cljc#L32) about values that complicates usage and has some clunky handling of resources.

`edn-env` aims to fix these issues by:
- assuming everything in the environment is EDN, pure and simple
- forcing the user to implement any complicated resource loading

These two points make `edn-env` small and flexible.

## Usage
To start:

```clojure
(require '[edn-env.core :as env])
```

### overlay
The main fn provided is the `overlay` function which takes in a map and applies any environment variables that are contained in the given map:
```clojure
;; Given this env:
;;   DATABASE__HOST=localhost
;;   DATABASE__PASSWORD=insecure
(env/overlay {:database {:host nil
                         :username "user"
                         :password nil}})
{:database
 {:host "localhost", :username "user", :password "insecure"}}
```
`overlay` can also take an options map which change its behavior:
- `kebab-re`: a string/char that indicates a `-` between two words in a single key
- `nest-re`: a string/char that indicates a new level of nesting as been added
- `path-fn`: is called for every part
```clojure
;; Given this (arbitrary and super confusing yet explanatory) env:
;;   CLUSTERSv0vHOST.NAME=localhost
(env/overlay {:clusters [{:host-name nil}]
             {:kebab-re #"\."
              :nest-re #"v"
              :path-fn #(try
                          (Integer/parseInt %)
                          (catch Exception _
                            (keyword %)))}
{:clusters [{:host-name "localhost"}]}
```

### load-config
Loads the given resource (defaulting to `config.edn`) and calls `overlay` on the loaded config:

```clojure
;; config.edn
{:test-value nil}

;; Given this env:
;;   TEST_VALUE=420
(edn/load-config)
{:test-value 420}
```
`load-config` can also take the same options map `overlay` does:
```clojure
;; clusters.edn
{:clusters [{:host-name nil}]}

;; It's me again!:
;;   CLUSTERSv0vHOST.NAME=localhost
(env/load-config "cluster.edn" {:kebab-re #"\."
                                :nest-re #"v"
                                :path-fn #(try
                                            (Integer/parseInt %)
                                            (catch Exception _
                                              (keyword %)))}
{:clusters [{:host-name "localhost"}]}
```
# Contributing
Issues and PRs are welcome.
