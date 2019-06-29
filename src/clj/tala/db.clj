(ns tala.db
  (:require [datomic.api :as d]))

(def uri "datomic:free://localhost:4334/tala")
(d/create-database uri)
(def conn (d/connect uri))
