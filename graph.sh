#!/bin/bash

for f in *.n3
do
    echo "Processing $f"
    if [ ! -e "$f.graph" ] ; then
        cat<< EOF >"$f.graph"
http://www.cs.uga.edu#
EOF
    fi
done
