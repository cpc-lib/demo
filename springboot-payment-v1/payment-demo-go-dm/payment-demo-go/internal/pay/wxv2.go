package pay

import (
	"bytes"
	"context"
	"crypto/md5"
	"encoding/hex"
	"encoding/xml"
	"fmt"
	"io"
	"net/http"
	"sort"
	"strings"
	"time"

	"payment-demo-go/internal/config"
)

type WxV2Client struct {
	cfg        config.WxPayConfig
	httpClient *http.Client
}

func NewWxV2Client(cfg config.WxPayConfig) *WxV2Client {
	return &WxV2Client{cfg: cfg, httpClient: &http.Client{Timeout: 15 * time.Second}}
}

func (c *WxV2Client) PostXML(ctx context.Context, rawURL string, params map[string]string) (string, map[string]string, error) {
	xmlBody := GenerateSignedXML(params, c.cfg.PartnerKey)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, rawURL, strings.NewReader(xmlBody))
	if err != nil {
		return "", nil, err
	}
	req.Header.Set("Content-Type", "application/xml; charset=utf-8")
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", nil, err
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	m, err := XMLToMap(body)
	return string(body), m, err
}

func GenerateSignedXML(params map[string]string, key string) string {
	copyMap := map[string]string{}
	for k, v := range params {
		if v != "" {
			copyMap[k] = v
		}
	}
	copyMap["sign"] = SignV2(copyMap, key)
	return MapToXML(copyMap)
}

func VerifyV2Sign(params map[string]string, key string) bool {
	sign := params["sign"]
	return sign != "" && strings.EqualFold(sign, SignV2(params, key))
}

func SignV2(params map[string]string, key string) string {
	keys := make([]string, 0, len(params))
	for k, v := range params {
		if k != "sign" && v != "" {
			keys = append(keys, k)
		}
	}
	sort.Strings(keys)
	parts := make([]string, 0, len(keys)+1)
	for _, k := range keys {
		parts = append(parts, fmt.Sprintf("%s=%s", k, params[k]))
	}
	parts = append(parts, "key="+key)
	sum := md5.Sum([]byte(strings.Join(parts, "&")))
	return strings.ToUpper(hex.EncodeToString(sum[:]))
}

func MapToXML(params map[string]string) string {
	var b strings.Builder
	b.WriteString("<xml>")
	keys := make([]string, 0, len(params))
	for k := range params {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	for _, k := range keys {
		b.WriteString("<" + k + "><![CDATA[")
		b.WriteString(params[k])
		b.WriteString("]]></" + k + ">")
	}
	b.WriteString("</xml>")
	return b.String()
}

func XMLToMap(data []byte) (map[string]string, error) {
	decoder := xml.NewDecoder(bytes.NewReader(data))
	result := map[string]string{}
	var current string
	for {
		tok, err := decoder.Token()
		if err == io.EOF {
			break
		}
		if err != nil {
			return nil, err
		}
		switch t := tok.(type) {
		case xml.StartElement:
			if t.Name.Local != "xml" {
				current = t.Name.Local
			}
		case xml.CharData:
			if current != "" {
				result[current] = string([]byte(t))
			}
		case xml.EndElement:
			if t.Name.Local == current {
				current = ""
			}
		}
	}
	return result, nil
}
