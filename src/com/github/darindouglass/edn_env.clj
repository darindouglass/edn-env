(ns com.github.darindouglass.edn-env
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def default-config-file "config.edn")
(def default-options {:path-fn keyword
                      :kebab-re #"_"
                      :nest-re #"__"})

(defn- impartial
  "The opposite of `partial`.

  Binds `args` to the last arguments of `func`. Returns a fn whose
  args will be applied first to `func`."
  [func & args]
  (fn [& inner-args]
    (apply func (concat inner-args args))))

(defn- contains-in?
  "Returns true if `path` exists in `config`."
  [config [first & rest]]
  (cond
    ;; If we're given a bad config or used up all the path
    (or (nil? config) (not (seqable? config))) false
    ;; Return true if this is the last part of the path
    (and (empty? rest) (contains? config first)) true
    :else (contains-in? (get config first) rest)))

(defn var->path
  "Converts a env var name into a path suitable for `get-in`."
  ([var]
   (var->path var default-options))
  ([var options]
   (let [{:keys [kebab-re nest-re path-fn]} (merge default-options options)
         parts (str/split (str/lower-case var) nest-re)]
     (map (comp path-fn (impartial str/replace kebab-re "-")) parts))))

(defn parse-value
  "Parses an env value using `edn/read-string`.

  On reading a symbol or failing to parse the string, the original value is returned."
  [value]
  (try
    (let [parsed (edn/read-string value)]
      (if (symbol? parsed)
        value
        parsed))
    (catch Exception _
      value)))

;; Wrap for testing
(defn- system-env []
  (System/getenv))

(defn env-vars
  "Parses env vars into a map of paths -> edn-parsed values."
  ([]
   (env-vars default-options))
  ([options]
   (->> (system-env)
        (map (juxt (comp (impartial var->path options) key) (comp parse-value val)))
        (into {}))))

(defn skip?
  "Returns true iff `::skip` metadata is `true`."
  [all path]
  (->> path
       (get-in all)
       (meta)
       (::skip)
       (true?)))

(defn overlay
  "Overlays env vars onto the provided config."
  ([config]
   (overlay config default-options))
  ([config options]
   (reduce-kv (fn [all path value]
                (cond-> all
                  (and (contains-in? all path) (not (skip? all path)))
                  (assoc-in path value)))
              config
              (env-vars options))))

(defn load-config
  "Loads a resource and overlays the environment ontop."
  ([]
   (load-config default-options))
  ([options]
   (load-config default-config-file options))
  ([file options]
   (when-let [resource (io/resource file)]
     (-> resource
         (slurp)
         (edn/read-string)
         (overlay options)))))
