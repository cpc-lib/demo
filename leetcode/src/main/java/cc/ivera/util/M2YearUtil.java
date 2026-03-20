package cc.ivera.util;

public class M2YearUtil {
    public static int calculateYear(int season) {
        int year = 1; // 初始年份
        while (season > 12) {
            season -= 12; // 每次减去4个季节
            year++;
        }
        return year;
    }
}
