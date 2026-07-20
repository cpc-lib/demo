package mq

import (
	"context"
	"encoding/json"
	"log"
	"time"

	"payment-demo-go/internal/config"
	"payment-demo-go/internal/model"

	amqp "github.com/rabbitmq/amqp091-go"
)

const (
	OrderCloseEventExchange      = "payment.order.close.event.exchange"
	OrderCloseDeadLetterExchange = "payment.order.close.dead-letter.exchange"
	OrderCloseDelayQueue         = "payment.order.close.delay.queue"
	OrderCloseReleaseQueue       = "payment.order.close.release.queue"
	OrderCloseDelayRoutingKey    = "payment.order.close.delay"
	OrderCloseReleaseRoutingKey  = "payment.order.close.release"

	ReconciliationTaskExchange      = "payment.reconciliation.task.exchange"
	ReconciliationTaskQueue     = "payment.reconciliation.task.queue"
	ReconciliationTaskRoutingKey = "payment.reconciliation.task"
	ReconciliationCompleteExchange = "payment.reconciliation.task.complete.exchange"
)

type Client struct {
	conn  *amqp.Connection
	ch    *amqp.Channel
	delay time.Duration
}

func New(cfg config.RabbitMQConfig, delay time.Duration) (*Client, error) {
	conn, err := amqp.Dial(cfg.URL)
	if err != nil {
		return nil, err
	}
	ch, err := conn.Channel()
	if err != nil {
		_ = conn.Close()
		return nil, err
	}
	c := &Client{conn: conn, ch: ch, delay: delay}
	if err := c.declare(); err != nil {
		_ = ch.Close()
		_ = conn.Close()
		return nil, err
	}
	return c, nil
}

func (c *Client) declare() error {
	if err := c.declareOrderClose(); err != nil {
		return err
	}
	if err := c.declareRefundQuery(); err != nil {
		return err
	}
	return c.declareReconciliationTask()
}

func (c *Client) declareReconciliationTask() error {
	if err := c.ch.ExchangeDeclare(ReconciliationTaskExchange, "direct", true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.ExchangeDeclare(ReconciliationCompleteExchange, "fanout", true, false, false, false, nil); err != nil {
		return err
	}
	if _, err := c.ch.QueueDeclare(ReconciliationTaskQueue, true, false, false, false, nil); err != nil {
		return err
	}
	return c.ch.QueueBind(ReconciliationTaskQueue, ReconciliationTaskRoutingKey, ReconciliationTaskExchange, false, nil)
}

func (c *Client) declareOrderClose() error {
	if err := c.ch.ExchangeDeclare(OrderCloseEventExchange, "direct", true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.ExchangeDeclare(OrderCloseDeadLetterExchange, "direct", true, false, false, false, nil); err != nil {
		return err
	}
	args := amqp.Table{
		"x-dead-letter-exchange":    OrderCloseDeadLetterExchange,
		"x-dead-letter-routing-key": OrderCloseReleaseRoutingKey,
		"x-message-ttl":             int64(c.delay / time.Millisecond),
	}
	if _, err := c.ch.QueueDeclare(OrderCloseDelayQueue, true, false, false, false, args); err != nil {
		return err
	}
	if _, err := c.ch.QueueDeclare(OrderCloseReleaseQueue, true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.QueueBind(OrderCloseDelayQueue, OrderCloseDelayRoutingKey, OrderCloseEventExchange, false, nil); err != nil {
		return err
	}
	return c.ch.QueueBind(OrderCloseReleaseQueue, OrderCloseReleaseRoutingKey, OrderCloseDeadLetterExchange, false, nil)
}

func (c *Client) SendCloseOrderMessage(ctx context.Context, orderNo, paymentType string) error {
	body, _ := json.Marshal(model.OrderCloseMessage{OrderNo: orderNo, PaymentType: paymentType})
	return c.ch.PublishWithContext(ctx, OrderCloseEventExchange, OrderCloseDelayRoutingKey, false, false, amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Body:         body,
	})
}

func (c *Client) ConsumeCloseOrder(ctx context.Context, handler func(context.Context, model.OrderCloseMessage) error) error {
	msgs, err := c.ch.Consume(OrderCloseReleaseQueue, "", false, false, false, false, nil)
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
				var m model.OrderCloseMessage
				if err := json.Unmarshal(msg.Body, &m); err != nil {
					log.Printf("解析延迟关单消息失败: %v", err)
					_ = msg.Ack(false)
					continue
				}
				if err := handler(ctx, m); err != nil {
					log.Printf("处理延迟关单消息失败: %v", err)
					_ = msg.Nack(false, true)
					continue
				}
				_ = msg.Ack(false)
			}
		}
	}()
	return nil
}

func (c *Client) SendReconciliationTaskMessage(ctx context.Context, taskID int64, trigger string) error {
	body, _ := json.Marshal(model.ReconciliationTaskMessage{TaskID: taskID, Trigger: trigger})
	return c.ch.PublishWithContext(ctx, ReconciliationTaskExchange, ReconciliationTaskRoutingKey, false, false, amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Body:         body,
	})
}

func (c *Client) ConsumeReconciliationTask(ctx context.Context, handler func(context.Context, model.ReconciliationTaskMessage) error) error {
	msgs, err := c.ch.Consume(ReconciliationTaskQueue, "", false, false, false, false, nil)
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
				var m model.ReconciliationTaskMessage
				if err := json.Unmarshal(msg.Body, &m); err != nil {
					log.Printf("解析对账任务消息失败: %v", err)
					_ = msg.Ack(false)
					continue
				}
				if err := handler(ctx, m); err != nil {
					log.Printf("处理对账任务消息失败: %v", err)
					_ = msg.Nack(false, true)
					continue
				}
				_ = msg.Ack(false)
			}
		}
	}()
	return nil
}
