(load-file "util/mem_measure.clj")
(cljkv.mem-measure/measure (cljkv.mem-measure/** 2 24) "output.csv")
