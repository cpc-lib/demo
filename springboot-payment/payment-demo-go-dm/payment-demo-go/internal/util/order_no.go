package util

import (
	"fmt"
	"math/rand"
	"time"
)

func OrderNo() string            { return "ORD" + no() }
func RefundNo() string           { return "RFD" + no() }
func ReconciliationTaskNo() string { return "REC" + no() }

func no() string {
	return time.Now().Format("20060102150405") + fmt.Sprintf("%04d", rand.Intn(10000))
}
