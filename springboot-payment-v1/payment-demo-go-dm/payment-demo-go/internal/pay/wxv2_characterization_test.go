package pay

import (
	"strings"
	"testing"
)

func TestSignV2_CurrentBehaviorIgnoresExistingSignAndEmptyValues(t *testing.T) {
	params := map[string]string{
		"appid":        "wx-app",
		"mch_id":       "mch-1",
		"body":         "",
		"out_trade_no": "ORDER-1",
		"sign":         "SHOULD_BE_IGNORED",
	}

	got := SignV2(params, "partner-key")
	withoutSign := SignV2(map[string]string{
		"appid":        "wx-app",
		"mch_id":       "mch-1",
		"out_trade_no": "ORDER-1",
	}, "partner-key")

	if got != withoutSign {
		t.Fatalf("sign = %q, want same as map without sign/empty values %q", got, withoutSign)
	}
	if !VerifyV2Sign(map[string]string{
		"appid":        "wx-app",
		"mch_id":       "mch-1",
		"out_trade_no": "ORDER-1",
		"sign":         got,
	}, "partner-key") {
		t.Fatalf("VerifyV2Sign returned false for generated sign")
	}
}

func TestMapToXML_CurrentBehaviorSortsFieldsAndWrapsCDATA(t *testing.T) {
	xml := MapToXML(map[string]string{
		"return_msg":  "OK",
		"return_code": "SUCCESS",
	})

	want := "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>"
	if xml != want {
		t.Fatalf("xml = %q, want %q", xml, want)
	}
}

func TestXMLToMap_CurrentBehaviorParsesSimpleWxXML(t *testing.T) {
	m, err := XMLToMap([]byte("<xml><return_code><![CDATA[SUCCESS]]></return_code><total_fee>1</total_fee></xml>"))
	if err != nil {
		t.Fatalf("XMLToMap error = %v", err)
	}
	if m["return_code"] != "SUCCESS" || m["total_fee"] != "1" {
		t.Fatalf("map = %#v", m)
	}

	_, err = XMLToMap([]byte("<xml><bad"))
	if err == nil || !strings.Contains(err.Error(), "EOF") {
		t.Fatalf("malformed XML error = %v, want EOF parse error", err)
	}
}
