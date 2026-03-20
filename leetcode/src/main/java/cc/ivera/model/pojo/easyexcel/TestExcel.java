package cc.ivera.model.pojo.easyexcel;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TestExcel {

    private String contractNo;

    private String address;

    private String dateTime;

    private List<Item> itemList;

    @Data
    public static class Item {

        private String name;

        private BigDecimal price;
    }
}