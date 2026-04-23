package cc.ivera.policy;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.exception.RenderException;
import com.deepoove.poi.policy.RenderPolicy;
import com.deepoove.poi.render.compute.EnvModel;
import com.deepoove.poi.render.compute.RenderDataCompute;
import com.deepoove.poi.render.processor.DocumentProcessor;
import com.deepoove.poi.render.processor.EnvIterator;
import com.deepoove.poi.resolver.TemplateResolver;
import com.deepoove.poi.template.ElementTemplate;
import com.deepoove.poi.template.MetaTemplate;
import com.deepoove.poi.template.run.RunTemplate;
import com.deepoove.poi.util.ReflectionUtils;
import com.deepoove.poi.util.TableTools;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.util.Iterator;
import java.util.List;

public class MergeRowsRenderPolicy implements RenderPolicy {
 
    private final String prefix;
    private final String suffix;
    private final boolean onSameLine;
 
    public MergeRowsRenderPolicy() {
        this(false);
    }
 
    public MergeRowsRenderPolicy(boolean onSameLine) {
        this("[", "]", onSameLine);
    }
 
    public MergeRowsRenderPolicy(String prefix, String suffix) {
        this(prefix, suffix, false);
    }
 
    public MergeRowsRenderPolicy(String prefix, String suffix, boolean onSameLine) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.onSameLine = onSameLine;
    }
 
    @Override
    public void render(ElementTemplate eleTemplate, Object data, XWPFTemplate template) {
        RunTemplate runTemplate = (RunTemplate) eleTemplate;
        XWPFRun run = runTemplate.getRun();
        try {
            if (!TableTools.isInsideTable(run)) {
                throw new IllegalStateException(
                        "The template tag " + runTemplate.getSource() + " must be inside a table");
            }
            XWPFTableCell tagCell = (XWPFTableCell) ((XWPFParagraph) run.getParent()).getBody();
            XWPFTable table = tagCell.getTableRow().getTable();
            run.setText("", 0);
 
            int templateRowIndex = getTemplateRowIndex(tagCell);
            if (data instanceof Iterable) {
                Iterator<?> iterator = ((Iterable<?>) data).iterator();
                XWPFTableRow templateRow = table.getRow(templateRowIndex);
                int insertPosition = templateRowIndex;
 
                TemplateResolver resolver = new TemplateResolver(template.getConfig().copy(prefix, suffix));
                boolean firstFlag = true;
                int index = 0;
                boolean hasNext = iterator.hasNext();
                while (hasNext) {
                    Object root = iterator.next();
                    hasNext = iterator.hasNext();
 
                    insertPosition = templateRowIndex++;
                    table.insertNewTableRow(insertPosition);
                    setTableRow(table, templateRow, insertPosition);
 
                    // double set row
                    XmlCursor newCursor = templateRow.getCtRow().newCursor();
                    newCursor.toPrevSibling();
                    XmlObject object = newCursor.getObject();
                    XWPFTableRow nextRow = new XWPFTableRow((CTRow) object, table);
                    if (!firstFlag) {
                        // update VMerge cells for non-first row
                        List<XWPFTableCell> tableCells = nextRow.getTableCells();
                        for (XWPFTableCell cell : tableCells) {
                            CTTcPr tcPr = TableTools.getTcPr(cell);
                            CTVMerge vMerge = tcPr.getVMerge();
                            if (null == vMerge) {continue;}
                            if (STMerge.RESTART == vMerge.getVal()) {
                                vMerge.setVal(STMerge.CONTINUE);
                            }
                        }
                    } else {
                        firstFlag = false;
                    }
                    setTableRow(table, nextRow, insertPosition);
 
                    RenderDataCompute dataCompute = template.getConfig()
                            .getRenderDataComputeFactory()
                            .newCompute(EnvModel.of(root, EnvIterator.makeEnv(index++, hasNext)));
                    List<XWPFTableCell> cells = nextRow.getTableCells();
                    cells.forEach(cell -> {
                        List<MetaTemplate> templates = resolver.resolveBodyElements(cell.getBodyElements());
                        new DocumentProcessor(template, resolver, dataCompute).process(templates);
                    });
                }
                // 处理连续数据相同行合并
                int currPos = getTemplateRowIndex(tagCell);
                XWPFTableRow prevRow = table.getRow(currPos);
                while (currPos <= insertPosition) {
                    currPos++;
                    XWPFTableRow nextRow = table.getRow(currPos);
                    List<XWPFTableCell> cells = nextRow.getTableCells();
                    int currCellPos = 0;
                    while (currCellPos < cells.size()) {
                        CTTcPr tcPrPrev = TableTools.getTcPr(prevRow.getCell(currCellPos));
                        CTVMerge vMergePrev = tcPrPrev.getVMerge();
                        if (null == vMergePrev) {
                            vMergePrev = tcPrPrev.addNewVMerge();
                            vMergePrev.setVal(STMerge.RESTART);
                        }
                        CTTcPr tcPrNext = TableTools.getTcPr(nextRow.getCell(currCellPos));
                        CTVMerge vMergeNext = tcPrNext.getVMerge();
                        if (null == vMergeNext) {
                            vMergeNext = tcPrNext.addNewVMerge();
                            vMergeNext.setVal(STMerge.RESTART);
                        }
                        // 判断上下两行的字符串值是否一样，一样就合并
                        if (StringUtils.equalsIgnoreCase(
                                prevRow.getCell(currCellPos).getText(),
                                nextRow.getCell(currCellPos).getText())) {
                            vMergeNext.setVal(STMerge.CONTINUE);
                        }
                        currCellPos++;
                    }
                    prevRow = nextRow;
                }
 
            }
 
            table.removeRow(templateRowIndex);
            afterloop(table, data);
        } catch (Exception e) {
            throw new RenderException("HackLoopTable for " + eleTemplate + "error: " + e.getMessage(), e);
        }
    }
 
    private int getTemplateRowIndex(XWPFTableCell tagCell) {
        XWPFTableRow tagRow = tagCell.getTableRow();
        return onSameLine ? getRowIndex(tagRow) : (getRowIndex(tagRow) + 1);
    }
 
    protected void afterloop(XWPFTable table, Object data) {
    }
 
    @SuppressWarnings("unchecked")
    private void setTableRow(XWPFTable table, XWPFTableRow templateRow, int pos) {
        List<XWPFTableRow> rows = (List<XWPFTableRow>) ReflectionUtils.getValue("tableRows", table);
        rows.set(pos, templateRow);
        table.getCTTbl().setTrArray(pos, templateRow.getCtRow());
    }
 
    private int getRowIndex(XWPFTableRow row) {
        List<XWPFTableRow> rows = row.getTable().getRows();
        return rows.indexOf(row);
    }
}