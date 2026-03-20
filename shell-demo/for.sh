#!/bin/bash 
for i in `seq 9 -1 1`
do
	echo -ne "$i\b"
	sleep 1
done
files=(`ls`)
for file in ${files[@]}
do
	echo "$file"
done
for((i=1;i<10;i++))
do
	echo $i
done
