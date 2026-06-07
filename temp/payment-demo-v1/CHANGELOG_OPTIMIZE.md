# 本次优化说明

## 1. 统一下单处理
- 新增 `OrderInfoService#createOrReuseOrder`，微信 Native / 微信 V2 / 支付宝下单统一走同一个订单创建入口。
- 统一职责：
  - 校验商品是否存在
  - 复用同商品、同支付方式、未支付订单
  - 不重复生成本地订单

## 2. 支持多次退款
- 新增通用退款申请入口：`RefundInfoService#createRefund(orderNo, refundAmount, reason)`
- 支持同一订单多次部分退款。
- 严格校验：`累计处理中 + 累计成功退款金额 <= 订单总金额`
- 若 `refundAmount` 为空，则默认退“剩余可退金额”。

## 3. 本地退款状态统一
新增本地退款状态枚举 `RefundStatus`：
- `PROCESSING`
- `SUCCESS`
- `FAILED`
- `ABNORMAL`
- `CLOSED`

说明：
- 本地库不再依赖第三方渠道返回状态直接入库作为唯一语义。
- 第三方原始报文仍保存在 `content_return/content_notify` 中。

## 4. 订单退款状态自动刷新
新增订单状态：`PARTIAL_REFUND(部分退款)`

订单状态会根据退款结果自动计算：
- 成功退款金额 = 订单总金额 -> `REFUND_SUCCESS`
- 成功退款金额 > 0 且 < 订单总金额 -> `PARTIAL_REFUND`
- 处理中退款金额 > 0 -> `REFUND_PROCESSING`
- 无处理中、无成功退款 -> `SUCCESS`

## 5. 支付宝多次退款修复
- 退款请求增加 `out_request_no = refundNo`
- 查询退款改为按 `refundNo` 查询，再反查 `orderNo`

这一步是支付宝支持“一笔订单多次部分退款”的关键。

## 6. 新退款接口
### 微信退款
`POST /api/wx-pay/refunds`

请求体：
```json
{
  "orderNo": "202604190001",
  "refundAmount": 100,
  "reason": "用户申请部分退款"
}
```

### 支付宝退款
`POST /api/ali-pay/trade/refund`

请求体：
```json
{
  "orderNo": "202604190001",
  "refundAmount": 100,
  "reason": "用户申请部分退款"
}
```

> 旧接口仍保留：
> - `/api/wx-pay/refunds/{orderNo}/{reason}`
> - `/api/ali-pay/trade/refund/{orderNo}/{reason}`
>
> 旧接口会默认退剩余可退金额。

## 7. SQL 优化
- `t_order_info.order_no` 唯一索引
- `t_refund_info.refund_no` 唯一索引
- 增加退款/订单常用查询索引
- `code_url` 扩大到 `varchar(512)`，避免二维码地址被截断

## 8. 错误返回优化
- 新增 `BizException`
- 新增全局异常处理 `GlobalExceptionHandler`
- 业务错误会直接返回可读消息
