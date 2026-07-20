package pay

import (
	"bytes"
	"context"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"html"
	"io"
	"net/http"
	"net/url"
	"sort"
	"strings"
	"time"

	"payment-demo-go/internal/config"
)

type AliPayClient struct {
	cfg        config.AliPayConfig
	privateKey *rsa.PrivateKey
	publicKey  *rsa.PublicKey
	httpClient *http.Client
}

func NewAliPayClient(cfg config.AliPayConfig) (*AliPayClient, error) {
	pk, err := parseBase64PKCS8PrivateKey(cfg.MerchantPrivateKey)
	if err != nil {
		return nil, err
	}
	pub, err := parseBase64PublicKey(cfg.AlipayPublicKey)
	if err != nil {
		return nil, err
	}
	return &AliPayClient{cfg: cfg, privateKey: pk, publicKey: pub, httpClient: &http.Client{Timeout: 15 * time.Second}}, nil
}

func (c *AliPayClient) PagePayForm(bizContent string) (string, error) {
	params := c.commonParams("alipay.trade.page.pay", bizContent)
	params.Set("notify_url", c.cfg.NotifyURL)
	params.Set("return_url", c.cfg.ReturnURL)
	if err := c.signParams(params); err != nil {
		return "", err
	}
	var b strings.Builder
	b.WriteString(`<form name="punchout_form" method="post" action="` + html.EscapeString(c.cfg.GatewayURL) + `">`)
	keys := make([]string, 0, len(params))
	for k := range params {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	for _, k := range keys {
		b.WriteString(`<input type="hidden" name="` + html.EscapeString(k) + `" value="` + html.EscapeString(params.Get(k)) + `"/>`)
	}
	b.WriteString(`<input type="submit" value="立即支付" style="display:none" ></form>`)
	b.WriteString(`<script>document.forms[0].submit();</script>`)
	return b.String(), nil
}

func (c *AliPayClient) Execute(ctx context.Context, method, bizContent string) (string, error) {
	params := c.commonParams(method, bizContent)
	if err := c.signParams(params); err != nil {
		return "", err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.cfg.GatewayURL, strings.NewReader(params.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	return string(body), nil
}

func (c *AliPayClient) commonParams(method, bizContent string) url.Values {
	params := url.Values{}
	params.Set("app_id", c.cfg.AppID)
	params.Set("method", method)
	params.Set("format", "JSON")
	params.Set("charset", "utf-8")
	params.Set("sign_type", "RSA2")
	params.Set("timestamp", time.Now().Format("2006-01-02 15:04:05"))
	params.Set("version", "1.0")
	params.Set("biz_content", bizContent)
	return params
}

func (c *AliPayClient) signParams(params url.Values) error {
	content := alipaySignContent(params)
	h := sha256.Sum256([]byte(content))
	sig, err := rsa.SignPKCS1v15(rand.Reader, c.privateKey, crypto.SHA256, h[:])
	if err != nil {
		return err
	}
	params.Set("sign", base64.StdEncoding.EncodeToString(sig))
	return nil
}

func (c *AliPayClient) VerifyNotify(params url.Values) bool {
	sign := params.Get("sign")
	if sign == "" {
		return false
	}
	content := alipaySignContent(params)
	h := sha256.Sum256([]byte(content))
	sig, err := base64.StdEncoding.DecodeString(sign)
	if err != nil {
		return false
	}
	return rsa.VerifyPKCS1v15(c.publicKey, crypto.SHA256, h[:], sig) == nil
}

func alipaySignContent(params url.Values) string {
	keys := make([]string, 0, len(params))
	for k := range params {
		if k == "sign" || k == "sign_type" {
			continue
		}
		if params.Get(k) == "" {
			continue
		}
		keys = append(keys, k)
	}
	sort.Strings(keys)
	parts := make([]string, 0, len(keys))
	for _, k := range keys {
		parts = append(parts, k+"="+params.Get(k))
	}
	return strings.Join(parts, "&")
}

func parseBase64PKCS8PrivateKey(s string) (*rsa.PrivateKey, error) {
	der, err := base64.StdEncoding.DecodeString(strings.TrimSpace(s))
	if err != nil {
		return nil, err
	}
	key, err := x509.ParsePKCS8PrivateKey(der)
	if err != nil {
		return nil, err
	}
	pk, ok := key.(*rsa.PrivateKey)
	if !ok {
		return nil, fmt.Errorf("支付宝私钥不是 RSA")
	}
	return pk, nil
}

func parseBase64PublicKey(s string) (*rsa.PublicKey, error) {
	der, err := base64.StdEncoding.DecodeString(strings.TrimSpace(s))
	if err != nil {
		return nil, err
	}
	pubAny, err := x509.ParsePKIXPublicKey(der)
	if err != nil {
		return nil, err
	}
	pub, ok := pubAny.(*rsa.PublicKey)
	if !ok {
		return nil, fmt.Errorf("支付宝公钥不是 RSA")
	}
	return pub, nil
}

func AliPayResponseMap(body, key string) map[string]interface{} {
	var root map[string]interface{}
	dec := json.NewDecoder(bytes.NewBufferString(body))
	dec.UseNumber()
	if err := dec.Decode(&root); err != nil {
		return nil
	}
	m, _ := root[key].(map[string]interface{})
	return m
}

func AliPaySuccess(body, key string) bool {
	m := AliPayResponseMap(body, key)
	if m == nil {
		return false
	}
	code, _ := m["code"].(string)
	return code == "10000"
}

func FirstString(m map[string]interface{}, keys ...string) string {
	for _, k := range keys {
		if v, ok := m[k]; ok && v != nil {
			return fmt.Sprint(v)
		}
	}
	return ""
}

func (c *AliPayClient) DownloadBill(ctx context.Context, billURL string) (string, error) {
	if billURL == "" {
		return "", fmt.Errorf("账单下载地址为空")
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, billURL, nil)
	if err != nil {
		return "", err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}
	return string(body), nil
}
