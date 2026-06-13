package main

import (
	"context"
	"fmt"
	"log"
	"time"

	"payment-demo-go/internal/config"
	"payment-demo-go/internal/db"
	"payment-demo-go/internal/handler"
	"payment-demo-go/internal/lock"
	"payment-demo-go/internal/mq"
	"payment-demo-go/internal/pay"
	"payment-demo-go/internal/service"

	"github.com/gin-gonic/gin"
	"github.com/redis/go-redis/v9"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("加载配置失败: %v", err)
	}

	database, err := db.Open(cfg.MySQL)
	if err != nil {
		log.Fatalf("连接 MySQL 失败: %v", err)
	}

	rdb := redis.NewClient(&redis.Options{
		Addr:         cfg.Redis.Addr,
		Password:     cfg.Redis.Password,
		DB:           cfg.Redis.DB,
		DialTimeout:  cfg.Redis.Timeout,
		ReadTimeout:  cfg.Redis.Timeout,
		WriteTimeout: cfg.Redis.Timeout,
	})
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := rdb.Ping(ctx).Err(); err != nil {
		log.Fatalf("连接 Redis 失败: %v", err)
	}

	mqClient, err := mq.New(cfg.RabbitMQ, cfg.Payment.OrderCloseDelay)
	if err != nil {
		log.Fatalf("连接 RabbitMQ 失败: %v", err)
	}

	wxV3, err := pay.NewWxV3Client(cfg.WxPay)
	if err != nil {
		log.Fatalf("初始化微信支付 V3 客户端失败: %v", err)
	}
	wxV2 := pay.NewWxV2Client(cfg.WxPay)

	ali, err := pay.NewAliPayClient(cfg.AliPay)
	if err != nil {
		log.Fatalf("初始化支付宝客户端失败: %v", err)
	}

	svc := service.New(database, rdb, lock.NewRedisLock(rdb), mqClient, cfg, wxV3, wxV2, ali)
	if err := mqClient.ConsumeCloseOrder(context.Background(), svc.HandleOrderCloseMessage); err != nil {
		log.Fatalf("启动订单延迟关闭消费者失败: %v", err)
	}

	r := gin.Default()
	handler.New(svc).Register(r)
	addr := fmt.Sprintf(":%d", cfg.ServerPort)
	log.Printf("payment-demo-go 启动成功，监听 %s", addr)
	if err := r.Run(addr); err != nil {
		log.Fatalf("HTTP 服务启动失败: %v", err)
	}
}
