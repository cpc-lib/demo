package cc.ivera.util;

public class S2YearUtil {
    public static int calculateYear(int season) {
        int year = 1; // 初始年份
        while (season > 4) {
            season -= 4; // 每次减去4个季节
            year++;
        }
        return year;
    }
}
