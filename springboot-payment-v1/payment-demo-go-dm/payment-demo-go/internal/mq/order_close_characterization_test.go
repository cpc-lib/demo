package mq

import (
	"encoding/json"
	"testing"

	"payment-demo-go/internal/model"
)

func TestOrderCloseEvent_CurrentNamesAreExternalContract(t *testing.T) {
	if OrderCloseEventExchange != "payment.order.close.event.exchange" {
		t.Fatalf("event exchange = %q", OrderCloseEventExchange)
	}
	if OrderCloseDeadLetterExchange != "payment.order.close.dead-letter.exchange" {
		t.Fatalf("dead-letter exchange = %q", OrderCloseDeadLetterExchange)
	}
	if OrderCloseDelayQueue != "payment.order.close.delay.queue" {
		t.Fatalf("delay queue = %q", OrderCloseDelayQueue)
	}
	if OrderCloseReleaseQueue != "payment.order.close.release.queue" {
		t.Fatalf("release queue = %q", OrderCloseReleaseQueue)
	}
	if OrderCloseDelayRoutingKey != "payment.order.close.delay" {
		t.Fatalf("delay routing key = %q", OrderCloseDelayRoutingKey)
	}
	if OrderCloseReleaseRoutingKey != "payment.order.close.release" {
		t.Fatalf("release routing key = %q", OrderCloseReleaseRoutingKey)
	}
}

func TestOrderCloseMessage_CurrentJSONAuditEventShape(t *testing.T) {
	body, err := json.Marshal(model.OrderCloseMessage{OrderNo: "ORDER-1", PaymentType: "微信"})
	if err != nil {
		t.Fatal(err)
	}

	if string(body) != `{"orderNo":"ORDER-1","paymentType":"微信"}` {
		t.Fatalf("body = %s", body)
	}
}

func TestRefundQueryEvent_CurrentNamesAreExternalContract(t *testing.T) {
	if RefundQueryEventExchange != "payment.refund.query.event.exchange" {
		t.Fatalf("event exchange = %q", RefundQueryEventExchange)
	}
	if RefundQueryDeadLetterExchange != "payment.refund.query.dead-letter.exchange" {
		t.Fatalf("dead-letter exchange = %q", RefundQueryDeadLetterExchange)
	}
	if RefundQueryDelayQueue != "payment.refund.query.delay.queue" {
		t.Fatalf("delay queue = %q", RefundQueryDelayQueue)
	}
	if RefundQueryReleaseQueue != "payment.refund.query.release.queue" {
		t.Fatalf("release queue = %q", RefundQueryReleaseQueue)
	}
	if RefundQueryDelayRoutingKey != "payment.refund.query.delay" {
		t.Fatalf("delay routing key = %q", RefundQueryDelayRoutingKey)
	}
	if RefundQueryReleaseRoutingKey != "payment.refund.query.release" {
		t.Fatalf("release routing key = %q", RefundQueryReleaseRoutingKey)
	}
}

func TestRefundQueryMessage_CurrentJSONAuditEventShape(t *testing.T) {
	body, err := json.Marshal(model.RefundQueryMessage{RefundNo: "REFUND-1", Attempt: 1})
	if err != nil {
		t.Fatal(err)
	}

	if string(body) != `{"refundNo":"REFUND-1","attempt":1}` {
		t.Fatalf("body = %s", body)
	}
}
