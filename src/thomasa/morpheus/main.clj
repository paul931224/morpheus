(ns thomasa.morpheus.main
  (:require [thomasa.morpheus.core :as m]
            [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn usage [options-summary]
  (str/join
   "\n"
   ["Usage: morpheus -d DIR -f FORMAT [-n NODE] [-u] [--help] paths"
    options-summary]))

(defn- var-usages->file! [analysis node dir format]
  (-> (m/var-usages-graph analysis node)
      (m/graph->file! "fdp" dir (str node "-usgs") format)))

(defn- var-deps->file!
  [graph node nodes format dir]
  (-> (m/node->subgraph graph node)
      (m/add-ref-to-subgraphs nodes format)
      (m/graph->file! dir node format)))

(defn -main [& args]
  (let [{:keys [arguments errors summary] {:keys [dir format help usages node]} :options}
        (cli/parse-opts
         args
         [["-d" "--dir DIR" "Directory to save output files to"
           :parse-fn #(.getCanonicalFile (io/file %))
           :validate [#(.exists %)]]
          ["-f" "--format FORMAT" "dot, png, svg"
           :default "dot"
           :validate [#{"dot" "png" "svg"}]]
          ["-n" "--node NODE" "Node to generate subgraph view for"]
          ["-u" "--usages" "Generate graph to show usages of var defined by NODE (-n --node)."]
          ["-h" "--help"]])]
    (cond
      errors
      (println
       "Errors occured while parsing command:\n"
       (str/join "\n" errors))

      help
      (println (usage summary))

      usages
      (if node
        (var-usages->file! (m/lint-analysis arguments) node dir format)
        (println "You have to supply a node with -n --node option to generate usages graph for it."))

      :default
      (let [analysis (m/lint-analysis arguments)
            graph    (m/var-deps-graph analysis)
            nodes    (map
                      (fn [{:keys [ns name]}] (str ns "/" name))
                      (:var-definitions analysis))]
        (if node
          (var-deps->file! graph node nodes format dir)
          (doseq [node nodes]
            (var-deps->file! graph node nodes format dir)))))))
