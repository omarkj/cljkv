set term png
set title "cljkv memory usage"
set logscale x 2
set format x "2^{%L}"
set xrange [512:33554432]
set xlabel "Keys"
set logscale y 2
set format y "2^{%L}"
set yrange [*:1073741824]
set ylabel "Mem (bytes)"
set datafile separator ","
set output 'res.png'
plot "output.csv" using 1:2