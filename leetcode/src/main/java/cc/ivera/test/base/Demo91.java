package cc.ivera.test.base;


import java.time.*;
import java.util.Date;

public class Demo91 {
    public static void main(String[] args) {
        // 假设你有一个 Date 对象（虽然在这个例子中我们其实不需要它，但为了说明问题我们仍然创建它）
        Date date = new Date(); // 这将获取当前的日期和时间

        // 但为了获取当前日期的0时0分0秒，我们其实不需要 Date 对象
        // 我们可以直接使用 LocalDateTime 和 LocalDate 来获取

        // 方法1：直接使用 LocalDateTime 和 LocalDate
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIDNIGHT); // 或者使用 .truncatedTo(ChronoUnit.DAYS)
        // 注意：LocalTime.MIDNIGHT 在 Java 9+ 中已被标记为过时(deprecated)，建议使用 LocalTime.MIN
        // 但在这里为了兼容性，我们仍然使用 MIDNIGHT
        // 或者更现代的做法：LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
        // 或者更简洁的做法：LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)

        // 方法2：（如果你真的有一个 Date 对象并想转换它）
        // 将 Date 转换为 Instant
        Instant instant = date.toInstant();
        // 将 Instant 转换为当前时区的 ZonedDateTime
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        // 从 ZonedDateTime 中提取出日期部分，并转换为 LocalDateTime（时间部分设为0时0分0秒）
        LocalDateTime fromDate = zonedDateTime.toLocalDate().atStartOfDay();
        // 输出结果
        System.out.println("使用 LocalDateTime 获取的当天0时0分0秒: " + startOfDay);
        System.out.println("从 Date 转换得到的当天0时0分0秒: " + fromDate);
    }
}
