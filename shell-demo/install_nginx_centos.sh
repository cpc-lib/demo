#!/bin/bash
yum -y install wget gcc pcre-devel zlib-devel
wget http://nginx.org/download/nginx-1.16.0.tar.gz 
tar -zxvf nginx-1.16.0.tar.gz 
cd nginx-1.16.0
./configure --prefix=/usr/local/nginx 
make -j 2 # 数字代表cpu核心个数
make install