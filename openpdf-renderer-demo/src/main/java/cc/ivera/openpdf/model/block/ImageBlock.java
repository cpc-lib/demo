package cc.ivera.openpdf.model.block;

/**
 * 图片块。
 */
public class ImageBlock implements PdfElement {

    private String imagePath;
    private Float width;
    private Float height;
    private boolean center = true;
    private float spacingAfter = 10F;

    public String getImagePath() {
        return imagePath;
    }

    public ImageBlock setImagePath(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }

    public Float getWidth() {
        return width;
    }

    public ImageBlock setWidth(Float width) {
        this.width = width;
        return this;
    }

    public Float getHeight() {
        return height;
    }

    public ImageBlock setHeight(Float height) {
        this.height = height;
        return this;
    }

    public boolean isCenter() {
        return center;
    }

    public ImageBlock setCenter(boolean center) {
        this.center = center;
        return this;
    }

    public float getSpacingAfter() {
        return spacingAfter;
    }

    public ImageBlock setSpacingAfter(float spacingAfter) {
        this.spacingAfter = spacingAfter;
        return this;
    }
}
