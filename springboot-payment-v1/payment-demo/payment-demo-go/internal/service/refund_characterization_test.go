package service

import (
	"errors"
	"reflect"
	"testing"

	"payment-demo-go/internal/constant"
)

func TestMapAliPayRefundStatus_CurrentBehaviorOnlySuccessMapsToLocalSuccess(t *testing.T) {
	if got := mapAliPayRefundStatus(constant.AliPayRefundSuccess); got != constant.RefundStatusSuccess {
		t.Fatalf("REFUND_SUCCESS maps to %q, want %q", got, constant.RefundStatusSuccess)
	}

	// 现状：支付宝退款异常等非成功状态映射为空字符串。
	if got := mapAliPayRefundStatus(constant.AliPayRefundError); got != "" {
		t.Fatalf("REFUND_ERROR maps to %q, want empty string", got)
	}
}

func TestMapWxRefundStatus_CurrentBehaviorIncludesLegacyChannelValues(t *testing.T) {
	cases := map[string]string{
		constant.WxRefundStatusSuccess:    constant.RefundStatusSuccess,
		constant.WxRefundStatusProcessing: constant.RefundStatusProcessing,
		constant.WxRefundStatusAbnormal:   constant.RefundStatusAbnormal,
		"CHANGE":                          constant.RefundStatusAbnormal,
		constant.WxRefundStatusClosed:     constant.RefundStatusClosed,
		"REFUNDCLOSE":                     constant.RefundStatusClosed,
		"UNKNOWN":                         "",
	}

	for input, want := range cases {
		if got := mapWxRefundStatus(input); got != want {
			t.Fatalf("mapWxRefundStatus(%q) = %q, want %q", input, got, want)
		}
	}
}

func TestSyncableStatuses_CurrentBehaviorStatusTransitionWhitelist(t *testing.T) {
	cases := map[string][]string{
		constant.RefundStatusSuccess: {
			constant.RefundStatusCreated,
			constant.RefundStatusProcessing,
			constant.RefundStatusFailed,
			constant.RefundStatusAbnormal,
		},
		constant.RefundStatusProcessing: {
			constant.RefundStatusCreated,
			constant.RefundStatusFailed,
		},
		constant.RefundStatusAbnormal: {
			constant.RefundStatusCreated,
			constant.RefundStatusProcessing,
			constant.RefundStatusFailed,
		},
		constant.RefundStatusClosed: {
			constant.RefundStatusCreated,
			constant.RefundStatusProcessing,
			constant.RefundStatusFailed,
			constant.RefundStatusAbnormal,
		},
		constant.RefundStatusFailed: {
			constant.RefundStatusCreated,
			constant.RefundStatusProcessing,
		},
	}

	for target, want := range cases {
		if got := syncableStatuses(target); !reflect.DeepEqual(got, want) {
			t.Fatalf("syncableStatuses(%q) = %#v, want %#v", target, got, want)
		}
	}

	if got := syncableStatuses("UNSUPPORTED"); got != nil {
		t.Fatalf("syncableStatuses(UNSUPPORTED) = %#v, want nil", got)
	}
}

func TestIsDuplicate_CurrentBehaviorMatchesErrorString(t *testing.T) {
	if !isDuplicate(errors.New("Error 1062: Duplicate entry 'x' for key 'uk_order_payment_type'")) {
		t.Fatalf("Duplicate error string should be treated as duplicate")
	}

	// 现状：duplicate 判断依赖错误字符串包含大写 Duplicate。
	if isDuplicate(errors.New("duplicate entry lower-case")) {
		t.Fatalf("lower-case duplicate should not match current behavior")
	}
	if isDuplicate(nil) {
		t.Fatalf("nil error should not match duplicate")
	}
}
