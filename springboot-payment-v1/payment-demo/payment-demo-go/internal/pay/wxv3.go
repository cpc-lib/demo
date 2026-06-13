package pay

import (
	"bytes"
	"context"
	"crypto"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"io"
	"math/big"
	"net/http"
	"net/url"
	"os"
	"strings"
	"sync"
	"time"

	"payment-demo-go/internal/config"
	"payment-demo-go/internal/constant"
)

type WxV3Client struct {
	cfg        config.WxPayConfig
	privateKey *rsa.PrivateKey
	httpClient *http.Client
	mu         sync.RWMutex
	certs      map[string]*x509.Certificate
}

type WxHTTPResponse struct {
	StatusCode int
	Body       string
}

func (r WxHTTPResponse) Successful() bool { return r.StatusCode == 200 || r.StatusCode == 204 }

func NewWxV3Client(cfg config.WxPayConfig) (*WxV3Client, error) {
	pk, err := LoadRSAPrivateKeyFromFile(cfg.PrivateKeyPath)
	if err != nil {
		return nil, err
	}
	return &WxV3Client{cfg: cfg, privateKey: pk, httpClient: &http.Client{Timeout: 15 * time.Second}, certs: map[string]*x509.Certificate{}}, nil
}

func (c *WxV3Client) Get(ctx context.Context, rawURL string) (string, error) {
	resp, err := c.Do(ctx, http.MethodGet, rawURL, "", true)
	if err != nil {
		return "", err
	}
	if !resp.Successful() {
		return "", fmt.Errorf("微信支付请求失败, 响应码=%d, 返回=%s", resp.StatusCode, resp.Body)
	}
	return resp.Body, nil
}

func (c *WxV3Client) GetNoSign(ctx context.Context, rawURL string) (string, error) {
	resp, err := c.Do(ctx, http.MethodGet, rawURL, "", false)
	if err != nil {
		return "", err
	}
	if !resp.Successful() {
		return "", fmt.Errorf("微信支付请求失败, 响应码=%d, 返回=%s", resp.StatusCode, resp.Body)
	}
	return resp.Body, nil
}

func (c *WxV3Client) PostJSON(ctx context.Context, rawURL, body string) (string, error) {
	resp, err := c.Do(ctx, http.MethodPost, rawURL, body, true)
	if err != nil {
		return "", err
	}
	if !resp.Successful() {
		return "", fmt.Errorf("微信支付请求失败, 响应码=%d, 返回=%s", resp.StatusCode, resp.Body)
	}
	return resp.Body, nil
}

func (c *WxV3Client) PostJSONForResponse(ctx context.Context, rawURL, body string) (WxHTTPResponse, error) {
	return c.Do(ctx, http.MethodPost, rawURL, body, true)
}

func (c *WxV3Client) Do(ctx context.Context, method, rawURL, body string, signed bool) (WxHTTPResponse, error) {
	var reader io.Reader
	if body != "" {
		reader = strings.NewReader(body)
	}
	req, err := http.NewRequestWithContext(ctx, method, rawURL, reader)
	if err != nil {
		return WxHTTPResponse{}, err
	}
	req.Header.Set("Accept", "application/json")
	if body != "" {
		req.Header.Set("Content-Type", "application/json")
	}
	if signed {
		if err := c.signRequest(req, body); err != nil {
			return WxHTTPResponse{}, err
		}
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return WxHTTPResponse{}, err
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	return WxHTTPResponse{StatusCode: resp.StatusCode, Body: string(b)}, nil
}

func (c *WxV3Client) signRequest(req *http.Request, body string) error {
	timestamp := fmt.Sprintf("%d", time.Now().Unix())
	nonce := randomString(32)
	canonicalURL := req.URL.EscapedPath()
	if req.URL.RawQuery != "" {
		canonicalURL += "?" + req.URL.RawQuery
	}
	message := req.Method + "\n" + canonicalURL + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n"
	sig, err := signSHA256RSA(c.privateKey, []byte(message))
	if err != nil {
		return err
	}
	auth := fmt.Sprintf(`WECHATPAY2-SHA256-RSA2048 mchid="%s",nonce_str="%s",timestamp="%s",serial_no="%s",signature="%s"`, c.cfg.MchID, nonce, timestamp, c.cfg.MchSerialNo, sig)
	req.Header.Set("Authorization", auth)
	return nil
}

func (c *WxV3Client) VerifyNotification(ctx context.Context, header http.Header, body []byte) error {
	serial := header.Get("Wechatpay-Serial")
	signature := header.Get("Wechatpay-Signature")
	nonce := header.Get("Wechatpay-Nonce")
	timestamp := header.Get("Wechatpay-Timestamp")
	if serial == "" || signature == "" || nonce == "" || timestamp == "" {
		return fmt.Errorf("微信通知验签头缺失")
	}
	sec, err := parseInt64(timestamp)
	if err != nil {
		return fmt.Errorf("微信通知时间戳非法")
	}
	if time.Since(time.Unix(sec, 0)).Abs() >= 5*time.Minute {
		return fmt.Errorf("微信通知已过期")
	}
	cert := c.platformCert(serial)
	if cert == nil {
		if err := c.FetchPlatformCertificates(ctx); err != nil {
			return err
		}
		cert = c.platformCert(serial)
	}
	if cert == nil {
		return fmt.Errorf("未找到微信平台证书 serial=%s", serial)
	}
	pub, ok := cert.PublicKey.(*rsa.PublicKey)
	if !ok {
		return fmt.Errorf("微信平台证书不是 RSA 公钥")
	}
	message := timestamp + "\n" + nonce + "\n" + string(body) + "\n"
	sig, err := base64.StdEncoding.DecodeString(signature)
	if err != nil {
		return err
	}
	h := sha256.Sum256([]byte(message))
	return rsa.VerifyPKCS1v15(pub, crypto.SHA256, h[:], sig)
}

func (c *WxV3Client) platformCert(serial string) *x509.Certificate {
	c.mu.RLock()
	defer c.mu.RUnlock()
	return c.certs[serial]
}

func (c *WxV3Client) FetchPlatformCertificates(ctx context.Context) error {
	rawURL := strings.TrimRight(c.cfg.Domain, "/") + constant.WxAPICertificates
	body, err := c.Get(ctx, rawURL)
	if err != nil {
		return fmt.Errorf("拉取微信平台证书失败: %w", err)
	}
	var raw map[string][]map[string]interface{}
	if err := json.Unmarshal([]byte(body), &raw); err != nil {
		return err
	}
	data := raw["data"]
	certs := map[string]*x509.Certificate{}
	for _, item := range data {
		serial, _ := item["serial_no"].(string)
		ec, _ := item["encrypt_certificate"].(map[string]interface{})
		ciphertext, _ := ec["ciphertext"].(string)
		nonce, _ := ec["nonce"].(string)
		associatedData, _ := ec["associated_data"].(string)
		plain, err := AESGCMDecrypt(c.cfg.ApiV3Key, associatedData, nonce, ciphertext)
		if err != nil {
			return err
		}
		block, _ := pem.Decode([]byte(plain))
		if block == nil {
			return fmt.Errorf("微信平台证书 PEM 解析失败")
		}
		cert, err := x509.ParseCertificate(block.Bytes)
		if err != nil {
			return err
		}
		certs[serial] = cert
	}
	c.mu.Lock()
	c.certs = certs
	c.mu.Unlock()
	return nil
}

func (c *WxV3Client) DecryptResource(bodyMap map[string]interface{}) (string, error) {
	resource, ok := bodyMap["resource"].(map[string]interface{})
	if !ok {
		return "", fmt.Errorf("微信通知resource不能为空")
	}
	ciphertext, _ := resource["ciphertext"].(string)
	nonce, _ := resource["nonce"].(string)
	associatedData, _ := resource["associated_data"].(string)
	return AESGCMDecrypt(c.cfg.ApiV3Key, associatedData, nonce, ciphertext)
}

func AESGCMDecrypt(key, associatedData, nonce, ciphertext string) (string, error) {
	block, err := aes.NewCipher([]byte(key))
	if err != nil {
		return "", err
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return "", err
	}
	cipherData, err := base64.StdEncoding.DecodeString(ciphertext)
	if err != nil {
		return "", err
	}
	plain, err := gcm.Open(nil, []byte(nonce), cipherData, []byte(associatedData))
	if err != nil {
		return "", err
	}
	return string(plain), nil
}

func LoadRSAPrivateKeyFromFile(path string) (*rsa.PrivateKey, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	return ParseRSAPrivateKey(b)
}

func ParseRSAPrivateKey(b []byte) (*rsa.PrivateKey, error) {
	block, _ := pem.Decode(b)
	if block == nil {
		return nil, fmt.Errorf("私钥 PEM 解析失败")
	}
	if key, err := x509.ParsePKCS8PrivateKey(block.Bytes); err == nil {
		if pk, ok := key.(*rsa.PrivateKey); ok {
			return pk, nil
		}
	}
	return x509.ParsePKCS1PrivateKey(block.Bytes)
}

func signSHA256RSA(privateKey *rsa.PrivateKey, data []byte) (string, error) {
	h := sha256.Sum256(data)
	sig, err := rsa.SignPKCS1v15(rand.Reader, privateKey, crypto.SHA256, h[:])
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(sig), nil
}

func randomString(n int) string {
	const letters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	buf := make([]byte, n)
	max := big.NewInt(int64(len(letters)))
	for i := range buf {
		x, _ := rand.Int(rand.Reader, max)
		buf[i] = letters[x.Int64()]
	}
	return string(buf)
}

func parseInt64(s string) (int64, error) {
	var n int64
	_, err := fmt.Sscanf(s, "%d", &n)
	return n, err
}

func BuildURL(domain, path string, query url.Values) string {
	u := strings.TrimRight(domain, "/") + path
	if len(query) > 0 {
		u += "?" + query.Encode()
	}
	return u
}

func DecodeJSONMap(s string) (map[string]interface{}, error) {
	var m map[string]interface{}
	dec := json.NewDecoder(bytes.NewBufferString(s))
	dec.UseNumber()
	if err := dec.Decode(&m); err != nil {
		return nil, err
	}
	return m, nil
}

func (c *WxV3Client) SignJSAPIPackage(appID, prepayID string, timestamp int64, nonce string) (string, error) {
	message := fmt.Sprintf("%s\n%d\n%s\n%s\n", appID, timestamp, nonce, prepayID)
	return signSHA256RSA(c.privateKey, []byte(message))
}
