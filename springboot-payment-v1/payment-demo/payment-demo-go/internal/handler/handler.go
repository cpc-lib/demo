package handler

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"payment-demo-go/internal/constant"
	"payment-demo-go/internal/model"
	"payment-demo-go/internal/pay"
	"payment-demo-go/internal/response"
	"payment-demo-go/internal/service"
	"payment-demo-go/internal/types"
	"payment-demo-go/internal/util"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

const (
	wxV3NotifyKeyPrefix = "payment:wx:notify:processed:"
	wxV2NotifyKeyPrefix = "payment:wx:v2:notify:processed:"
	notifyExpire        = 24 * time.Hour
)

type Handler struct{ Svc *service.Service }

func New(s *service.Service) *Handler { return &Handler{Svc: s} }

func (h *Handler) Register(r *gin.Engine) {
	r.Use(h.recoverJSON(), cors())

	product := r.Group("/api/product")
	product.GET("/test", h.productTest)
	product.GET("/list", h.productList)

	order := r.Group("/api/order-info")
	order.GET("/list", h.orderList)
	order.GET("/query-order-status/:orderNo", h.orderStatus)

	paymentChannel := r.Group("/api/payment-channel")
	paymentChannel.GET("/list", h.paymentChannelList)
	paymentChannel.POST("/save", h.paymentChannelSave)
	paymentChannel.POST("/update/:channelCode", h.paymentChannelUpdate)
	paymentChannel.POST("/delete/:channelCode", h.paymentChannelDelete)

	paymentApp := r.Group("/api/payment-app")
	paymentApp.GET("/list", h.paymentAppList)
	paymentApp.POST("/save", h.paymentAppSave)
	paymentApp.POST("/update/:appCode", h.paymentAppUpdate)
	paymentApp.POST("/delete/:appCode", h.paymentAppDelete)

	paymentConfig := r.Group("/api/payment-config")
	paymentConfig.GET("/apps", h.paymentConfigApps)
	paymentConfig.POST("/reload", h.paymentConfigReload)

	wx := r.Group("/api/wx-pay")
	wx.POST("/native/:productId", h.wxNativePay)
	wx.POST("/native/notify", h.wxNativeNotify)
	wx.POST("/cancel/:orderNo", h.wxCancel)
	wx.GET("/query/:orderNo", h.wxQuery)
	wx.GET("/check-order-status/:orderNo", h.wxCheckOrderStatus)
	wx.GET("/query-refund/:refundNo", h.wxQueryRefund)
	wx.POST("/refunds/notify", h.wxRefundNotify)
	wx.GET("/querybill/:billDate/:type", h.wxQueryBill)
	wx.GET("/downloadbill/:billDate/:type", h.wxDownloadBill)
	wx.POST("/jsapi", h.wxJSAPI)
	wx.POST("/jsapi/notify/v1", h.wxNativeNotify)

	wx2 := r.Group("/api/wx-pay-v2")
	wx2.POST("/native/:productId", h.wxNativePayV2)
	wx2.POST("/native/notify", h.wxV2Notify)

	ali := r.Group("/api/ali-pay")
	ali.POST("/trade/page/pay/:productId", h.aliTradePagePay)
	ali.POST("/trade/notify", h.aliTradeNotify)
	ali.POST("/trade/close/:orderNo", h.aliCancel)
	ali.GET("/trade/query/:orderNo", h.aliQuery)
	ali.GET("/trade/fastpay/refund/:refundNo", h.aliQueryRefund)
	ali.GET("/bill/downloadurl/query/:billDate/:type", h.aliQueryBill)

	r.POST("/api/refund-info/apply", h.refundApply)
	r.POST("/api/wx-pay/refunds", h.refundApply)
	r.POST("/api/ali-pay/trade/refund", h.refundApply)
	r.POST("/api/refund-info/apply/:orderNo/:reason", h.refundApplyLegacy)
	r.POST("/api/wx-pay/refunds/:orderNo/:reason", h.refundApplyLegacy)
	r.POST("/api/ali-pay/trade/refund/:orderNo/:reason", h.refundApplyLegacy)

	refund := r.Group("/api/refund-info")
	refund.GET("/list", h.refundList)
	refund.GET("/list/:orderNo", h.refundListByOrderNo)
	refund.POST("/approve/:refundNo", h.refundApprove)
	refund.POST("/reject/:refundNo", h.refundReject)
	refund.POST("/query/:refundNo", h.refundQueryStatus)
	refund.POST("/reconcile/:orderNo", h.refundReconcile)
}

func cors() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Origin,Content-Type,Accept,Authorization,Wechatpay-Serial,Wechatpay-Signature,Wechatpay-Nonce,Wechatpay-Timestamp")
		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		c.Next()
	}
}

func (h *Handler) recoverJSON() gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if r := recover(); r != nil {
				c.AbortWithStatusJSON(http.StatusOK, response.Error(toErrMessage(r)))
			}
		}()
		c.Next()
	}
}

func ok(c *gin.Context) { c.JSON(http.StatusOK, response.OK()) }
func okMsg(c *gin.Context, msg string) {
	c.JSON(http.StatusOK, response.R{Code: 0, Message: msg, Data: gin.H{}})
}
func okData(c *gin.Context, data interface{}) { c.JSON(http.StatusOK, response.OKData(data)) }
func okKey(c *gin.Context, key string, value interface{}) {
	c.JSON(http.StatusOK, response.WithData(response.OK(), key, value))
}
func fail(c *gin.Context, err error) {
	if err == nil {
		return
	}
	c.JSON(http.StatusOK, response.Error(err.Error()))
}
func failMsg(c *gin.Context, msg string) { c.JSON(http.StatusOK, response.Error(msg)) }

func (h *Handler) productTest(c *gin.Context) {
	c.JSON(http.StatusOK, response.WithData(response.WithData(response.OK(), "message", "hello"), "now", types.Now()))
}

func (h *Handler) productList(c *gin.Context) {
	list, err := h.Svc.ListProducts()
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "productList", list)
}

func (h *Handler) orderList(c *gin.Context) {
	list, err := h.Svc.ListOrders()
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "list", list)
}

func (h *Handler) orderStatus(c *gin.Context) {
	status, err := h.Svc.GetOrderStatus(c.Param("orderNo"))
	if err != nil {
		fail(c, err)
		return
	}
	if status == constant.OrderStatusSuccess {
		okMsg(c, "支付成功")
		return
	}
	c.JSON(http.StatusOK, response.R{Code: 101, Message: "支付中......", Data: gin.H{}})
}

func (h *Handler) paymentChannelList(c *gin.Context) {
	list, err := h.Svc.ListPaymentChannels()
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "list", list)
}

func (h *Handler) paymentChannelSave(c *gin.Context) {
	var req model.PaymentChannelRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		failMsg(c, err.Error())
		return
	}
	channel, err := h.Svc.SavePaymentChannel(req)
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "paymentChannel", channel)
}

func (h *Handler) paymentChannelUpdate(c *gin.Context) {
	var req model.PaymentChannelRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		failMsg(c, err.Error())
		return
	}
	channel, err := h.Svc.UpdatePaymentChannel(c.Param("channelCode"), req)
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "paymentChannel", channel)
}

func (h *Handler) paymentChannelDelete(c *gin.Context) {
	if err := h.Svc.DeletePaymentChannel(c.Param("channelCode")); err != nil {
		fail(c, err)
		return
	}
	okMsg(c, "payment channel deleted")
}

func (h *Handler) paymentAppList(c *gin.Context) {
	enabled, err := parseOptionalBool(c.Query("enabled"))
	if err != nil {
		fail(c, err)
		return
	}
	list, err := h.Svc.ListPaymentApps(c.Query("channelCode"), c.Query("paymentType"), enabled)
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "list", list)
}

func (h *Handler) paymentAppSave(c *gin.Context) {
	var req model.PaymentAppRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		failMsg(c, err.Error())
		return
	}
	app, err := h.Svc.SavePaymentApp(req)
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "paymentApp", app)
}

func (h *Handler) paymentAppUpdate(c *gin.Context) {
	var req model.PaymentAppRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		failMsg(c, err.Error())
		return
	}
	app, err := h.Svc.UpdatePaymentApp(c.Param("appCode"), req)
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "paymentApp", app)
}

func (h *Handler) paymentAppDelete(c *gin.Context) {
	if err := h.Svc.DeletePaymentApp(c.Param("appCode")); err != nil {
		fail(c, err)
		return
	}
	okMsg(c, "payment app deleted")
}

func (h *Handler) paymentConfigApps(c *gin.Context) {
	list, err := h.Svc.ListEnabledPaymentApps()
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "apps", list)
}

func (h *Handler) paymentConfigReload(c *gin.Context) {
	h.paymentConfigApps(c)
}

func (h *Handler) wxNativePay(c *gin.Context) {
	productID, err := parseID(c.Param("productId"))
	if err != nil {
		fail(c, err)
		return
	}
	paymentAppID, err := parsePaymentAppID(c.Query("paymentAppId"))
	if err != nil {
		fail(c, err)
		return
	}
	data, err := h.Svc.WxNativePayWithApp(c.Request.Context(), productID, paymentAppID)
	if err != nil {
		fail(c, err)
		return
	}
	okData(c, data)
}

func (h *Handler) wxNativePayV2(c *gin.Context) {
	productID, err := parseID(c.Param("productId"))
	if err != nil {
		fail(c, err)
		return
	}
	paymentAppID, err := parsePaymentAppID(c.Query("paymentAppId"))
	if err != nil {
		fail(c, err)
		return
	}
	data, err := h.Svc.WxNativePayV2WithApp(c.Request.Context(), productID, c.ClientIP(), paymentAppID)
	if err != nil {
		fail(c, err)
		return
	}
	okData(c, data)
}

func (h *Handler) wxCancel(c *gin.Context) {
	if err := h.Svc.WxCancelOrder(c.Request.Context(), c.Param("orderNo")); err != nil {
		fail(c, err)
		return
	}
	okMsg(c, "订单已取消")
}

func (h *Handler) wxQuery(c *gin.Context) {
	result, err := h.Svc.WxQueryOrder(c.Request.Context(), c.Param("orderNo"))
	if err != nil {
		fail(c, err)
		return
	}
	r := response.WithData(response.OK(), "result", result)
	r.Message = "查询成功"
	c.JSON(http.StatusOK, r)
}

func (h *Handler) wxCheckOrderStatus(c *gin.Context) {
	result, err := h.Svc.WxQueryPaymentStatus(c.Request.Context(), c.Param("orderNo"))
	if err != nil {
		fail(c, err)
		return
	}
	c.JSON(http.StatusOK, response.R{Code: 0, Message: "查询成功", Data: result})
}

func (h *Handler) wxQueryRefund(c *gin.Context) {
	result, err := h.Svc.WxQueryRefund(c.Request.Context(), c.Param("refundNo"))
	if err != nil {
		fail(c, err)
		return
	}
	r := response.WithData(response.OK(), "result", result)
	r.Message = "查询成功"
	c.JSON(http.StatusOK, r)
}

func (h *Handler) wxQueryBill(c *gin.Context) {
	downloadURL, err := h.Svc.WxQueryBill(c.Request.Context(), c.Param("billDate"), c.Param("type"), c.Query("billType"), c.Query("accountType"), c.Query("tarType"))
	if err != nil {
		fail(c, err)
		return
	}
	r := response.WithData(response.OK(), "downloadUrl", downloadURL)
	r.Message = "获取账单url成功"
	c.JSON(http.StatusOK, r)
}

func (h *Handler) wxDownloadBill(c *gin.Context) {
	result, err := h.Svc.WxDownloadBill(c.Request.Context(), c.Param("billDate"), c.Param("type"), c.Query("billType"), c.Query("accountType"), c.Query("tarType"))
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "result", result)
}

func (h *Handler) wxJSAPI(c *gin.Context) {
	order, openid, err := parseJSAPIRequest(c)
	if err != nil {
		fail(c, err)
		return
	}
	data, err := h.Svc.WxJSAPIPay(c.Request.Context(), order, openid)
	if err != nil {
		fail(c, err)
		return
	}
	okData(c, data)
}

func (h *Handler) wxNativeNotify(c *gin.Context) {
	h.handleWxV3Notify(c, h.Svc.WxProcessOrderNotifyPlain)
}

func (h *Handler) wxRefundNotify(c *gin.Context) {
	h.handleWxV3Notify(c, h.Svc.WxProcessRefundNotifyPlain)
}

func (h *Handler) handleWxV3Notify(c *gin.Context, processor func(context.Context, string, int64) error) {
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		wxV3Fail(c, "失败")
		return
	}
	var bodyMap map[string]interface{}
	if err := json.Unmarshal(body, &bodyMap); err != nil {
		wxV3Fail(c, "失败")
		return
	}
	notifyID, _ := bodyMap["id"].(string)
	if !h.tryAcquireNotify(c.Request.Context(), wxV3NotifyKeyPrefix, notifyID) {
		wxV3Success(c)
		return
	}
	plain, paymentAppID, err := h.Svc.VerifyAndDecryptWxV3Notification(c.Request.Context(), c.Request.Header, body, bodyMap)
	if err != nil {
		h.releaseNotify(c.Request.Context(), wxV3NotifyKeyPrefix, notifyID)
		wxV3Fail(c, "失败")
		return
	}
	if err := processor(c.Request.Context(), plain, paymentAppID); err != nil {
		h.releaseNotify(c.Request.Context(), wxV3NotifyKeyPrefix, notifyID)
		wxV3Fail(c, "失败")
		return
	}
	h.markNotifyProcessed(c.Request.Context(), wxV3NotifyKeyPrefix, notifyID)
	wxV3Success(c)
}

func wxV3Success(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"code": "SUCCESS", "message": "成功"})
}
func wxV3Fail(c *gin.Context, msg string) {
	c.JSON(http.StatusInternalServerError, gin.H{"code": "ERROR", "message": msg})
}

func (h *Handler) wxV2Notify(c *gin.Context) {
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		c.String(http.StatusOK, wxV2Response("FAIL", "失败"))
		return
	}
	notifyMap, err := pay.XMLToMap(body)
	if err != nil {
		c.String(http.StatusOK, wxV2Response("FAIL", "失败"))
		return
	}
	transactionID := notifyMap["transaction_id"]
	if !h.tryAcquireNotify(c.Request.Context(), wxV2NotifyKeyPrefix, transactionID) {
		c.String(http.StatusOK, wxV2Response("SUCCESS", "OK"))
		return
	}
	paymentAppID, err := h.Svc.VerifyWxV2NotifyApp(c.Request.Context(), notifyMap)
	if err != nil {
		h.releaseNotify(c.Request.Context(), wxV2NotifyKeyPrefix, transactionID)
		c.String(http.StatusOK, wxV2Response("FAIL", "验签失败"))
		return
	}
	if notifyMap["return_code"] != "SUCCESS" || notifyMap["result_code"] != "SUCCESS" {
		h.releaseNotify(c.Request.Context(), wxV2NotifyKeyPrefix, transactionID)
		c.String(http.StatusOK, wxV2Response("FAIL", "失败"))
		return
	}
	if _, err := strconv.Atoi(notifyMap["total_fee"]); err != nil {
		h.releaseNotify(c.Request.Context(), wxV2NotifyKeyPrefix, transactionID)
		c.String(http.StatusOK, wxV2Response("FAIL", "金额校验失败"))
		return
	}
	if err := h.Svc.WxProcessV2NotifyWithApp(c.Request.Context(), notifyMap, string(body), paymentAppID); err != nil {
		h.releaseNotify(c.Request.Context(), wxV2NotifyKeyPrefix, transactionID)
		c.String(http.StatusOK, wxV2Response("FAIL", "失败"))
		return
	}
	h.markNotifyProcessed(c.Request.Context(), wxV2NotifyKeyPrefix, transactionID)
	c.String(http.StatusOK, wxV2Response("SUCCESS", "OK"))
}

func wxV2Response(code, msg string) string {
	return "<xml><return_code><![CDATA[" + code + "]]></return_code><return_msg><![CDATA[" + msg + "]]></return_msg></xml>"
}

func (h *Handler) aliTradePagePay(c *gin.Context) {
	productID, err := parseID(c.Param("productId"))
	if err != nil {
		fail(c, err)
		return
	}
	paymentAppID, err := parsePaymentAppID(c.Query("paymentAppId"))
	if err != nil {
		fail(c, err)
		return
	}
	form, err := h.Svc.AliTradeCreateWithApp(c.Request.Context(), productID, paymentAppID)
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "formStr", form)
}

func (h *Handler) aliTradeNotify(c *gin.Context) {
	_ = c.Request.ParseForm()
	params := map[string]string{}
	for k, v := range c.Request.PostForm {
		if len(v) > 0 {
			params[k] = v[0]
		}
	}
	result := "failure"
	cfg, paymentAppID, err := h.Svc.VerifyAliNotifyApp(c.Request.Context(), c.Request.PostForm)
	if err != nil {
		c.String(http.StatusOK, result)
		return
	}
	order, err := h.Svc.GetOrderByOrderNo(params["out_trade_no"])
	if err != nil || order == nil {
		c.String(http.StatusOK, result)
		return
	}
	total, err := util.YuanToCents(params["total_amount"])
	if err != nil || total != order.TotalFee {
		c.String(http.StatusOK, result)
		return
	}
	if params["seller_id"] != cfg.SellerID {
		c.String(http.StatusOK, result)
		return
	}
	if params["app_id"] != cfg.AppID {
		c.String(http.StatusOK, result)
		return
	}
	if params["trade_status"] != constant.AliPayTradeSuccess {
		c.String(http.StatusOK, result)
		return
	}
	if err := h.Svc.AliProcessOrderWithApp(c.Request.Context(), params, paymentAppID); err != nil {
		c.String(http.StatusOK, result)
		return
	}
	c.String(http.StatusOK, "success")
}

func (h *Handler) aliCancel(c *gin.Context) {
	if err := h.Svc.AliCancelOrder(c.Request.Context(), c.Param("orderNo")); err != nil {
		fail(c, err)
		return
	}
	okMsg(c, "订单已取消")
}

func (h *Handler) aliQuery(c *gin.Context) {
	result, err := h.Svc.AliQueryOrder(c.Request.Context(), c.Param("orderNo"))
	if err != nil {
		fail(c, err)
		return
	}
	r := response.WithData(response.OK(), "result", result)
	r.Message = "查询成功"
	c.JSON(http.StatusOK, r)
}

func (h *Handler) aliQueryRefund(c *gin.Context) {
	result, err := h.Svc.AliQueryRefund(c.Request.Context(), c.Param("refundNo"))
	if err != nil {
		fail(c, err)
		return
	}
	r := response.WithData(response.OK(), "result", result)
	r.Message = "查询成功"
	c.JSON(http.StatusOK, r)
}

func (h *Handler) aliQueryBill(c *gin.Context) {
	downloadURL, err := h.Svc.AliQueryBill(c.Request.Context(), c.Param("billDate"), c.Param("type"))
	if err != nil {
		fail(c, err)
		return
	}
	r := response.WithData(response.OK(), "downloadUrl", downloadURL)
	r.Message = "获取账单url成功"
	c.JSON(http.StatusOK, r)
}

func (h *Handler) refundApply(c *gin.Context) {
	var req model.RefundRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		failMsg(c, err.Error())
		return
	}
	if _, err := h.Svc.CreateApplication(c.Request.Context(), req.OrderNo, req.RefundAmount, req.Reason); err != nil {
		fail(c, err)
		return
	}
	okMsg(c, "退款申请单创建成功，待审核")
}

func (h *Handler) refundApplyLegacy(c *gin.Context) {
	if _, err := h.Svc.CreateApplication(c.Request.Context(), c.Param("orderNo"), nil, c.Param("reason")); err != nil {
		fail(c, err)
		return
	}
	okMsg(c, "退款申请单创建成功，待审核")
}

func (h *Handler) refundList(c *gin.Context) {
	list, err := h.Svc.ListRefundAll()
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "list", list)
}

func (h *Handler) refundListByOrderNo(c *gin.Context) {
	list, err := h.Svc.ListRefundByOrderNo(c.Param("orderNo"))
	if err != nil {
		fail(c, err)
		return
	}
	okKey(c, "list", list)
}

func (h *Handler) refundApprove(c *gin.Context) {
	var req model.RefundApproveRequest
	_ = c.ShouldBindJSON(&req)
	if err := h.Svc.Approve(c.Request.Context(), c.Param("refundNo"), req.ApproveRemark); err != nil {
		fail(c, err)
		return
	}
	okMsg(c, "审核通过，退款已提交处理")
}

func (h *Handler) refundReject(c *gin.Context) {
	var req model.RefundApproveRequest
	_ = c.ShouldBindJSON(&req)
	if err := h.Svc.Reject(c.Request.Context(), c.Param("refundNo"), req.ApproveRemark); err != nil {
		fail(c, err)
		return
	}
	okMsg(c, "退款申请已拒绝")
}

func (h *Handler) refundQueryStatus(c *gin.Context) {
	info, err := h.Svc.QueryRefundStatus(c.Request.Context(), c.Param("refundNo"))
	if err != nil {
		fail(c, err)
		return
	}
	r := response.WithData(response.OK(), "refundInfo", info)
	r.Message = "退款状态查询完成"
	c.JSON(http.StatusOK, r)
}

func (h *Handler) refundReconcile(c *gin.Context) {
	list, err := h.Svc.ReconcileOrderRefundStatus(c.Request.Context(), c.Param("orderNo"))
	if err != nil {
		fail(c, err)
		return
	}
	r := response.WithData(response.OK(), "list", list)
	r.Message = "订单退款状态对账完成"
	c.JSON(http.StatusOK, r)
}

func (h *Handler) tryAcquireNotify(ctx context.Context, prefix, id string) bool {
	if strings.TrimSpace(id) == "" {
		return true
	}
	ok, err := h.Svc.Redis.SetNX(ctx, prefix+id, "processing", notifyExpire).Result()
	return err == nil && ok
}

func (h *Handler) releaseNotify(ctx context.Context, prefix, id string) {
	if strings.TrimSpace(id) == "" {
		return
	}
	key := prefix + id
	v, err := h.Svc.Redis.Get(ctx, key).Result()
	if err == nil && v == "processing" {
		_ = h.Svc.Redis.Del(ctx, key).Err()
	}
}

func (h *Handler) markNotifyProcessed(ctx context.Context, prefix, id string) {
	if strings.TrimSpace(id) == "" {
		return
	}
	_ = h.Svc.Redis.Set(ctx, prefix+id, "processed", notifyExpire).Err()
}

func parseID(s string) (int64, error) {
	id, err := strconv.ParseInt(s, 10, 64)
	if err != nil || id <= 0 {
		return 0, util.Biz("商品ID必须大于0")
	}
	return id, nil
}

func parsePaymentAppID(s string) (int64, error) {
	return service.ParseRequiredPaymentAppIDValue(s)
}

func parseOptionalBool(s string) (*bool, error) {
	s = strings.TrimSpace(s)
	if s == "" {
		return nil, nil
	}
	v, err := strconv.ParseBool(s)
	if err != nil {
		return nil, util.Biz("enabled must be true or false")
	}
	return &v, nil
}

func parseJSAPIRequest(c *gin.Context) (model.OrderInfo, string, error) {
	var order model.OrderInfo
	openid := c.Query("openid")
	if strings.Contains(c.GetHeader("Content-Type"), "application/json") {
		var raw map[string]interface{}
		if err := c.ShouldBindJSON(&raw); err != nil {
			return order, "", err
		}
		order.Title = strMap(raw, "title")
		order.OrderNo = strMap(raw, "orderNo")
		order.CodeURL = strMap(raw, "codeUrl")
		order.OrderStatus = strMap(raw, "orderStatus")
		order.PaymentType = strMap(raw, "paymentType")
		order.ProductID = int64Map(raw, "productId")
		order.TotalFee = intMap(raw, "totalFee")
		if openid == "" {
			openid = strMap(raw, "openid")
		}
	} else {
		_ = c.Request.ParseForm()
		form := c.Request.PostForm
		order.Title = form.Get("title")
		order.OrderNo = form.Get("orderNo")
		order.CodeURL = form.Get("codeUrl")
		order.OrderStatus = form.Get("orderStatus")
		order.PaymentType = form.Get("paymentType")
		order.ProductID, _ = strconv.ParseInt(form.Get("productId"), 10, 64)
		order.TotalFee, _ = strconv.Atoi(form.Get("totalFee"))
		if openid == "" {
			openid = form.Get("openid")
		}
	}
	if strings.TrimSpace(openid) == "" {
		return order, "", util.Biz("openid不能为空")
	}
	return order, openid, nil
}

func strMap(m map[string]interface{}, key string) string {
	if v, ok := m[key]; ok && v != nil {
		return toString(v)
	}
	return ""
}
func intMap(m map[string]interface{}, key string) int {
	if v, ok := m[key]; ok && v != nil {
		n, _ := strconv.Atoi(toString(v))
		return n
	}
	return 0
}
func int64Map(m map[string]interface{}, key string) int64 {
	if v, ok := m[key]; ok && v != nil {
		n, _ := strconv.ParseInt(toString(v), 10, 64)
		return n
	}
	return 0
}
func toString(v interface{}) string {
	switch x := v.(type) {
	case json.Number:
		return x.String()
	default:
		return strings.TrimSpace(strings.Trim(fmt.Sprint(x), "\""))
	}
}
func toErrMessage(v interface{}) string {
	switch x := v.(type) {
	case error:
		return x.Error()
	case string:
		return x
	default:
		return "失败"
	}
}

var _ = gorm.ErrRecordNotFound
