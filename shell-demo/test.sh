#/bin/bash
item='target'
if [ ${item} == ${item:-1,6} ] && [ ${#item} -eq 6 ] 
	then
		echo true
else 
	echo false
fi