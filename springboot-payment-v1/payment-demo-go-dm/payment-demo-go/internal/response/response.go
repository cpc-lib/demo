package response

import "github.com/gin-gonic/gin"

type R struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data"`
}

func OK() R { return R{Code: 0, Message: "成功", Data: gin.H{}} }

func OKData(data interface{}) R { return R{Code: 0, Message: "成功", Data: data} }

func Error(message string) R { return R{Code: -1, Message: message, Data: gin.H{}} }

func WithData(r R, key string, value interface{}) R {
	m, ok := r.Data.(gin.H)
	if !ok || m == nil {
		m = gin.H{}
	}
	m[key] = value
	r.Data = m
	return r
}

func JSON(c *gin.Context, r R) { c.JSON(200, r) }
