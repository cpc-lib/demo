package cc.ivera.service.impl.reconciliation;

import cc.ivera.dto.ChannelBillImportRequest;
import cc.ivera.entity.ChannelBill;
import cc.ivera.entity.Reconciliation;
import cc.ivera.exception.BizException;
import cc.ivera.mapper.ChannelBillMapper;
import cc.ivera.mapper.ReconciliationMapper;
import cc.ivera.service.AliPayService;
import cc.ivera.service.reconciliation.ChannelBillService;
import cc.ivera.service.wxpay.WxPayBillFacade;
import cc.ivera.util.HttpClientUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Slf4j
public class ChannelBillServiceImpl implements ChannelBillService {

    private static final ZoneId BILL_ZONE = ZoneId.of("Asia/Shanghai");

    private static final String CHANNEL_WXPAY = "WXPAY";
    private static final String CHANNEL_ALIPAY = "ALIPAY";
    private static final String BILL_TYPE_ALL = "ALL";
    private static final String SOURCE_AUTO_DOWNLOAD = "AUTO_DOWNLOAD";
    private static final String SOURCE_MANUAL_UPLOAD = "MANUAL_UPLOAD";
    private static final String STATUS_IMPORTED = "IMPORTED";

    private final ChannelBillMapper channelBillMapper;
    private final ReconciliationMapper reconciliationMapper;
    private final WxPayBillFacade wxPayBillFacade;
    private final AliPayService aliPayService;
    private final WxBillParser wxBillParser;
    private final AliPayBillParser aliPayBillParser;

    public ChannelBillServiceImpl(
            ChannelBillMapper channelBillMapper,
            ReconciliationMapper reconciliationMapper,
            WxPayBillFacade wxPayBillFacade,
            AliPayService aliPayService,
            WxBillParser wxBillParser,
            AliPayBillParser aliPayBillParser
    ) {
        this.channelBillMapper = channelBillMapper;
        this.reconciliationMapper = reconciliationMapper;
        this.wxPayBillFacade = wxPayBillFacade;
        this.aliPayService = aliPayService;
        this.wxBillParser = wxBillParser;
        this.aliPayBillParser = aliPayBillParser;
    }

    @Override
    public ChannelBill importFromChannel(ChannelBillImportRequest request) {
        LocalDate billDate = parseBillDate(request.getBillDate());
        String channelCode = request.getChannelCode();
        Long paymentAppId = request.getPaymentAppId();
        String billType = StringUtils.hasText(request.getBillType()) ? request.getBillType() : BILL_TYPE_ALL;
        boolean force = Boolean.TRUE.equals(request.getForce());

        validateChannelCode(channelCode);
        validateBillDate(billDate);

        String billContent = downloadFromChannel(billDate, channelCode, paymentAppId, billType);
        log.info("渠道账单下载完成，billDate={}, channelCode={}", billDate, channelCode);

        ChannelBill bill = saveImportedBill(billContent, billDate, channelCode, paymentAppId, billType,
                SOURCE_AUTO_DOWNLOAD, null, force);
        log.info("渠道账单导入完成，billId={}, recordCount={}", bill.getId(), bill.getRecordCount());
        return bill;
    }

    @Override
    public ChannelBill uploadBill(MultipartFile file, String billDateStr, String channelCode,
                                  String billType, Boolean force) {
        if (file == null || file.isEmpty()) {
            throw new BizException("账单文件不能为空");
        }
        // 支付宝账单为 GZIP 压缩格式，当前手动上传仅支持微信交易账单文本或 XLSX
        if (!CHANNEL_WXPAY.equals(channelCode)) {
            throw new BizException("手动上传当前仅支持微信交易账单（CSV/TXT/XLSX），支付宝账单请使用自动拉取");
        }

        LocalDate billDate = parseBillDate(billDateStr);
        Long paymentAppId = null;
        String type = StringUtils.hasText(billType) ? billType : BILL_TYPE_ALL;

        validateChannelCode(channelCode);
        validateBillDate(billDate);

        String billContent;
        try {
            billContent = wxBillParser.normalize(file.getBytes(), file.getOriginalFilename());
        } catch (Exception e) {
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            throw new BizException("读取账单文件失败", e);
        }
        if (!StringUtils.hasText(billContent)) {
            throw new BizException("账单文件内容为空");
        }
        ChannelBill bill = saveImportedBill(billContent, billDate, channelCode, paymentAppId, type,
                SOURCE_MANUAL_UPLOAD, file.getOriginalFilename(), Boolean.TRUE.equals(force));
        log.info("手动上传账单导入完成，billId={}, recordCount={}, fileName={}",
                bill.getId(), bill.getRecordCount(), file.getOriginalFilename());
        return bill;
    }

    @Override
    public IPage<ChannelBill> listBills(int pageNum, int pageSize,
                                        LocalDate billDateStart, LocalDate billDateEnd,
                                        String channelCode, String billSource) {
        Page<ChannelBill> page = new Page<>(pageNum, pageSize);
        return channelBillMapper.selectPageByConditions(page, billDateStart, billDateEnd, channelCode, billSource);
    }

    @Override
    public ChannelBill getBillById(Long id) {
        ChannelBill bill = channelBillMapper.selectById(id);
        if (bill == null) {
            throw new BizException("渠道账单不存在");
        }
        return bill;
    }

    @Override
    public IPage<ChannelBillRecord> listRecords(Long billId, int pageNum, int pageSize) {
        ChannelBill bill = getBillById(billId);
        List<ChannelBillRecord> records = parseBill(bill.getBillContent(), bill.getChannelCode());

        Page<ChannelBillRecord> page = new Page<>(pageNum, pageSize);
        page.setTotal(records.size());
        int fromIndex = Math.min((pageNum - 1) * pageSize, records.size());
        int toIndex = Math.min(fromIndex + pageSize, records.size());
        page.setRecords(records.subList(fromIndex, toIndex));
        return page;
    }

    @Override
    public void deleteBill(Long id) {
        getBillById(id);
        Integer referenced = reconciliationMapper.selectCount(
                new QueryWrapper<Reconciliation>().eq("bill_id", id));
        if (referenced != null && referenced > 0) {
            throw new BizException("该账单已被对账记录引用，无法删除");
        }
        channelBillMapper.deleteById(id);
        log.info("渠道账单已删除，billId={}", id);
    }

    /**
     * 导入落库：解析校验 -> 计算统计 -> 唯一键存在时幂等返回（force 时覆盖更新）
     */
    private ChannelBill saveImportedBill(String billContent, LocalDate billDate, String channelCode,
                                         Long paymentAppId, String billType, String billSource,
                                         String fileName, boolean force) {
        ChannelBill existing = channelBillMapper.selectByUniqueKey(billDate, channelCode, paymentAppId, billType);
        if (existing != null && !force) {
            log.info("渠道账单已导入，直接返回，billId={}, billDate={}, channelCode={}",
                    existing.getId(), billDate, channelCode);
            return existing;
        }

        List<ChannelBillRecord> records = parseBill(billContent, channelCode);
        if (records.isEmpty()) {
            throw new BizException("账单解析结果为空，请确认账单文件内容");
        }

        long totalAmount = 0;
        for (ChannelBillRecord record : records) {
            if (record.getAmount() != null) {
                totalAmount += record.getAmount();
            }
        }

        ChannelBill bill;
        if (existing != null) {
            bill = existing;
        } else {
            bill = new ChannelBill();
            bill.setBillDate(billDate);
            bill.setChannelCode(channelCode);
            bill.setPaymentAppId(paymentAppId);
            bill.setBillType(billType);
        }
        bill.setBillSource(billSource);
        bill.setStatus(STATUS_IMPORTED);
        bill.setRecordCount(records.size());
        bill.setTotalAmount(totalAmount);
        bill.setBillHash(sha256(billContent));
        bill.setFileName(fileName);
        bill.setBillContent(billContent);
        bill.setImportTime(new java.util.Date());

        if (existing != null) {
            channelBillMapper.updateById(bill);
        } else {
            channelBillMapper.insert(bill);
        }
        return channelBillMapper.selectById(bill.getId());
    }

    private String downloadFromChannel(LocalDate billDate, String channelCode, Long paymentAppId, String billType) {
        String billDateStr = billDate.toString();

        if (CHANNEL_WXPAY.equals(channelCode)) {
            // WxPayBillService.validateBillDate 会校验 T+1 与昨日账单 10:00 后生成的限制
            return wxPayBillFacade.downloadBill(
                    paymentAppId, billDateStr, "tradebill", billType, null, null);
        }

        String downloadUrl = aliPayService.queryBill(billDateStr, "trade");
        try {
            HttpClientUtils httpClient = new HttpClientUtils(downloadUrl);
            if (downloadUrl.startsWith("https://")) {
                httpClient.setHttps(true);
            }
            httpClient.get();
            String content = httpClient.getContent();
            if (content == null || content.trim().isEmpty()) {
                throw new BizException("支付宝账单下载内容为空");
            }
            return content;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载支付宝账单失败，url={}", downloadUrl, e);
            throw new BizException("下载支付宝账单失败", e);
        }
    }

    private List<ChannelBillRecord> parseBill(String billContent, String channelCode) {
        if (CHANNEL_WXPAY.equals(channelCode)) {
            return wxBillParser.parse(billContent);
        }
        return aliPayBillParser.parse(billContent);
    }

    private void validateChannelCode(String channelCode) {
        if (!CHANNEL_WXPAY.equals(channelCode) && !CHANNEL_ALIPAY.equals(channelCode)) {
            throw new BizException("渠道编码只支持WXPAY或ALIPAY");
        }
    }

    /**
     * 渠道账单 T+1 出账：仅支持导入历史日期账单
     */
    private void validateBillDate(LocalDate billDate) {
        LocalDate today = LocalDate.now(BILL_ZONE);
        if (!billDate.isBefore(today)) {
            throw new BizException("渠道账单为T+1出账，不支持导入当日或未来日期账单，请使用"
                    + today.minusDays(1) + "或更早日期");
        }
    }

    private LocalDate parseBillDate(String billDateStr) {
        try {
            return LocalDate.parse(billDateStr);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BizException("账单日期格式必须为yyyy-MM-dd");
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
