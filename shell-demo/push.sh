#/bin/bash
GITADD(){
	for item in `ls`;do 
		if [ -d $item ]
		then
			cd $item
			GITADD $item 
			cd ..
		else
			git add $item
		fi
	done
}
GITADD $1 #$1 the folder to be added
read -p "Please enter commit information > " message
touch temp #create temporary file to store cmd
echo "#!/bin/bash">>temp
echo "git commit -m '${message}'">>temp
bash temp
rm -rf temp
read -p "Please enter the branch name>" branch
git push -u origin $branch
