package cc.ivera.openpdf.model.block;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表块。
 */
public class BulletListBlock implements PdfElement {

    private String title;
    private final List<String> items = new ArrayList<>();
    private float fontSize = 11F;
    private float spacingAfter = 10F;

    public String getTitle() {
        return title;
    }

    public BulletListBlock setTitle(String title) {
        this.title = title;
        return this;
    }

    public List<String> getItems() {
        return items;
    }

    public BulletListBlock addItem(String item) {
        this.items.add(item);
        return this;
    }

    public float getFontSize() {
        return fontSize;
    }

    public BulletListBlock setFontSize(float fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public float getSpacingAfter() {
        return spacingAfter;
    }

    public BulletListBlock setSpacingAfter(float spacingAfter) {
        this.spacingAfter = spacingAfter;
        return this;
    }
}
