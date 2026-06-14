package lock

import (
	"context"
	"testing"
	"time"
)

func TestExecuteValue_CurrentBehaviorRejectsEmptyKeyBeforeRedisAccess(t *testing.T) {
	l := NewRedisLock(nil)

	_, err := l.ExecuteValue(context.Background(), "", time.Millisecond, time.Millisecond, func() (interface{}, error) {
		t.Fatalf("callback should not run for empty key")
		return nil, nil
	})

	if err == nil {
		t.Fatalf("expected error for empty key")
	}
	if err.Error() != "分布式锁key不能为空" {
		t.Fatalf("error = %q", err.Error())
	}
}
