package handler

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestReconciliationRoutes_Registered(t *testing.T) {
	r := newTestRouter(t)
	cases := []struct {
		method string
		path   string
	}{
		{http.MethodPost, "/api/reconciliation/task"},
		{http.MethodGet, "/api/reconciliation/task/list"},
		{http.MethodGet, "/api/reconciliation/task/1"},
		{http.MethodPost, "/api/reconciliation/task/1/execute"},
		{http.MethodGet, "/api/reconciliation/task/1/diff"},
		{http.MethodPost, "/api/reconciliation/diff/1/handle"},
		{http.MethodGet, "/api/reconciliation/summary"},
	}
	for _, c := range cases {
		w := httptest.NewRecorder()
		req := httptest.NewRequest(c.method, c.path, nil)
		r.ServeHTTP(w, req)
		if w.Code == http.StatusNotFound {
			t.Fatalf("route %s %s returned 404, route not registered", c.method, c.path)
		}
	}
}

func TestExecuteReconciliationTask_InvalidTaskIDReturnsBizError(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/reconciliation/task/abc/execute", nil)
	r.ServeHTTP(w, req)
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != float64(-1) {
		t.Fatalf("code = %#v, want -1 for invalid task id", body["code"])
	}
}

func TestGetReconciliationTask_InvalidTaskIDReturnsBizError(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/reconciliation/task/0", nil)
	r.ServeHTTP(w, req)
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != float64(-1) {
		t.Fatalf("code = %#v, want -1 for invalid task id", body["code"])
	}
}

func TestHandleReconciliationDiff_InvalidDiffIDReturnsBizError(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/reconciliation/diff/0/handle", nil)
	r.ServeHTTP(w, req)
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != float64(-1) {
		t.Fatalf("code = %#v, want -1 for invalid diff id", body["code"])
	}
}

func TestCreateReconciliationTask_EmptyBodyReturnsBizError(t *testing.T) {
	r := newTestRouter(t)
	w := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/reconciliation/task", nil)
	r.ServeHTTP(w, req)
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200; body=%s", w.Code, w.Body.String())
	}
	body := decodeJSONBody(t, w.Body.String())
	if body["code"] != float64(-1) {
		t.Fatalf("code = %#v, want -1 for empty body", body["code"])
	}
}
