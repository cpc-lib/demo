package model

import (
	"encoding/json"

	"payment-demo-go/internal/types"
)

type BaseEntity struct {
	ID         types.ID        `gorm:"column:id;primaryKey;autoIncrement" json:"id"`
	CreateTime types.LocalTime `gorm:"column:create_time;autoCreateTime" json:"createTime"`
	UpdateTime types.LocalTime `gorm:"column:update_time;autoUpdateTime" json:"updateTime"`
}

type OrderInfo struct {
	BaseEntity
	Title        string `gorm:"column:title" json:"title"`
	OrderNo      string `gorm:"column:order_no" json:"orderNo"`
	UserID       *int64 `gorm:"column:user_id" json:"userId"`
	ProductID    int64  `gorm:"column:product_id" json:"productId"`
	TotalFee     int    `gorm:"column:total_fee" json:"totalFee"`
	CodeURL      string `gorm:"column:code_url" json:"codeUrl"`
	OrderStatus  string `gorm:"column:order_status" json:"orderStatus"`
	PaymentType  string `gorm:"column:payment_type" json:"paymentType"`
	PaymentAppID *int64 `gorm:"column:payment_app_id" json:"paymentAppId"`
	Version      int    `gorm:"column:version" json:"version"`
}

func (OrderInfo) TableName() string { return "t_order_info" }

type PaymentInfo struct {
	BaseEntity
	OrderNo       string `gorm:"column:order_no" json:"orderNo"`
	TransactionID string `gorm:"column:transaction_id" json:"transactionId"`
	PaymentType   string `gorm:"column:payment_type" json:"paymentType"`
	TradeType     string `gorm:"column:trade_type" json:"tradeType"`
	TradeState    string `gorm:"column:trade_state" json:"tradeState"`
	PayerTotal    *int   `gorm:"column:payer_total" json:"payerTotal"`
	Content       string `gorm:"column:content" json:"content"`
}

func (PaymentInfo) TableName() string { return "t_payment_info" }

type Product struct {
	BaseEntity
	Title string `gorm:"column:title" json:"title"`
	Price int    `gorm:"column:price" json:"price"`
}

func (Product) TableName() string { return "t_product" }

type PaymentChannel struct {
	BaseEntity
	ChannelCode  string `gorm:"column:channel_code" json:"channelCode"`
	ChannelName  string `gorm:"column:channel_name" json:"channelName"`
	PaymentType  string `gorm:"column:payment_type" json:"paymentType"`
	Enabled      bool   `gorm:"column:enabled" json:"enabled"`
	ConfigParams string `gorm:"column:config_params" json:"configParams"`
}

func (PaymentChannel) TableName() string { return "t_payment_channel" }

type PaymentChannelRequest struct {
	ChannelCode  string           `json:"channelCode"`
	ChannelName  string           `json:"channelName"`
	PaymentType  string           `json:"paymentType"`
	Enabled      *bool            `json:"enabled"`
	ConfigParams *json.RawMessage `json:"configParams"`
}

type PaymentApp struct {
	BaseEntity
	AppCode     string `gorm:"column:app_code" json:"appCode"`
	AppName     string `gorm:"column:app_name" json:"appName"`
	ChannelCode string `gorm:"column:channel_code" json:"channelCode"`
	PaymentType string `gorm:"column:payment_type" json:"paymentType"`
	Enabled     bool   `gorm:"column:enabled" json:"enabled"`
	AppConfig   string `gorm:"column:app_config" json:"appConfig"`
}

func (PaymentApp) TableName() string { return "t_payment_app" }

type PaymentAppRequest struct {
	AppCode     string           `json:"appCode"`
	AppName     string           `json:"appName"`
	ChannelCode string           `json:"channelCode"`
	PaymentType string           `json:"paymentType"`
	Enabled     *bool            `json:"enabled"`
	AppConfig   *json.RawMessage `json:"appConfig"`
}

type RefundInfo struct {
	BaseEntity
	OrderNo        string           `gorm:"column:order_no" json:"orderNo"`
	RefundNo       string           `gorm:"column:refund_no" json:"refundNo"`
	RefundID       string           `gorm:"column:refund_id" json:"refundId"`
	TotalFee       int              `gorm:"column:total_fee" json:"totalFee"`
	Refund         int              `gorm:"column:refund" json:"refund"`
	Reason         string           `gorm:"column:reason" json:"reason"`
	ApprovalStatus string           `gorm:"column:approval_status" json:"approvalStatus"`
	ApproveRemark  string           `gorm:"column:approve_remark" json:"approveRemark"`
	ApprovedTime   *types.LocalTime `gorm:"column:approved_time" json:"approvedTime"`
	RefundStatus   string           `gorm:"column:refund_status" json:"refundStatus"`
	ContentReturn  string           `gorm:"column:content_return" json:"contentReturn"`
	ContentNotify  string           `gorm:"column:content_notify" json:"contentNotify"`
}

func (RefundInfo) TableName() string { return "t_refund_info" }

type RefundRequest struct {
	OrderNo      string `json:"orderNo"`
	RefundAmount *int   `json:"refundAmount"`
	Reason       string `json:"reason"`
}

type RefundApproveRequest struct {
	ApproveRemark string `json:"approveRemark"`
}

type OrderCloseMessage struct {
	OrderNo     string `json:"orderNo"`
	PaymentType string `json:"paymentType"`
}

type RefundStatusSyncResult struct {
	OrderNo       string
	RefundNo      string
	RefundID      string
	ChannelStatus string
	RefundStatus  string
	Content       string
	TotalFee      *int
	RefundAmount  *int
}

func (r RefundStatusSyncResult) HasRefundStatus() bool { return r.RefundStatus != "" }
