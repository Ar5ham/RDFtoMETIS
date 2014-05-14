 #!/bin/bash

 # grep -vf: -v: invert match -f: Pattern file 
 # egrep 
 #grep -vf blackList.txt University1_1.SR.nt | egrep '.*\ .*\ (<|_)' |sort | less

function Usage {

	echo "SYNOPSIS: "
	echo -e "\t $1 [OPTIONS] [FILE]"
	echo " "
	echo "DESCRIPTION: "
	echo -e "\t This Script will pre process N-Triple file and will remove lines with strings  provided in pattern file and lines with litteral on the Object side of triple."
	echo " "
	echo "Arguments (All arguments specified below are mandatory):"
	echo -e "\t -i FILE, --input FILE"
	echo -e "\t\t  Obtain N-Triples from input file."
	echo -e "\t -o FILE --output FILE"
	echo -e "\t\t Write the resulting Triples in to the output file"
	echo -e "\t -pf FILE"
	echo -e "\t\t Obtain patterns from FILE, one per line.  The empty file contains zero patterns, and therefore matches nothing."
}





if [ $# == 0 ]
then 
	Usage $0
	exit
fi

IN_FILE="" 
OUT_FILE=""
PAT_FILE=""

while [ $# -gt 0 ]; do
	case "$1" in 
		-h|--help)
		Usage $0
		;;

		-i|--input) 
		shift
		if [ $# -gt 0 ];then
			IN_FILE="$1"
		else
			echo "Error: No input file specified"
			exit 1
		fi
		shift
		;;
		-o|--output)
		shift
		if [ $# -gt 0 ]; then
			OUT_FILE="$1"
		else
			echo "Error: No output file specified"
			exit 1
		fi
		shift
		;;
		-pf|--pattern_file)
		shift
		if [ $# -gt 0 ]; then
			PAT_FILE="$1"
		else
			echo "Error: No pattern file specified"
			exit 1
		fi
		shift 
		;;
	esac
	 
done

echo "IN_FILE: $IN_FILE"
echo "OUT_FILE: $OUT_FILE"
echo "PAT_FILE: $PAT_FILE"

if [ -z "$IN_FILE" ] || [ -z "$OUT_FILE" ] || [ -z  "$PAT_FILE" ]; then
	Usage "$0"
	exit 1;
else
	grep -vf $PAT_FILE $IN_FILE |  egrep '.*\ .*\ (<|_)' | sort --field-separator=' ' -k2 -k1> $OUT_FILE 
 	#echo "------ Got to the code!!!! FINALLY!------- "
fi




















