package cc.ivera.service.reconciliation;

import cc.ivera.dto.ChannelBillImportRequest;
import cc.ivera.entity.ChannelBill;
import cc.ivera.service.impl.reconciliation.ChannelBillRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 渠道账单管理：账单导入（自动拉取/手动上传）是执行对账的前置条件
 */
public interface ChannelBillService {

    /** 自动拉取渠道账单并导入（调微信/支付宝 API） */
    ChannelBill importFromChannel(ChannelBillImportRequest request);

    /** 手动上传账单文件导入（当前支持微信交易账单 CSV/TXT/XLSX） */
    ChannelBill uploadBill(MultipartFile file, String billDate, String channelCode, String billType, Boolean force);

    IPage<ChannelBill> listBills(int pageNum, int pageSize,
                                 LocalDate billDateStart, LocalDate billDateEnd,
                                 String channelCode, String billSource);

    ChannelBill getBillById(Long id);

    /** 查看已导入账单解析后的记录（分页） */
    IPage<ChannelBillRecord> listRecords(Long billId, int pageNum, int pageSize);

    /** 删除账单（被对账记录引用时禁止删除） */
    void deleteBill(Long id);
}
