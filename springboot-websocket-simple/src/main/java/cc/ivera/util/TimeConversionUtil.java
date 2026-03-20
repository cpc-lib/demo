package cc.ivera.util;

import org.springframework.util.Assert;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeConversionUtil {
    
    //传入long时间戳 ，转换成格式化的String类型时间
    public  String TimeOfLongToStr(long time) {
        Assert.notNull(time, "time is null");
        DateTimeFormatter ftf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return ftf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));

    }

    //传入String类型格式化时间，转换成Long类型的时间戳

    public  long TimeOfStrToLong(String strTime){
        Assert.notNull(strTime, "time is null");
        DateTimeFormatter ftf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime parse = LocalDateTime.parse(strTime, ftf);
        return LocalDateTime.from(parse).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    }


    //通过LocalDateTime获取当前格式化时间

    public  static String getTimeStrNow(){
        //DateTimeFormatter替换了SimpleDateFormat, 因为线程安全问题。
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    //通过LocalDateTime获取当前格式化时间

    public  static  String getTimeStrNow1(){
        //DateTimeFormatter替换了SimpleDateFormat, 因为线程安全问题。
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    //获取当前时间戳方法一
    public long getTimeLongOfClock(){

        return Clock.systemDefaultZone().millis();
    }

    //获取当前时间戳方法二
    public long getTimeLongOfCalendar(){
        Calendar calendar = Calendar.getInstance();

        return calendar.getTimeInMillis();
    }

    //获取当前时间戳方法三
    public long getTimeLongOfSystem(){

        return System.currentTimeMillis();
    }

    //获取当前时间戳方法四
    public static long getTimeLongOfDate(){

        Date date = new Date();

        return date.getTime();
    }

    //获取当前时间戳五 精确到毫秒
    public  static long getTimeLongOfInstant(){

        Instant now = Instant.now().plusMillis(TimeUnit.HOURS.toMillis(8));
        return now.toEpochMilli();
    }
    
    //获取当前时间戳六 精确到秒
    public static long getTimeLongOfInstantEpochSecond(){
        Instant now = Instant.now().plusMillis(TimeUnit.HOURS.toMillis(8));
        long longTimeNow=now.getEpochSecond();
        return longTimeNow;
    }

}

