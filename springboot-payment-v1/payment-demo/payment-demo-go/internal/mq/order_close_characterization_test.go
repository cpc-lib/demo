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
