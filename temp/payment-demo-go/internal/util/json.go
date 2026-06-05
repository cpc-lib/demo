package util

import "encoding/json"

func ToJSON(v interface{}) string {
	b, _ := json.Marshal(v)
	return string(b)
}

func ToMap(data []byte) (map[string]interface{}, error) {
	m := map[string]interface{}{}
	err := json.Unmarshal(data, &m)
	return m, err
}
