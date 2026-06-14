package response

import (
	"testing"

	"github.com/gin-gonic/gin"
)

func TestOK_CurrentResponseContract(t *testing.T) {
	r := OK()

	if r.Code != 0 {
		t.Fatalf("code = %d, want 0", r.Code)
	}
	if r.Message != "成功" {
		t.Fatalf("message = %q, want 成功", r.Message)
	}
	data, ok := r.Data.(gin.H)
	if !ok {
		t.Fatalf("data type = %T, want gin.H", r.Data)
	}
	if len(data) != 0 {
		t.Fatalf("data = %#v, want empty map", data)
	}
}

func TestError_CurrentResponseContractUsesBusinessCodeMinusOne(t *testing.T) {
	r := Error("boom")

	if r.Code != -1 {
		t.Fatalf("code = %d, want -1", r.Code)
	}
	if r.Message != "boom" {
		t.Fatalf("message = %q, want boom", r.Message)
	}
	data, ok := r.Data.(gin.H)
	if !ok {
		t.Fatalf("data type = %T, want gin.H", r.Data)
	}
	if len(data) != 0 {
		t.Fatalf("data = %#v, want empty map", data)
	}
}

func TestWithData_CurrentBehaviorMutatesMapStyleData(t *testing.T) {
	r := WithData(OK(), "message", "hello")
	r = WithData(r, "now", "2026-06-12 00:00:00")

	data, ok := r.Data.(gin.H)
	if !ok {
		t.Fatalf("data type = %T, want gin.H", r.Data)
	}
	if data["message"] != "hello" {
		t.Fatalf("data[message] = %#v, want hello", data["message"])
	}
	if data["now"] != "2026-06-12 00:00:00" {
		t.Fatalf("data[now] = %#v", data["now"])
	}
}
