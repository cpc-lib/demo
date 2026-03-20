package cc.ivera.model.pojo;

/**
 *  word数据表单
 */
public class DataForm {

    /** 公司名称 */
    private String companyName;

    /** 时间-年 */
    private String year;

    /** 时间-月 */
    private String month;

    /** 时间-日 */
    private String day;

    /** 会议主题 */
    private String meetingTheme;

    /** 会议类型 1、股东会 2、董事会  3、合伙人会 4、其他 */
    private String meetingType;

    /** 是否通过  1、同意  2、续议  3、拒绝 */
    private String passFlag;

    /** 汇总意见 所有投票人的意见汇总 */
    private String allOpinion;

    /** 签名 */
    private String sign;

    /** 签名日期  yyyy-MM-dd */
    private String signDate;


    public DataForm() {
    }

    public DataForm(String companyName, String year, String month, String day, String meetingTheme, String meetingType, String passFlag, String allOpinion, String sign, String signDate) {
        this.companyName = companyName;
        this.year = year;
        this.month = month;
        this.day = day;
        this.meetingTheme = meetingTheme;
        this.meetingType = meetingType;
        this.passFlag = passFlag;
        this.allOpinion = allOpinion;
        this.sign = sign;
        this.signDate = signDate;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getMeetingTheme() {
        return meetingTheme;
    }

    public void setMeetingTheme(String meetingTheme) {
        this.meetingTheme = meetingTheme;
    }

    public String getMeetingType() {
        return meetingType;
    }

    public void setMeetingType(String meetingType) {
        this.meetingType = meetingType;
    }

    public String getPassFlag() {
        return passFlag;
    }

    public void setPassFlag(String passFlag) {
        this.passFlag = passFlag;
    }

    public String getAllOpinion() {
        return allOpinion;
    }

    public void setAllOpinion(String allOpinion) {
        this.allOpinion = allOpinion;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getSignDate() {
        return signDate;
    }

    public void setSignDate(String signDate) {
        this.signDate = signDate;
    }
}
