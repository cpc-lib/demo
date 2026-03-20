#! /bin/sh
function Uncompress()
{
    curFileList=()
    path=$1
    curFileList=`ls $path`
    if [ -f $1 ]; then
        path=${1%/*}
    elif [ -d $1 ]; then
        path=$1
    else
        echo "Invalid path=$path deepLevel:$deepLevel"
        exit
    fi
    deepLevel=`expr $2 + 1`
    for file in ${curFileList[@]}; do
        curFileName=$path/$file
        if [ -d $curFileName ]; then
            ./RecurDeComp.sh $curFileName $deepLevel
        else
            if [[ "$curFileName" != *.tar.gz ]] && \
               [[ "$curFileName" != *.tar ]] && \
               [[ "$curFileName" != *.zip ]]; then
                continue
            fi
            echo "$curFileName deepLevel:$deepLevel"
            #对每种类型的压缩包进行解压
            if [[ "$curFileName" == *.zip ]]; then
                dirName=${curFileName%.zip}
                mkdir $dirName
                unzip -d$dirName $curFileName
                sync
            fi
            if [[ "$curFileName" == *.tar.gz ]]; then
                dirName=${curFileName%.tar.gz}
                mkdir $dirName
                tar zxvf $curFileName -C $dirName
                sync
            fi
            if [[ "$curFileName" == *.tar ]]; then
                dirName=${curFileName%.tar}
                mkdir $dirName
                tar xvf $curFileName -C $dirName 
                sync
            fi
            if[["$curFileName" == *.rar ]];
            	dirName=${curFileName%.rar}
                mkdir $dirName
                unar -f -q -o $dirName $curFileName
                sync
            fi
            #只保留最顶层的压缩包
            if [ $deepLevel -gt 1 ]; then
                rm $curFileName
            fi
            ./UnCompressRecursely.sh $dirName $deepLevel
        fi
    done
}

deepLevel=0
if [ $# -eq 0 ]; then
    workSpace=./
elif [[ "$1" == */ ]] && [[ -d $1 ]]; then
    workSpace=${1%/*}
elif [[ "$1" == */* ]] && [[ -f $1 ]]; then
    workSpace=$1
elif [ -f $1 ]; then
    workSpace=./$1
elif [ -d $1 ]; then
    workSpace=$1
else
    echo "ERROR! $workSpace File Not Found!"
    exit
fi
if [ $# -eq 2 ]; then
    deepLevel=$2
fi
Uncompress $workSpace $deepLevel

