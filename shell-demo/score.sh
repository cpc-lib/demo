#!/bin/bash 
read -p "请输入你的考试成绩：" score
case $((score/10)) in
	[6-9]|10)
		echo "succeeded to pass the examination"
		;;
	[0-5])
		echo "failed to pass the examination"
		;;
esac

