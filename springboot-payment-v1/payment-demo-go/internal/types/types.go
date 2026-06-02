package types

import (
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"strconv"
	"time"
)

type ID uint64

func (id ID) MarshalJSON() ([]byte, error) {
	if id == 0 {
		return []byte(`"0"`), nil
	}
	return []byte(`"` + strconv.FormatUint(uint64(id), 10) + `"`), nil
}

type LocalTime struct{ time.Time }

func Now() LocalTime { return LocalTime{Time: time.Now()} }

func (t LocalTime) MarshalJSON() ([]byte, error) {
	if t.Time.IsZero() {
		return []byte("null"), nil
	}
	return json.Marshal(t.Time.Format("2006-01-02 15:04:05"))
}

func (t *LocalTime) UnmarshalJSON(data []byte) error {
	if string(data) == "null" || string(data) == `""` {
		*t = LocalTime{}
		return nil
	}
	var s string
	if err := json.Unmarshal(data, &s); err != nil {
		return err
	}
	parsed, err := time.ParseInLocation("2006-01-02 15:04:05", s, time.Local)
	if err != nil {
		parsed, err = time.Parse(time.RFC3339, s)
		if err != nil {
			return err
		}
	}
	*t = LocalTime{Time: parsed}
	return nil
}

func (t LocalTime) Value() (driver.Value, error) {
	if t.Time.IsZero() {
		return nil, nil
	}
	return t.Time, nil
}

func (t *LocalTime) Scan(value interface{}) error {
	if value == nil {
		*t = LocalTime{}
		return nil
	}
	switch v := value.(type) {
	case time.Time:
		*t = LocalTime{Time: v}
		return nil
	case []byte:
		return t.scanString(string(v))
	case string:
		return t.scanString(v)
	default:
		return fmt.Errorf("unsupported LocalTime scan type %T", value)
	}
}

func (t *LocalTime) scanString(s string) error {
	parsed, err := time.ParseInLocation("2006-01-02 15:04:05", s, time.Local)
	if err != nil {
		return err
	}
	*t = LocalTime{Time: parsed}
	return nil
}
