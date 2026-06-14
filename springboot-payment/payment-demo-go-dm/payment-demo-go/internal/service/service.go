package service

import (
	"payment-demo-go/internal/config"
	"payment-demo-go/internal/lock"
	"payment-demo-go/internal/mq"
	"payment-demo-go/internal/pay"

	"github.com/redis/go-redis/v9"
	"gorm.io/gorm"
)

type Service struct {
	DB     *gorm.DB
	Redis  *redis.Client
	Lock   *lock.RedisLock
	MQ     *mq.Client
	Cfg    *config.Config
	WxV3   *pay.WxV3Client
	WxV2   *pay.WxV2Client
	AliPay *pay.AliPayClient
}

func New(db *gorm.DB, redisClient *redis.Client, lockTpl *lock.RedisLock, mqClient *mq.Client, cfg *config.Config, wxV3 *pay.WxV3Client, wxV2 *pay.WxV2Client, ali *pay.AliPayClient) *Service {
	return &Service{DB: db, Redis: redisClient, Lock: lockTpl, MQ: mqClient, Cfg: cfg, WxV3: wxV3, WxV2: wxV2, AliPay: ali}
}
