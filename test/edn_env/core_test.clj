(ns edn-env.core-test
  (:require [clojure.test :refer [are deftest is testing]]
            [edn-env.core :as sut]))

(deftest test-contains-in?
  (let [config {:database {:host "host" :port 12345}}]
    (is (true? (#'sut/contains-in? config [:database])))
    (is (true? (#'sut/contains-in? config [:database])))
    (is (true? (#'sut/contains-in? config [:database :host])))
    (is (true? (#'sut/contains-in? {:a [{:b 2}]} [:a 0])))
    (is (true? (#'sut/contains-in? {:a [{:b 2}]} [:a 0 :b])))
    (is (false? (#'sut/contains-in? {:a [{:b 2}]} [:a 1])))
    (is (false? (#'sut/contains-in? config [:database :user])))
    (is (false? (#'sut/contains-in? config [:missing])))
    (is (false? (#'sut/contains-in? config [:database :port :some-other-key])))
    (is (false? (#'sut/contains-in? nil [:database :port :some-other-key])))
    (is (false? (#'sut/contains-in?{} [:database :port :some-other-key])))))

(deftest test-var->path
  (are [input expected options] (= expected (sut/var->path input options))
    "ONE" [:one] sut/default-options
    "ONE_TWO" [:one-two] sut/default-options
    "ONE__TWO" [:one :two] sut/default-options
    "ONE__TWO_THREE_FOUR" [:one :two-three-four] sut/default-options
    "ONE_TWO" [:one :two] (assoc sut/default-options :nest-char "_")
    "ONE_TWO.THREE" [:one :two-three] (assoc sut/default-options
                                             :nest-char \_
                                             :kebab-char \.)
    "ONE__2__THREE" [:one 2 :three] (assoc sut/default-options
                                           :path-fn #(try
                                                       (Long/parseLong %)
                                                       (catch Exception _
                                                         (keyword %))))))

(deftest test-parse-value
  (testing "normal parsing"
    (are [input] (= input (sut/parse-value (pr-str input)))
      nil
      "test"
      1
      -100000000000000000000
      :test
      #{:a :b}
      [1 2 3]
      {:a 1 :b 2}))
  (testing "bad edn"
    (is (= "{:a" (sut/parse-value "{:a")))))

(deftest test-env-vars
  (with-redefs [sut/system-env (constantly {"DATABASE__HOST" "a host" "THREAD_COUNT" "3"})]
    (is (= {[:database :host] "a host"
            [:thread-count] 3}
           (sut/env-vars)))))

(deftest test-overlay
  (with-redefs [sut/system-env (constantly {"DATABASE__HOST" "a host"})]
    (is (= {:database {:host "a host"
                       :user "user"}
            :meaning-of-life nil}
           (sut/overlay {:database {:host nil
                                    :user "user"}
                         :meaning-of-life nil})))))

(deftest test-load-config
  (with-redefs [sut/system-env (constantly {"DATABASE__HOST" "a host"
                                            "OUR.CLUSTERS_0_URL" "http://somewhere"})]
    (testing "using defaults"
      (is (= {:database {:host "a host" :port 1234}} (sut/load-config))))
    (testing "overriding defaults"
      (is (= {:our-clusters [{:url "http://somewhere"}]}
             (sut/load-config "other-config.edn"
                              {:kebab-char "."
                               :nest-char "_"
                               :path-fn #(try
                                           (Long/parseLong %)
                                           (catch Exception _
                                             (keyword %)))}))))
    (testing "missing files are ignored"
      (is (nil? (sut/load-config "notafile.edn" {}))))))
