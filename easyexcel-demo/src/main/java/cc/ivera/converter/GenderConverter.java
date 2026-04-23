package cc.ivera.converter;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;

import java.util.Objects;

/**
 * 类描述：性别字段的数据转换器
 *
 * @Author wang_qz
 * @Date 2021/8/15 19:16
 * @Version 1.0
 */
public class GenderConverter implements Converter<String> {

    private static final String MALE = "男";
    private static final String FEMALE = "女";
    private static final String NONE = "未知";

    // Java数据类型 integer
    @Override
    public Class supportJavaTypeKey() {
        return String.class;
    }

    // Excel文件中单元格的数据类型  string
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    // 读取Excel文件时将string转换为integer
    @Override
    public String convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        String value = cellData.getStringValue();
        if (Objects.equals(FEMALE, value)) {
            return "0"; // 0-女
        } else if (Objects.equals(MALE, value)) {
            return "1"; // 1-男
        }
        return "2"; // 2-未知
    }

    // 写入Excel文件时将integer转换为string
    @Override
    public WriteCellData<?> convertToExcelData(String value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        if ("1".equals(value)) {
            return new WriteCellData<>(MALE);
        } else if ("0".equals(value)) {
            return new WriteCellData<>(FEMALE);
        }
        return new WriteCellData<>(NONE); // 不男不女
    }
}
