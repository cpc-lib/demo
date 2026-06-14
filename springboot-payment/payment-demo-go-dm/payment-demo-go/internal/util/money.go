package util

import (
	"math"
	"strconv"
)

func CentsToYuan(cents int) string {
	return strconv.FormatFloat(float64(cents)/100, 'f', 2, 64)
}

func YuanToCents(yuan string) (int, error) {
	f, err := strconv.ParseFloat(yuan, 64)
	if err != nil {
		return 0, err
	}
	return int(math.Round(f * 100)), nil
}
