package mq

import (
	"context"
	"encoding/json"
	"log"
	"time"

	"payment-demo-go/internal/model"

	amqp "github.com/rabbitmq/amqp091-go"
)

const (
	RefundQueryEventExchange      = "payment.refund.query.event.exchange"
	RefundQueryDeadLetterExchange = "payment.refund.query.dead-letter.exchange"
	RefundQueryDelayQueue         = "payment.refund.query.delay.queue"
	RefundQueryReleaseQueue       = "payment.refund.query.release.queue"
	RefundQueryDelayRoutingKey    = "payment.refund.query.delay"
	RefundQueryReleaseRoutingKey  = "payment.refund.query.release"

	refundQueryDelay = time.Minute
)

func (c *Client) declareRefundQuery() error {
	if err := c.ch.ExchangeDeclare(RefundQueryEventExchange, "direct", true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.ExchangeDeclare(RefundQueryDeadLetterExchange, "direct", true, false, false, false, nil); err != nil {
		return err
	}
	args := amqp.Table{
		"x-dead-letter-exchange":    RefundQueryDeadLetterExchange,
		"x-dead-letter-routing-key": RefundQueryReleaseRoutingKey,
		"x-message-ttl":             int64(refundQueryDelay / time.Millisecond),
	}
	if _, err := c.ch.QueueDeclare(RefundQueryDelayQueue, true, false, false, false, args); err != nil {
		return err
	}
	if _, err := c.ch.QueueDeclare(RefundQueryReleaseQueue, true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.QueueBind(RefundQueryDelayQueue, RefundQueryDelayRoutingKey, RefundQueryEventExchange, false, nil); err != nil {
		return err
	}
	return c.ch.QueueBind(RefundQueryReleaseQueue, RefundQueryReleaseRoutingKey, RefundQueryDeadLetterExchange, false, nil)
}

func (c *Client) SendRefundQueryMessage(ctx context.Context, refundNo string, attempt int) error {
	if attempt <= 0 {
		attempt = 1
	}
	body, _ := json.Marshal(model.RefundQueryMessage{RefundNo: refundNo, Attempt: attempt})
	return c.ch.PublishWithContext(ctx, RefundQueryEventExchange, RefundQueryDelayRoutingKey, false, false, amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Body:         body,
	})
}

func (c *Client) ConsumeRefundQuery(ctx context.Context, handler func(context.Context, model.RefundQueryMessage) error) error {
	msgs, err := c.ch.Consume(RefundQueryReleaseQueue, "", false, false, false, false, nil)
	if err != nil {
		return err
	}
	go func() {
		for {
			select {
			case <-ctx.Done():
				return
			case msg, ok := <-msgs:
				if !ok {
					return
				}
				var m model.RefundQueryMessage
				if err := json.Unmarshal(msg.Body, &m); err != nil {
					log.Printf("解析退款自动查询消息失败: %v", err)
					_ = msg.Ack(false)
					continue
				}
				if err := handler(ctx, m); err != nil {
					log.Printf("处理退款自动查询消息失败: %v", err)
					_ = msg.Nack(false, true)
					continue
				}
				_ = msg.Ack(false)
			}
		}
	}()
	return nil
}
