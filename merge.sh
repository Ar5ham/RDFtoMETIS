#!/bin/bash

for f in *.owl; 
    do
	echo -e "$f"
	rapper -o ntriples $f> ${f%.owl}.nt
    done;
# merge everything 
echo -e "merging all files ...."
cat *.nt > University.MERGED.nt
