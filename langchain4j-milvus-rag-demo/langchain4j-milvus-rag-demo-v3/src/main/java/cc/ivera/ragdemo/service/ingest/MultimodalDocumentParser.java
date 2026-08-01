package cc.ivera.ragdemo.service.ingest;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.ContentType;
import cc.ivera.ragdemo.model.knowledge.ExtractedImageKnowledge;
import cc.ivera.ragdemo.model.knowledge.PageCoordinate;
import cc.ivera.ragdemo.model.knowledge.ParsedKnowledgeDocument;
import dev.langchain4j.data.document.Document;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MultimodalDocumentParser {

    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[(?<alt>[^]]*)]\\((?<src>[^)]+)\\)");
    private static final Pattern IMAGE_NUMBER = Pattern.compile("(图|Figure|Fig\\.)\\s*([0-9]+(?:[-.][0-9]+)*)", Pattern.CASE_INSENSITIVE);
    private static final int CONTEXT_CHARS = 500;

    private final TikaParser tikaParser;
    private final RagProperties properties;

    public ParsedKnowledgeDocument parse(InputStream in, String fileName, String documentId) throws IOException {
        byte[] bytes = in.readAllBytes();
        Document textDocument = tikaParser.parse(new ByteArrayInputStream(bytes));

        if (!properties.getMultimodalIngest().isEnabled()) {
            return new ParsedKnowledgeDocument(textDocument, List.of());
        }

        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        List<ExtractedImageKnowledge> images;
        if (lowerName.endsWith(".pdf")) {
            images = parsePdf(bytes, documentId, textDocument.text());
        } else if (lowerName.endsWith(".docx")) {
            images = parseDocx(bytes, documentId);
        } else if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            images = parseMarkdown(bytes, documentId);
        } else {
            images = List.of();
        }
        return new ParsedKnowledgeDocument(textDocument, images);
    }

    private List<ExtractedImageKnowledge> parsePdf(byte[] bytes, String documentId, String fullText) throws IOException {
        List<ExtractedImageKnowledge> images = new ArrayList<>();
        Path documentDir = documentAssetDir(documentId);
        Files.createDirectories(documentDir);

        try (PDDocument pdf = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int pageIndex = 0; pageIndex < pdf.getNumberOfPages(); pageIndex++) {
                int pageNo = pageIndex + 1;
                stripper.setStartPage(pageNo);
                stripper.setEndPage(pageNo);
                String pageText = normalize(stripper.getText(pdf));
                PdfImageCollector collector = new PdfImageCollector(pdf.getPage(pageIndex));
                collector.processPage(pdf.getPage(pageIndex));

                int imageIndex = 0;
                for (PdfImage image : collector.images()) {
                    String id = documentId + "-p" + pageNo + "-img" + imageIndex++;
                    Path assetPath = documentDir.resolve(id + ".png");
                    ImageIO.write(image.bufferedImage(), "png", assetPath.toFile());
                    ImageContext context = contextAroundCaption(pageText, fullText);
                    images.add(new ExtractedImageKnowledge(
                            id,
                            inferContentType(context.caption()),
                            assetPath,
                            imageUrl(documentId, assetPath),
                            pageNo,
                            image.coordinate(),
                            context.sectionTitle(),
                            context.caption(),
                            extractImageNumber(context.caption()),
                            context.previousText(),
                            context.nextText(),
                            null,
                            null
                    ));
                }
            }
        }
        return images;
    }

    private List<ExtractedImageKnowledge> parseDocx(byte[] bytes, String documentId) throws IOException {
        List<ExtractedImageKnowledge> images = new ArrayList<>();
        Path documentDir = documentAssetDir(documentId);
        Files.createDirectories(documentDir);

        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<String> paragraphs = docx.getBodyElements().stream()
                    .filter(e -> e instanceof XWPFParagraph)
                    .map(e -> normalize(((XWPFParagraph) e).getText()))
                    .filter(StringUtils::hasText)
                    .toList();
            List<XWPFPictureData> pictures = docx.getAllPictures();
            for (int i = 0; i < pictures.size(); i++) {
                XWPFPictureData picture = pictures.get(i);
                String id = documentId + "-docx-img" + i;
                String ext = normalizeExtension(picture.suggestFileExtension());
                Path assetPath = documentDir.resolve(id + "." + ext);
                Files.write(assetPath, picture.getData());
                ImageContext context = contextFromParagraphs(paragraphs, i);
                images.add(new ExtractedImageKnowledge(
                        id,
                        inferContentType(context.caption()),
                        assetPath,
                        imageUrl(documentId, assetPath),
                        null,
                        null,
                        context.sectionTitle(),
                        context.caption(),
                        extractImageNumber(context.caption()),
                        context.previousText(),
                        context.nextText(),
                        null,
                        null
                ));
            }
        }
        return images;
    }

    private List<ExtractedImageKnowledge> parseMarkdown(byte[] bytes, String documentId) throws IOException {
        String markdown = new String(bytes, StandardCharsets.UTF_8);
        Matcher matcher = MARKDOWN_IMAGE.matcher(markdown);
        List<ExtractedImageKnowledge> images = new ArrayList<>();
        int index = 0;
        while (matcher.find()) {
            String caption = normalize(matcher.group("alt"));
            String src = normalize(matcher.group("src"));
            String id = documentId + "-md-img" + index++;
            ImageContext context = contextAroundOffset(markdown, matcher.start(), caption);
            images.add(new ExtractedImageKnowledge(
                    id,
                    inferContentType(caption),
                    null,
                    src,
                    null,
                    null,
                    context.sectionTitle(),
                    StringUtils.hasText(caption) ? caption : src,
                    extractImageNumber(caption),
                    context.previousText(),
                    context.nextText(),
                    null,
                    null
            ));
        }
        return images;
    }

    private Path documentAssetDir(String documentId) {
        return Path.of(properties.getMultimodalIngest().getAssetDirectory(), documentId);
    }

    private String imageUrl(String documentId, Path assetPath) {
        String fileName = assetPath.getFileName().toString();
        return properties.getMultimodalIngest().getAssetUrlPrefix() + "/" + documentId + "/" + fileName;
    }

    private ImageContext contextAroundCaption(String pageText, String fullText) {
        List<String> lines = pageText.lines().map(MultimodalDocumentParser::normalize).filter(StringUtils::hasText).toList();
        String caption = lines.stream().filter(line -> IMAGE_NUMBER.matcher(line).find()).findFirst().orElse(null);
        String sectionTitle = latestSectionTitle(fullText, pageText);
        String previous = firstNonBlank(lines, 0, Math.max(0, lines.size() / 2));
        String next = firstNonBlank(lines, Math.max(0, lines.size() / 2), lines.size());
        return new ImageContext(sectionTitle, caption, previous, next);
    }

    private ImageContext contextFromParagraphs(List<String> paragraphs, int imageIndex) {
        int anchor = paragraphs.isEmpty() ? 0 : Math.min(paragraphs.size() - 1, imageIndex);
        String sectionTitle = latestSectionTitle(String.join("\n", paragraphs.subList(0, anchor + 1)), "");
        String caption = paragraphs.stream().filter(p -> IMAGE_NUMBER.matcher(p).find()).skip(imageIndex).findFirst().orElse(null);
        String previous = anchor > 0 ? paragraphs.get(anchor - 1) : null;
        String next = anchor + 1 < paragraphs.size() ? paragraphs.get(anchor + 1) : null;
        return new ImageContext(sectionTitle, caption, previous, next);
    }

    private ImageContext contextAroundOffset(String markdown, int offset, String fallbackCaption) {
        int start = Math.max(0, offset - CONTEXT_CHARS);
        int end = Math.min(markdown.length(), offset + CONTEXT_CHARS);
        String previous = normalize(markdown.substring(start, offset).replaceAll("(?m)^#+\\s*", ""));
        String next = normalize(markdown.substring(offset, end).replaceAll(MARKDOWN_IMAGE.pattern(), ""));
        String sectionTitle = latestMarkdownHeading(markdown.substring(0, offset));
        return new ImageContext(sectionTitle, fallbackCaption, previous, next);
    }

    private static String latestSectionTitle(String textBefore, String pageText) {
        String merged = (textBefore == null ? "" : textBefore) + "\n" + (pageText == null ? "" : pageText);
        return merged.lines()
                .map(MultimodalDocumentParser::normalize)
                .filter(line -> line.matches("^([0-9]+(\\.[0-9]+)*\\s+)?\\S.{0,80}$"))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private static String latestMarkdownHeading(String textBefore) {
        return textBefore.lines()
                .map(MultimodalDocumentParser::normalize)
                .filter(line -> line.startsWith("#"))
                .map(line -> line.replaceFirst("^#+\\s*", ""))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private static String firstNonBlank(List<String> lines, int from, int to) {
        for (int i = from; i < to; i++) {
            if (StringUtils.hasText(lines.get(i))) {
                return lines.get(i);
            }
        }
        return null;
    }

    private static String extractImageNumber(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = IMAGE_NUMBER.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private static ContentType inferContentType(String caption) {
        if (caption == null) {
            return ContentType.IMAGE;
        }
        String lower = caption.toLowerCase(Locale.ROOT);
        if (lower.contains("flow") || caption.contains("流程")) {
            return ContentType.FLOWCHART;
        }
        if (lower.contains("architecture") || caption.contains("架构")) {
            return ContentType.ARCHITECTURE;
        }
        if (lower.contains("chart") || caption.contains("图表") || caption.contains("折线") || caption.contains("柱状")) {
            return ContentType.CHART;
        }
        return ContentType.IMAGE;
    }

    private static String normalize(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim();
    }

    private static String normalizeExtension(String ext) {
        if (!StringUtils.hasText(ext)) {
            return "png";
        }
        String normalized = ext.toLowerCase(Locale.ROOT).replace(".", "");
        return normalized.equals("jpeg") ? "jpg" : normalized;
    }

    public static String stableDocumentId(String fileName, byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(fileName == null ? new byte[0] : fileName.getBytes(StandardCharsets.UTF_8));
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest()).substring(0, 24);
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private record ImageContext(String sectionTitle, String caption, String previousText, String nextText) {
    }

    private record PdfImage(BufferedImage bufferedImage, PageCoordinate coordinate) {
    }

    private static class PdfImageCollector extends PDFGraphicsStreamEngine {
        private final PDPage page;
        private final List<PdfImage> images = new ArrayList<>();

        protected PdfImageCollector(PDPage page) {
            super(page);
            this.page = page;
        }

        List<PdfImage> images() {
            return images;
        }

        @Override
        public void drawImage(PDImage pdImage) throws IOException {
            Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
            float x = ctm.getTranslateX();
            float y = ctm.getTranslateY();
            float width = ctm.getScalingFactorX();
            float height = ctm.getScalingFactorY();
            float pageHeight = page.getMediaBox().getHeight();
            PageCoordinate coordinate = new PageCoordinate(
                    x,
                    pageHeight - y - height,
                    width,
                    height,
                    page.getMediaBox().getWidth(),
                    pageHeight
            );
            images.add(new PdfImage(toBufferedImage(pdImage), coordinate));
        }

        private BufferedImage toBufferedImage(PDImage pdImage) throws IOException {
            if (pdImage instanceof PDImageXObject imageXObject) {
                return imageXObject.getImage();
            }
            BufferedImage source = pdImage.getImage();
            BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = copy.createGraphics();
            g.drawImage(source, 0, 0, null);
            g.dispose();
            return copy;
        }

        @Override public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {}
        @Override public void clip(int windingRule) {}
        @Override public void moveTo(float x, float y) {}
        @Override public void lineTo(float x, float y) {}
        @Override public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {}
        @Override public Point2D getCurrentPoint() { return null; }
        @Override public void closePath() {}
        @Override public void endPath() {}
        @Override public void strokePath() {}
        @Override public void fillPath(int windingRule) {}
        @Override public void fillAndStrokePath(int windingRule) {}
        @Override public void shadingFill(COSName shadingName) {}
    }
}
