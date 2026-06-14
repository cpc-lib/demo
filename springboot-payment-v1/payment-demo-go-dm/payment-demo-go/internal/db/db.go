package db

import (
	"payment-demo-go/internal/config"

	dameng "github.com/godoes/gorm-dameng"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func Dialector(cfg config.DatabaseConfig) gorm.Dialector {
	return dameng.New(dameng.Config{
		DriverName:              cfg.Driver,
		DSN:                     cfg.DSN,
		VarcharSizeIsCharLength: true,
	})
}

func Open(cfg config.DatabaseConfig) (*gorm.DB, error) {
	return gorm.Open(Dialector(cfg), &gorm.Config{Logger: logger.Default.LogMode(logger.Info)})
}
