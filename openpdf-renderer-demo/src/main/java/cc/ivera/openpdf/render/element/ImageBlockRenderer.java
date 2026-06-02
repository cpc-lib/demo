package cc.ivera.openpdf.render.element;

import cc.ivera.openpdf.model.block.ImageBlock;
import cc.ivera.openpdf.support.placeholder.RenderContext;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Image;

/**
 * 图片块渲染器。
 */
public class ImageBlockRenderer implements PdfElementRenderer<ImageBlock> {

    @Override
    public Class<ImageBlock> supportType() {
        return ImageBlock.class;
    }

    @Override
    public void render(Document document, ImageBlock block, RenderContext context) throws Exception {
        String imagePath = context.resolve(block.getImagePath());
        Image image = Image.getInstance(imagePath);

        if (block.getWidth() != null && block.getHeight() != null) {
            image.scaleToFit(block.getWidth(), block.getHeight());
        }

        if (block.isCenter()) {
            image.setAlignment(Element.ALIGN_CENTER);
        }
        image.setSpacingAfter(block.getSpacingAfter());
        document.add(image);
    }
}
