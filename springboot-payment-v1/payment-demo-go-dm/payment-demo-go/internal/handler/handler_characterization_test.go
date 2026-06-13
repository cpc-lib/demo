package handler

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/gin-gonic/gin"
)

func newTestRouter(t *testing.T) *gin.Engine {
	t.Helper()
	oldMode := gin.Mode()
	gin.SetMode(gin.TestMode)
	t.Cleanup(func() {
		gin.SetMode(oldMode)
	})
	r := gin.New()
	New(nil).Register(r)
	return r
}

func decodeJSONBody(t *testing.T, body string) map[string]interface{} {
	t.Helper()
	var out map[string]interface{}
	if err := json.Unmarshal([]byte(body), &out); err != nil {
		t.Fatalf("decode json body %q: %v", body, err)
	}
	return out
}

func TestProductTest_PublicAPIResponseShape(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/product/test", nil)

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != float64(0) {
		t.Fatalf("code = %#v, want 0", body["code"])
	}
	if body["message"] != "成功" {
		t.Fatalf("message = %#v, want 成功", body["message"])
	}
	data, ok := body["data"].(map[string]interface{})
	if !ok {
		t.Fatalf("data type = %T, want object", body["data"])
	}
	if data["message"] != "hello" {
		t.Fatalf("data.message = %#v, want hello", data["message"])
	}
	if data["now"] == "" {
		t.Fatalf("data.now is empty")
	}
}

func TestRecoverJSON_CurrentBehaviorPanicBecomesHTTP200BusinessError(t *testing.T) {
	oldMode := gin.Mode()
	gin.SetMode(gin.TestMode)
	t.Cleanup(func() {
		gin.SetMode(oldMode)
	})
	h := &Handler{}
	r := gin.New()
	r.Use(h.recoverJSON())
	r.GET("/panic", func(c *gin.Context) {
		panic("boom")
	})

	w := httptest.NewRecorder()
	r.ServeHTTP(w, httptest.NewRequest(http.MethodGet, "/panic", nil))

	// 现状：普通 panic/业务错误被包装为 HTTP 200 + code=-1。
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != float64(-1) {
		t.Fatalf("code = %#v, want -1", body["code"])
	}
	if body["message"] != "boom" {
		t.Fatalf("message = %#v, want boom", body["message"])
	}
}

func TestCORSOptions_CurrentPublicAPIBehavior(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodOptions, "/api/product/list", nil)

	r.ServeHTTP(w, req)

	if w.Code != http.StatusNoContent {
		t.Fatalf("status = %d, want 204", w.Code)
	}
	if got := w.Header().Get("Access-Control-Allow-Origin"); got != "*" {
		t.Fatalf("Access-Control-Allow-Origin = %q, want *", got)
	}
	if got := w.Header().Get("Access-Control-Allow-Methods"); !strings.Contains(got, "GET") || !strings.Contains(got, "POST") {
		t.Fatalf("Access-Control-Allow-Methods = %q, want GET and POST", got)
	}
}

func TestWxV2Notify_CurrentBehaviorMalformedXMLRejectedBeforeRedisOrService(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/wx-pay-v2/native/notify", strings.NewReader("<xml><bad"))

	r.ServeHTTP(w, req)

	// 现状：微信 V2 XML 解析失败返回 HTTP 200 + FAIL XML，不访问 Redis/DB/Service。
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := w.Body.String()
	if !strings.Contains(body, "<return_code><![CDATA[FAIL]]></return_code>") {
		t.Fatalf("body = %s, want FAIL return_code", body)
	}
	if !strings.Contains(body, "<return_msg><![CDATA[失败]]></return_msg>") {
		t.Fatalf("body = %s, want 失败 return_msg", body)
	}
}

func TestWxV3Notify_CurrentBehaviorMalformedJSONRejectedWithProviderError(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/wx-pay/native/notify", strings.NewReader("{bad-json"))

	r.ServeHTTP(w, req)

	// 现状：微信 V3 非法 JSON 返回 HTTP 500 + 渠道错误结构，不访问 Redis/DB/Service。
	if w.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, want 500; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != "ERROR" {
		t.Fatalf("code = %#v, want ERROR", body["code"])
	}
	if body["message"] != "失败" {
		t.Fatalf("message = %#v, want 失败", body["message"])
	}
}

func TestPaymentAppIDQuery_CurrentBehaviorRejectsNegativeBeforeService(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/wx-pay/native/1?paymentAppId=-1", nil)

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != float64(-1) {
		t.Fatalf("code = %#v, want -1", body["code"])
	}
	if body["message"] == "" {
		t.Fatalf("message should not be empty")
	}
}

func TestPaymentAppIDQuery_RequiredForOrderEndpoints(t *testing.T) {
	cases := []struct {
		name string
		path string
	}{
		{name: "wx v3 native", path: "/api/wx-pay/native/1"},
		{name: "wx v2 native", path: "/api/wx-pay-v2/native/1"},
		{name: "alipay page pay", path: "/api/ali-pay/trade/page/pay/1"},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			r := newTestRouter(t)
			w := httptest.NewRecorder()
			req := httptest.NewRequest(http.MethodPost, tc.path, nil)

			r.ServeHTTP(w, req)

			if w.Code != http.StatusOK {
				t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
			}
			body := decodeJSONBody(t, w.Body.String())
			if body["code"] != float64(-1) {
				t.Fatalf("code = %#v, want -1", body["code"])
			}
			if body["message"] != "paymentAppId不能为空" {
				t.Fatalf("message = %#v, want paymentAppId不能为空", body["message"])
			}
		})
	}
}

func TestPaymentAppList_CurrentBehaviorRejectsInvalidEnabledBeforeService(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/payment-app/list?enabled=maybe", nil)

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != float64(-1) {
		t.Fatalf("code = %#v, want -1", body["code"])
	}
	if body["message"] != "enabled must be true or false" {
		t.Fatalf("message = %#v, want enabled parse error", body["message"])
	}
}
