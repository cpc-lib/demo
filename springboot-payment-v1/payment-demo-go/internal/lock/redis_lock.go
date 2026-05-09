package lock

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/redis/go-redis/v9"
)

type RedisLock struct{ rdb *redis.Client }

func NewRedisLock(rdb *redis.Client) *RedisLock { return &RedisLock{rdb: rdb} }

func (l *RedisLock) Execute(ctx context.Context, key string, wait, lease time.Duration, fn func() error) error {
	_, err := l.ExecuteValue(ctx, key, wait, lease, func() (interface{}, error) { return nil, fn() })
	return err
}

func (l *RedisLock) ExecuteValue(ctx context.Context, key string, wait, lease time.Duration, fn func() (interface{}, error)) (interface{}, error) {
	if key == "" {
		return nil, ErrBusy("分布式锁key不能为空")
	}
	if lease <= 0 {
		lease = 30 * time.Second
	}
	if wait <= 0 {
		wait = 3 * time.Second
	}
	value := uuid.NewString()
	deadline := time.Now().Add(wait)
	for {
		ok, err := l.rdb.SetNX(ctx, key, value, lease).Result()
		if err != nil {
			return nil, err
		}
		if ok {
			break
		}
		if time.Now().After(deadline) {
			return nil, ErrBusy("系统繁忙，请勿重复提交")
		}
		select {
		case <-ctx.Done():
			return nil, ctx.Err()
		case <-time.After(50 * time.Millisecond):
		}
	}
	defer l.release(ctx, key, value)
	return fn()
}

func (l *RedisLock) release(ctx context.Context, key, value string) {
	const script = `if redis.call("get", KEYS[1]) == ARGV[1] then return redis.call("del", KEYS[1]) else return 0 end`
	_ = l.rdb.Eval(ctx, script, []string{key}, value).Err()
}

type ErrBusy string

func (e ErrBusy) Error() string { return string(e) }
