apt-get update
apt-get install build-essential
apt-get install libtool
apt-get install openssl
apt-get install libpcre3 libpcre3-dev 
wget http://nginx.org/download/nginx-1.16.0.tar.gz 
tar -zxvf nginx-1.16.0.tar.gz 
cd nginx-1.16.0
./configure --prefix=/usr/local/nginx 
make -j 2 
make install

