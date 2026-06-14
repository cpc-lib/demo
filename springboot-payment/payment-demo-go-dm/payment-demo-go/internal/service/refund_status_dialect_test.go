package service

import "testing"

func TestRefundAmountSumExpression_CurrentBehaviorUsesPortableSQL(t *testing.T) {
	if refundAmountSumExpression != "COALESCE(sum(refund),0)" {
		t.Fatalf("refundAmountSumExpression = %q, want COALESCE(sum(refund),0)", refundAmountSumExpression)
	}
}
