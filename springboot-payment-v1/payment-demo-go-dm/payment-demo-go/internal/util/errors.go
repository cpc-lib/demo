package util

import "fmt"

type BizError struct{ Message string }

func (e BizError) Error() string { return e.Message }

func Biz(message string) error { return BizError{Message: message} }

func Bizf(format string, args ...interface{}) error {
	return BizError{Message: fmt.Sprintf(format, args...)}
}
