#!/bin/bash 
passwordcodebook="password.txt"
while read LINE
do
	echo "Trying [$LINE]"
	unzip -o -q -P $LINE $1
	if [ $? -ne 0 ];then
		echo "Failed"
	else
		echo "Succeeded"
		echo "The password is $LINE"
		rm -r $1
		break;
	fi
done<$passwordcodebook
if [ $? -ne 0 ];then
	echo "Sorry,the correct password was not found"
fi
