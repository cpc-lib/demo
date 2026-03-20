#!/bin/bash 
UNZIP(){
	for item in `ls`;do 
		if [ -d $item ]
		then
			cd $item
			UNZIP $item 
			cd ..
		elif [[ $item == *.zip ]]
		then 
			unzip -o $item 
			sleep 5
			rm -rf $item
			UNZIP $PWD
		else	
		fi
	done
}
UNZIP $1 #$1 is the sourcefile
