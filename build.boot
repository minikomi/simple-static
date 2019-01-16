(set-env!
 :project 'simple-static
 :version "0.1.0"
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[
                 ;; pin deps
                 [org.clojure/clojure            "1.9.0"   :scope "provided"]
                 [org.clojure/clojurescript      "1.9.946"]
                 ;; nrepl
                 [org.clojure/tools.nrepl "0.2.13"]
                 ;; other
                 [clj-time "0.14.2"]
                 [hiccup "1.0.5"]
                 ;; boot
                 [adzerk/boot-cljs "2.1.4"                  :scope "test"
                  :exclusions [org.clojure/clojurescript]]
                 [co.poyo/boot-create-html "0.1.0"          :scope "test"]
                 [com.cemerick/piggieback "0.2.1"           :scope "test"]
                 [adzerk/boot-cljs-repl "0.3.3"             :scope "test"]
                 [adzerk/boot-reload "0.5.2"                :scope "test"]
                 [samestep/boot-refresh "0.1.0"             :scope "test"]
                 [weasel "0.7.0"                            :scope "test"]
                 [danielsz/boot-autoprefixer "0.1.0"        :scope "test"]
                 [pandeiro/boot-http "0.8.3"                :scope "test"]
                 ;; frontend
                 [com.andrewmcveigh/cljs-time "0.5.1"]
                 [day8.re-frame/http-fx "0.1.5"]
                 [garden "1.3.4"]
                 [re-frame "0.10.5"]
                 [re-frisk "0.5.3" :exclusions [ring/ring-core]]
                 [reagent "0.7.0"]])

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[pandeiro.boot-http :refer [serve]]
 '[boot.util :as util]
 '[clojure.java.io :as io]
 '[tasks.garden]
 '[clojure.string :as s]
 '[danielsz.autoprefixer :refer [autoprefixer]]
 '[co.poyo.boot-create-html :refer [create-html]]
 '[samestep.boot-refresh :refer [refresh]])

(task-options!
 pom {:project (get-env :project) :version (get-env :version)}
 jar)

(deftask set-options []
  (task-options!
   ;;frontend
   cljs {:optimizations (if (get-env :debug) :none :advanced)
         :compiler-options
         {:closure-defines {'goog.DEBUG (get-env :debug false)}
          :parallel-build true}}
   autoprefixer {:files (if (get-env :debug)
                          []
                          ["styles.css"])})
  identity)

(deftask cider "CIDER profile" []
  (require 'boot.repl)
  (swap! @(resolve 'boot.repl/*default-dependencies*)
         concat '[[cider/cider-nrepl "0.16.0-SNAPSHOT"]
                  [refactor-nrepl "2.4.0-SNAPSHOT"]])
  (swap! @(resolve 'boot.repl/*default-middleware*)
         concat '[cider.nrepl/cider-middleware
                  refactor-nrepl.middleware/wrap-refactor])
  (repl :server true))

(deftask build-styles []
  (comp
   (tasks.garden/build-garden :styles-var 'simple-static.styles.core/combined
                              :output-to "public/css/styles.css"
                              :css-prepend ["css/normalize.css"]
                              :auto-prefix #{:cursor :transform}
                              :pretty-print (get-env :debug false))))

(deftask build-frontend []
  (comp
   (build-styles)
   (create-html)
   (cljs)))

(deftask dev []
  (set-env! :debug true)
  (comp
   (set-options)
   (serve :dir "target/public/"
          :port 3001)
   (cider)
   (cljs-repl)
   (watch :verbose true)
   (refresh)
   (reload :asset-path "/public")
   (build-frontend)
   (target)
   ))

(deftask build []
  (comp
   (set-options)
   (build-frontend)
   (sift :include #{#"\.out" #".*\.edn$"} :invert true)
   (sift :include #{#"^public/"})
   (target)))
