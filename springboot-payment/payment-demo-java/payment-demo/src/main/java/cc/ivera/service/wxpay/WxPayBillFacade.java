package cc.ivera.service.wxpay;

public interface WxPayBillFacade {

    String queryBill(String billDate, String type, String billType, String accountType, String tarType);

    String downloadBill(String billDate, String type, String billType, String accountType, String tarType);

    String downloadBill(Long paymentAppId, String billDate, String type,
                        String billType, String accountType, String tarType);
}
