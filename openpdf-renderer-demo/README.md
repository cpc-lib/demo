# OpenPDF Renderer Demo

一个基于 **OpenPDF** 的 PDF 渲染案例，支持：

- 简单文字渲染
- 占位符文字渲染：`${key}`
- 图片渲染
- 列表渲染
- 表格渲染
- PDF 加密
- PDF 解密
- 机密水印（CONFIDENTIAL）

## 1. 技术栈

- Java 21
- Maven
- OpenPDF 3.0.3
- Bouncy Castle 1.83

## 2. 本次结构优化点

相比上一版，这一版重点做了这些收敛：

1. **渲染编排与 Writer 配置分离**  
   `OpenPdfRenderService` 只保留流程编排，水印与加密统一下沉到 `PdfWriterConfigurator`。

2. **元素渲染查找改为注册表模式**  
   不再在服务里硬编码 `if / else` 或遍历匹配逻辑，而是交给 `PdfElementRendererRegistry`。

3. **请求校验单独下沉**  
   `PdfRenderRequestValidator` 负责校验，避免服务类职责过重。

4. **字体、占位符、文件输出能力分包管理**  
   `support/font`、`support/placeholder`、`support/io` 各自负责一类能力。

5. **示例构建与运行入口分离**  
   示例请求由 `SampleRequestFactory` 构建，`OpenPdfRenderDemo` 只负责启动演示。

## 3. 工程结构

```text
src/main/java/cc/ivera/openpdf
├── demo
│   ├── OpenPdfRenderDemo.java
│   └── SampleRequestFactory.java
├── model
│   ├── block
│   │   ├── BulletListBlock.java
│   │   ├── ImageBlock.java
│   │   ├── PdfElement.java
│   │   ├── TableBlock.java
│   │   └── TextBlock.java
│   └── request
│       ├── EncryptionOptions.java
│       └── PdfRenderRequest.java
├── render
│   ├── element
│   │   ├── BulletListBlockRenderer.java
│   │   ├── ImageBlockRenderer.java
│   │   ├── PdfElementRenderer.java
│   │   ├── TableBlockRenderer.java
│   │   └── TextBlockRenderer.java
│   ├── registry
│   │   └── PdfElementRendererRegistry.java
│   └── watermark
│       └── ConfidentialWatermarkPageEvent.java
├── service
│   ├── PdfRenderService.java
│   ├── PdfSecurityService.java
│   └── impl
│       ├── OpenPdfRenderService.java
│       ├── OpenPdfSecurityService.java
│       └── PdfWriterConfigurator.java
├── support
│   ├── exception
│   │   └── PdfRenderException.java
│   ├── font
│   │   ├── FontResolver.java
│   │   └── FontSupport.java
│   ├── io
│   │   └── FileSupport.java
│   └── placeholder
│       ├── PlaceholderResolver.java
│       └── RenderContext.java
└── validator
    └── PdfRenderRequestValidator.java
```

## 4. 运行方式

### 4.1 直接运行 Demo

```bash
mvn clean compile exec:java
```

运行后会在下面目录生成文件：

```text
target/generated-pdf/
├── business-report-encrypted.pdf
└── business-report-decrypted.pdf
```

## 5. 中文说明

本示例默认不打包字体文件。

如果你需要在 PDF 中稳定渲染中文，请把系统中的中文字体路径传给：

```java
request.setFontPath("你的中文字体文件路径");
```

例如常见的 ttf / otf 文件。

## 6. 核心扩展建议

如果你后面还要继续扩展，建议优先增加这些能力：

- 页眉 / 页脚渲染器
- 二维码 / 条形码块
- 固定坐标文本块
- 多列表格块
- 图片下载器（支持 URL 图片）
- Spring Boot 下载接口封装

## 7. 说明

这里把“机密 PDF”按业务中最常见的形式实现成了：

1. **密码加密**
2. **机密水印**
3. **解密输出示例**

这样既能展示 PDF 安全控制，也方便你在业务里直接改造成合同、报表、回单等场景。
