# 前端说明

这是一个最简的纯静态前端示例：

- 直接用浏览器打开 `index.html`
- 默认把后端地址指向 `http://localhost:8080`
- 上传流程：
  1. 调用 `/api/chunk/init`
  2. 多次调用 `/api/chunk/upload`
  3. 调用 `/api/chunk/merge` 得到一个 **5 分钟后过期** 的 MinIO 临时下载地址
