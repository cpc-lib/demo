package cc.ivera.openpdf.model.block;

import java.util.ArrayList;
import java.util.List;

/**
 * 表格块。
 */
public class TableBlock implements PdfElement {

    private String title;
    private final List<String> headers = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();
    private float[] columnWidths;
    private float fontSize = 11F;
    private float spacingAfter = 10F;

    public String getTitle() {
        return title;
    }

    public TableBlock setTitle(String title) {
        this.title = title;
        return this;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public TableBlock addHeader(String header) {
        this.headers.add(header);
        return this;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public TableBlock addRow(List<String> row) {
        this.rows.add(row);
        return this;
    }

    public float[] getColumnWidths() {
        return columnWidths;
    }

    public TableBlock setColumnWidths(float[] columnWidths) {
        this.columnWidths = columnWidths;
        return this;
    }

    public float getFontSize() {
        return fontSize;
    }

    public TableBlock setFontSize(float fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public float getSpacingAfter() {
        return spacingAfter;
    }

    public TableBlock setSpacingAfter(float spacingAfter) {
        this.spacingAfter = spacingAfter;
        return this;
    }
}
