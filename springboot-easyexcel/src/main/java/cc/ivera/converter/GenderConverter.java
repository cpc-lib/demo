package cc.ivera.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

import java.util.Objects;

/**
 * 类描述：性别字段的数据转换器
 *
 * @Author wang_qz
 * @Date 2021/8/15 19:16
 * @Version 1.0
 */
public class GenderConverter implements Converter<Integer> {

    private static final String MALE = "男";
    private static final String FEMALE = "女";
    private static final String NONE = "未知";

    // Java数据类型 integer
    @Override
    public Class supportJavaTypeKey() {
        return Integer.class;
    }

    // Excel文件中单元格的数据类型  string
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    // 读取Excel文件时将string转换为integer
    @Override
    public Integer convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration)
            throws Exception {
        String value = cellData.getStringValue();
        if (Objects.equals(FEMALE, value)) {
            return 0; // 0-女
        } else if (Objects.equals(MALE, value)) {
            return 1; // 1-男
        }
        return 2; // 2-未知
    }

    // 写入Excel文件时将integer转换为string
    @Override
    public WriteCellData<?> convertToExcelData(Integer value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        if (value == 1) {
            return new WriteCellData<>(MALE);
        } else if (value == 0) {
            return new WriteCellData<>(FEMALE);
        }
        return new WriteCellData<>(NONE); // 不男不女
    }
}
