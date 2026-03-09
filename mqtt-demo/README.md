# SpringBoot MQTT Demo

## Start EMQX

docker-compose up -d

EMQX Dashboard:
http://localhost:18083

username: admin
password: public

## Run Project

mvn spring-boot:run

## Test

http://localhost:8080/mqtt/send?msg=hello