#/bin/bash 
#使用了类c风格的for循环实现九九乘法表
for((outer=1;outer<=9;outer++))
do
	for((inner=1;inner<=outer;inner++))
	do
		echo -ne "${inner}x${outer}=$((inner*outer))\t"
	done
	echo
done
echo "======================================================================"
#使用while循环来实现九九乘法表
outer=1
while [ $outer -le 9 ]
do
	inner=1
	while [ $inner -le $outer ]
	do
		echo -ne "${inner}x${outer}=$((inner*outer))\t"
		let inner++
	done 
	let outer++
	echo 
done
echo "======================================================================"
#使用until循环来实现九九乘法表
outer=1
until [ $outer -gt 9 ]
do
	inner=1
	until [ $inner -gt $outer ]
	do
		echo -ne "${inner}x${outer}=$((inner*outer))\t"
		let inner++
	done
	let outer++
	echo 
done
