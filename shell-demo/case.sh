#!/bin/bash 
read -p "请输入考试成绩:" score
case $score in
[0-9]|[0-5][0-9])
	echo "E"
;;
[9][0-9]|100)
	echo "A"
;;
[8][0-9])
	echo "B"
;;
[7][0-9])
	echo "C"
;;
[6][0-9])
	echo "D"
;;
esac
