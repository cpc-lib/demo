# spring-security-dual-token-demo

#### 介绍
接口文档 ： swagger。在线 API 文档地址：运行后，地址： http://localhost:8999/doc.html （推荐👍）

双token机制，避免频繁重新登录，用户名admin，密码12345，先登录，前端拿到两个token存起来，然后拿accessToken作为请求头调用业务接口，一旦此token过期响应401时，refreshToekn作为请求头调用刷新token接口，然后把两个token再次存起来，再accessToken作为请求头重新调用业务接口，此后端只是一个简单实现，没有用数据库和缓存保证安全

