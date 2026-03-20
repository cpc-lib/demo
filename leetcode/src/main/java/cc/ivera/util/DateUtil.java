package cc.ivera.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Calendar.DATE;


public class DateUtil extends org.apache.commons.lang3.time.DateUtils {

    public static String YYYY = "yyyy";

    public static String YYYY_W = "yyyy-w";

    public static String YYYY_MM = "yyyy-MM";

    public static String YYYY_MM_DD = "yyyy-MM-dd";

    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    public static String YYYY_MM_DDT_HH_MM_SS_SSSXXX = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public static ThreadLocal<Calendar> calendarLocal = ThreadLocal.withInitial(() -> Calendar.getInstance());
    public static String _YYYY_MM = "yyyy年MM月";
    public static String _YYYY_MM_DD = "yyyy年MM月dd日";
    private static String[] parsePatterns = {"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM", "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM", "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};

    /**
     * 获取当前Date型日期
     *
     * @return Date() 当前日期
     */
    public static Date getNowDate() {
        return new Date();
    }

    /**
     * 获取当前日期, 默认格式为yyyy-MM-dd
     *
     * @return String
     */
    public static String getDate() {
        return dateTimeNow(YYYY_MM_DD);
    }

    public static final String getTime() {
        return dateTimeNow(YYYY_MM_DD_HH_MM_SS);
    }

    public static final String dateTimeNow() {
        return dateTimeNow(YYYYMMDDHHMMSS);
    }

    public static final String dateTimeNow(final String format) {
        return parseDateToStr(format, new Date());
    }

    public static final String dateTime(final Date date) {
        return parseDateToStr(YYYY_MM_DD, date);
    }

    /**
     * 将时间转换为特定的时间格式
     *
     * @param format
     * @param date
     * @return
     */
    public static final String parseDateToStr(final String format, final Date date) {
        return new SimpleDateFormat(format).format(date);
    }

    public static final Date dateTime(final String format, final String ts) {
        try {
            return new SimpleDateFormat(format).parse(ts);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将yyyy-MM格式转换成yyyy年MM月
     *
     * @param DateStr
     * @return
     */
    public static final String parseDateToStr(final String DateStr) {
        SimpleDateFormat inputFormat = new SimpleDateFormat(DateUtil.YYYY_MM);
        SimpleDateFormat outputFormat = new SimpleDateFormat(DateUtil._YYYY_MM);
        try {
            //将时间字符串解析为Date对象
            Date date = inputFormat.parse(DateStr);
            // 使用输出格式将Date对象格式化为指定的字符串格式
            String outputStr = outputFormat.format(date);
            // 输出格式化后的字符串
            return outputStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 日期路径 即年/月/日 如2018/08/08
     */
    public static final String datePath() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyy/MM/dd");
    }

    /**
     * 日期路径 即年/月/日 如20180808
     */
    public static final String dateTime() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyyMMdd");
    }

    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return parseDate(str.toString(), parsePatterns);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 获取服务器启动时间
     */
    public static Date getServerStartDate() {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(time);
    }

    /**
     * 计算相差天数
     *
     * @param date1
     * @param date2
     */
    public static int diffDaysByMillisecond(long date1, long date2) {
        return Math.abs((int) ((date2 - date1) / (1000 * 3600 * 24)));
    }

    /**
     * 计算两个时间差
     */
    public static String getDatePoor(Date endDate, Date nowDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    /**
     * 计算两个时间差,只返回天数
     */
    public static int getDatePoorDay(Date endDate, Date nowDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return (int) day;
    }

    /**
     * 增加 LocalDateTime ==> Date
     */
    public static Date toDate(LocalDateTime temporalAccessor) {
        ZonedDateTime zdt = temporalAccessor.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 增加 LocalDate ==> Date
     */
    public static Date toDate(LocalDate temporalAccessor) {
        LocalDateTime localDateTime = LocalDateTime.of(temporalAccessor, LocalTime.of(0, 0, 0));
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }


    /**
     * 将date转换为localDateTime
     *
     * @param date
     * @return
     */
    public static LocalDateTime convertDateToLocalDateTime(Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
        return localDateTime;
    }


    /**
     * 将时间转换为LocalDate
     *
     * @param date
     * @return
     */
    public static LocalDate convertDateToLocalDate(Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        LocalDate localDate = zonedDateTime.toLocalDate();
        return localDate;
    }


    /**
     * n>0
     * 获取n天后的日期
     *
     * @param days
     * @return
     */
    public static String getDay(int days) {
        Calendar calendar = DateUtil.calendarLocal.get();
        SimpleDateFormat spformat = new SimpleDateFormat(YYYY_MM_DD);
        try {
            calendar.setTime(spformat.parse(getDate()));
        } catch (ParseException e) {
            return null;
        }
        calendar.add(DATE, days);
        return spformat.format(calendar.getTime());
    }

    /**
     * 日期格式转换
     * 2020-04-09T23:00:00.000+08:00 TO 2020-04-09 23:00:00
     * 在json转换称Date入库时,推荐使用@@DateTimeFormat注解
     * 仅在入参时进行使用
     */
    public static String dealDateFormat(String dateTime) {
        DateFormat df = new SimpleDateFormat(YYYY_MM_DDT_HH_MM_SS_SSSXXX);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.UK);
        Date dateOriginal = null;
        Date dateNew = null;
        try {
            dateOriginal = df.parse(dateTime);
            dateNew = sdf.parse(dateOriginal.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        df = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS);
        return df.format(dateNew);
    }

    /**
     * 日期格式转换
     * 2020-04-09 23:00:00 TO 2020-04-09T23:00:00.000+08:00
     */
    public static String dealDateFormatReverse(String dateTime) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateNew = null;
        try {
            dateNew = sdf.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return df.format(dateNew);
    }

    /***
     * yyyy-MM-dd HH:mm:ss 时间格式校验
     */
    public static boolean validateDateTime(String dateTime) {
        boolean flag = false;
        if (dateTime == null || "".equals(dateTime)) {
            return flag;
        } else {
            try {
                DateUtil.parseDate(dateTime, YYYY_MM_DD_HH_MM_SS);
            } catch (ParseException e) {
                return flag;
            }
            Calendar calendar = Calendar.getInstance();
            int yearInput = Integer.parseInt(dateTime.substring(0, 4));
            int monthInput = Integer.parseInt(dateTime.substring(5, 7));
            int dayInput = Integer.parseInt(dateTime.substring(8, 10));
            if (monthInput > 12) {
                return flag;
            }
            calendar.set(yearInput, monthInput, 0);
            int dayActual = calendar.get(Calendar.DAY_OF_MONTH);
            if (dayInput > dayActual) {
                return flag;
            }
            String time = dateTime.split("-")[1];
            boolean matches = time.matches("/^([0-1]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)$/");
            if (matches) {
                return !flag;
            }
            return flag;
        }
    }


    /***
     * @param dateStr 时间字符串
     * @param format 时间格式
     * @param field 字段 同Calendar类
     * @param gap 时间差
     * @return
     */
    public static Date getDate(String dateStr, String format, int field, int gap) {
        Date date = null;
        Calendar calendar = DateUtil.calendarLocal.get();
        try {
            date = new SimpleDateFormat(format).parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.setTime(date);
        calendar.add(field, gap);
        return calendar.getTime();
    }

    /**
     * 计算两个时间中所有的月份
     *
     * @param date1 开始时间 日期格式yyyy-MM-dd
     * @param date2 结束时间 日期格式yyyy-MM-dd
     * @return
     * @throws ParseException
     */
    @Deprecated
    public static List<String> getMonths(String date1, String date2) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse(date1);
        Date parse2 = sdf.parse(date2);
        List<String> dateList = new ArrayList<>();
        Calendar c1 = Calendar.getInstance();
        c1.setTime(parse);
        //转为周一
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH);
        c1.set(year, month, 1, 0, 0, 0);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(parse2);
        int weekYear2 = c2.get(Calendar.YEAR);
        int weekOfYear2 = c2.get(Calendar.WEEK_OF_YEAR);
        c2.setWeekDate(weekYear2, weekOfYear2, Calendar.SUNDAY);
        while (true) {
            int tempMonth = c1.get(Calendar.MONTH);
            String date = c1.getWeekYear() + "-" + ((tempMonth + 1) <= 9 ? "0" + (tempMonth + 1) : tempMonth + 1);
            dateList.add(date);
            //下一个月<结束日期
            c1.set(Calendar.MONTH, tempMonth + 1);
            if (c1.getTimeInMillis() >= c2.getTimeInMillis()) {
                break;
            }
        }
        return dateList;
    }

    /**
     * 计算两个时间所有月份的重载方法
     *
     * @author y
     * @date 2023/2/21 14:11
     * @modify 2023/2/21 14:11
     */
    @Deprecated
    public static List<String> getMonths(Date parse, Date parse2) throws ParseException {
        List<String> dateList = new ArrayList<>();
        Calendar c1 = Calendar.getInstance();
        c1.setTime(parse);
        //转为周一
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH);
        c1.set(year, month, 1, 0, 0, 0);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(parse2);
        int weekYear2 = c2.get(Calendar.YEAR);
        int weekOfYear2 = c2.get(Calendar.WEEK_OF_YEAR);
        c2.setWeekDate(weekYear2, weekOfYear2, Calendar.SUNDAY);
        while (true) {
            int tempMonth = c1.get(Calendar.MONTH);
            String date = c1.getWeekYear() + "-" + ((tempMonth + 1) <= 9 ? "0" + (tempMonth + 1) : tempMonth + 1);
            dateList.add(date);
            //下一个月<结束日期
            c1.set(Calendar.MONTH, tempMonth + 1);
            if (c1.getTimeInMillis() >= c2.getTimeInMillis()) {
                break;
            }
        }
        return dateList;
    }

    /**
     * 计算两个时间的所有年月
     * 返回时间格式 yyyy-MM
     *
     * @param start
     * @param end
     * @return
     * @throws ParseException
     */
    public static List<String> getMonthsBetween(String start, String end) throws ParseException {
        start = start.substring(0, 7);
        end = end.substring(0, 7);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        Date startDate = sdf.parse(start);
        Date endDate = sdf.parse(end);
        Calendar c = Calendar.getInstance();
        List<String> list = new ArrayList<String>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
        for (; startDate.getTime() <= endDate.getTime(); ) {
            String _startDate = df.format(startDate);
            list.add(_startDate);
            c.setTime(startDate);
            c.add(Calendar.MONTH, 1);
            startDate = c.getTime();
        }
        return list;
    }

    /**
     * 计算两个时间的所有年月的重载方法
     *
     * @author y
     * @date 2023/2/21 14:16
     * @modify 2023/2/21 14:16
     */
    public static List<String> getMonthsBetween(Date start, Date end) throws ParseException {

        Calendar c = Calendar.getInstance();
        List<String> list = new ArrayList<String>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
        for (; start.getTime() <= end.getTime(); ) {
            String _startDate = df.format(start);
            list.add(_startDate);
            c.setTime(start);
            c.add(Calendar.MONTH, 1);
            start = c.getTime();
        }
        return list;
    }


    /**
     * 计算两个时间内 所有的周数
     *
     * @param date1 开始时间 日期格式yyyy-MM-dd
     * @param date2 结束时间 日期格式yyyy-MM-dd
     * @return
     * @throws ParseException
     */
    public static List<String> getWeeks(String date1, String date2) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse(date1);
        Date parse2 = sdf.parse(date2);

        List<String> dateList = new ArrayList<>();
        Calendar c1 = Calendar.getInstance();
        c1.setTime(parse);
        //转为周一
        int weekYear = c1.get(Calendar.YEAR);
        int weekOfYear = c1.get(Calendar.WEEK_OF_YEAR);
        c1.setWeekDate(weekYear, weekOfYear, Calendar.MONDAY);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(parse2);
        int weekYear2 = c2.get(Calendar.YEAR);
        int weekOfYear2 = c2.get(Calendar.WEEK_OF_YEAR);
        c2.setWeekDate(weekYear2, weekOfYear2, Calendar.SUNDAY);
        while (true) {
            int weekNum = c1.get(Calendar.WEEK_OF_YEAR);
            dateList.add(c1.getWeekYear() + "-" + (weekNum > 9 ? weekNum : "0" + weekNum));
            if (c1.getTimeInMillis() >= c2.getTimeInMillis()) {
                break;
            }
            //增加7天
            c1.setTimeInMillis(c1.getTimeInMillis() + 1000 * 60 * 60 * 24 * 7);
        }
        return dateList;
    }

    /**
     * 计算两个时间内所有日期
     *
     * @param date1 开始时间 日期格式yyyy-MM-dd
     * @param date2 开始时间 日期格式yyyy-MM-dd
     * @return
     * @throws ParseException
     */
    public static List<String> getDays(String date1, String date2) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Long startTime = sdf.parse(date1).getTime();
        Long endTime = sdf.parse(date2).getTime();

        List<String> dateList = new ArrayList<String>();
        Long oneDay = 1000 * 60 * 60 * 24L;

        Long time = startTime;
        while (time <= endTime) {
            Date d = new Date(time);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String date = df.format(d);
            dateList.add(date);
            time += oneDay;
        }
        return dateList;
    }

    /**
     * 得到某一天的该星期的第一日 00:00:00
     *
     * @param date
     * @param firstDayOfWeek 一个星期的第一天为星期几
     * @return
     */
    public static Date getFirstDayOfWeek(Date date, int firstDayOfWeek) {
        Calendar cal = Calendar.getInstance();
        if (date != null)
            cal.setTime(date);
        cal.setFirstDayOfWeek(firstDayOfWeek);//设置一星期的第一天是哪一天
        cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);//指示一个星期中的某天
        cal.set(Calendar.HOUR_OF_DAY, 0);//指示一天中的小时。HOUR_OF_DAY 用于 24 小时制时钟。例如，在 10:04:15.250 PM 这一时刻，HOUR_OF_DAY 为 22。
        cal.set(Calendar.MINUTE, 0);//指示一小时中的分钟。例如，在 10:04:15.250 PM 这一时刻，MINUTE 为 4。
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /***
     * Map<String,Map<String,Date>>
     *     获取每个月的开始时间和结束时间
     *     第一个String参数 格式yyyy-MM)
     *     Map<String,Date>
     *         String “begin” 合同开始时间
     *         String “end” 合同的结束时间
     * @param begin 日期格式 yyyy-MM-dd
     * @param end 日期格式 yyyy-MM-dd
     * @return
     * @throws ParseException
     */
    public static Map<String, Map<String, Date>> getBeginAndEnd(String begin, String end) throws ParseException {
        List<String> months = getMonthsBetween(begin, end);
        Map<String, Map<String, Date>> dateMap = new HashMap<String, Map<String, Date>>(24);
        for (int i = 0; i < months.size(); i++) {
            Map<String, Date> beginEndMap = new HashMap<String, Date>(24);
            String dateStr = months.get(i);
            Date beginDate = null;
            Date endDate = null;
            if (i == 0) {
                beginDate = DateUtil.getBegin(begin, true);
                if (months.size() == 1) {
                    if (begin.equals(end)) {
                        endDate = DateUtil.getEndOfDay(end);
                    }
                    //如果没有超过一个月的时间,合同到期的前1s钟作为结束时间
                    endDate = DateUtil.getEnd(end, true);
                } else {
                    endDate = DateUtil.getEnd(dateStr);
                }
            } else if (i < months.size() - 1) {
                beginDate = DateUtil.getBegin(dateStr);
                endDate = DateUtil.getEnd(dateStr);
            } else {
                beginDate = DateUtil.getBegin(dateStr);
                endDate = DateUtil.getEnd(end, true);
            }
            beginEndMap.put("begin", beginDate);
            beginEndMap.put("end", endDate);
            dateMap.put(dateStr, beginEndMap);
        }
        //根据年月进行排序
        LinkedHashMap<String, Map<String, Date>> sortedDateMap = dateMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new
                ));
        return sortedDateMap;
    }


    /**
     * dateStr 参数格式yyyy-MM
     *
     * @return
     */
    public static Date getBegin(String dateStr) throws ParseException {
        Calendar calendar = DateUtil.calendarLocal.get();
        calendar.setTime(DateUtil.parseDate(dateStr, DateUtil.YYYY_MM));
        calendar.set(Calendar.DATE, 1);
        return calendar.getTime();
    }


    /**
     * dateStr 参数格式yyyy-MM-dd
     * flag 是否以当前日期作为开始
     *
     * @return
     */
    public static Date getBegin(String dateStr, boolean flag) throws ParseException {
        Calendar calendar = DateUtil.calendarLocal.get();
        calendar.setTime(DateUtil.parseDate(dateStr, DateUtil.YYYY_MM_DD));
        if (!flag) {
            calendar.set(Calendar.DATE, 1);
        }
        return calendar.getTime();
    }

    /**
     * dateStr 参数格式yyyy-MM
     *
     * @return
     */
    public static Date getEnd(String dateStr) throws ParseException {
        Calendar calendar = DateUtil.calendarLocal.get();
        calendar.setTime(DateUtil.parseDate(dateStr, DateUtil.YYYY_MM));
        //月份+1
        calendar.add(Calendar.MONTH, 1);
        //获取上个月的一天
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }


    /**
     * dateStr 参数格式yyyy-MM-dd
     * flag:是否以当前日期作为结束日期
     *
     * @return
     */
    public static Date getEnd(String dateStr, boolean flag) throws ParseException {
        Calendar calendar = DateUtil.calendarLocal.get();
        calendar.setTime(DateUtil.parseDate(dateStr, DateUtil.YYYY_MM_DD));
        if (!flag) {
            //获取当前日期的前一天的最后日期作为结束时间
            //月份+1
            calendar.add(Calendar.MONTH, 1);
            //获取上个月的一天
            calendar.set(Calendar.DAY_OF_MONTH, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
        } else {
            calendar.add(Calendar.DATE, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
        }
        return calendar.getTime();
    }


    public static Date getEnd2(String dateStr, boolean flag) throws ParseException {
        Calendar calendar = DateUtil.calendarLocal.get();
        calendar.setTime(DateUtil.parseDate(dateStr, DateUtil.YYYY_MM_DD));
        if (!flag) {
            //获取当前日期的前一天的最后日期作为结束时间
            //月份+1
            calendar.add(Calendar.MONTH, 1);
            //获取上个月的一天
            calendar.set(Calendar.DAY_OF_MONTH, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
        } else {
            //!!!注意不要改成0，否则金额计算会出现问题
            calendar.add(Calendar.DATE, 0);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
        }
        return calendar.getTime();
    }


    /**
     * 使用场景:判断是否租用满一个月
     * 参数说明,begin,end为一个月份内的两个时间
     * 将日期转换为yyyy-MM-dd格式，然后判断间隔是否超过本月的实际天数
     *
     * @param begin
     * @param end
     * @return
     */
    public static Map<String, Object> isOneMonth(Date begin, Date end) {
        Calendar calendar = DateUtil.calendarLocal.get();
        Map<String, Object> map = new HashMap<String, Object>();
        Integer.parseInt(DateUtil
                .parseDateToStr(DateUtil.YYYY_MM_DD, begin).substring(8, 10));
        Integer beginNum = Integer.parseInt(DateUtil
                .parseDateToStr(DateUtil.YYYY_MM_DD, begin).substring(8, 10));
        Integer endNum = Integer.parseInt(DateUtil
                .parseDateToStr(DateUtil.YYYY_MM_DD, end).substring(8, 10));
        Integer used = endNum - beginNum + 1;
        calendar.setTime(begin);
        Integer days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (used < days) {
            map.put("isOneMonth", false);
            map.put("used", used);
            map.put("days", days);
        } else {
            map.put("isOneMonth", true);
            map.put("used", used);
            map.put("days", days);
        }
        return map;
    }


    /**
     * @param begin
     * @param end
     * @return
     */
    public static Map<String, Object> isOnePeriod(Date begin, Date end, Integer gap) {
        LocalDateTime periodStart = DateUtil.convertDateToLocalDateTime(begin);
        LocalDateTime periodEnd = DateUtil.convertDateToLocalDateTime(end);
        LocalDateTime periodActualEnd = periodStart.plusMonths(gap).minusDays(1);
        Map<String, Object> map = new HashMap<>();
        //把当天的时间也算上一天
        if (periodEnd.isBefore(periodActualEnd)) {
            map.put("isOnePeriod", false);
            map.put("used", getDaysDiff(periodStart, periodEnd) + 1);
            map.put("days", getDaysDiff(periodStart, periodActualEnd) + 1);
            return map;
        } else {
            map.put("isOnePeriod", true);
            map.put("used", getDaysDiff(periodStart, periodEnd) + 1);
            map.put("days", getDaysDiff(periodStart, periodActualEnd) + 1);
            return map;
        }
    }


    /**
     * 计算两个时间间隔的天数
     *
     * @param begin
     * @param end
     * @return
     */
    public static Long getDaysDiff(LocalDateTime begin, LocalDateTime end) {
        long between = ChronoUnit.DAYS.between(begin, end);
        return between;
    }


    public static Long getDaysDiff(Date begin, Date end) {
        long between = Math.abs(end.getTime() - begin.getTime());
        return TimeUnit.DAYS.convert(between, TimeUnit.MILLISECONDS) + 1;
    }


    /***
     * 获取某天的结束时间
     * 例如2022-10-29
     * 2022-10-29 23:59;59
     * @param dateStr
     * @return
     */
    public static Date getEndOfDay(String dateStr) throws ParseException {
        Calendar calendar = DateUtil.calendarLocal.get();
        calendar.setTime(DateUtil.parseDate(dateStr, DateUtil.YYYY_MM_DD));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    /**
     * 将时间戳转换成Date
     *
     * @param time
     * @return
     */
    public static Date convertTimeStampToDate(Long time) {
        return new Date(time);
    }

    /**
     * 判断是否在同一个月
     *
     * @return false:不在同一个月内，true在同一个月内
     */
    public static boolean isSameMonth(Date date1, Date date2) {
        boolean flag = false;
        String date1Str = DateUtil.parseDateToStr(DateUtil.YYYY_MM, date1);
        String date2Str = DateUtil.parseDateToStr(DateUtil.YYYY_MM, date2);
        if (Objects.equals(date1Str, date2Str)) {
            return !flag;
        } else {
            return flag;
        }
    }


    public static String getDeadLine(int payNum, String nowMonth) {
        //将年月转化为整型
        String[] yearMonth = getYearMonth(nowMonth);
        int yearNum = Integer.parseInt(yearMonth[0]);
        int monthNum = Integer.parseInt(yearMonth[1]);
        //小于29日
        if (payNum <= 28) {
            return nowMonth + "-" + (payNum < 10 ? "0" + payNum : payNum);
        }
        if (payNum >= 29 && payNum <= 31) {
            boolean flag = validateDay(yearNum, monthNum, payNum);
            if (!flag) {
                return getLastDay(yearNum, monthNum);
            } else {
                return nowMonth + "-" + (payNum < 10 ? "0" + payNum : payNum);
            }
        }
        //如果传入的数值为32，则取每月的最后一天
        if (payNum == 32) {
            return getLastDay(yearNum, monthNum);
        }
        //如果传入的数据小于82,表示为次月某天
        if (payNum >= 51 && payNum < 82) {
            payNum -= 50;
            if (monthNum == 12) {
                yearNum += 1;
                return yearNum + "-" + "01" + "-" + (payNum < 10 ? "0" + payNum : payNum);
            } else {
                int nextMonth = monthNum + 1;
                if (nextMonth < 10) {
                    return yearNum + "-" + 0 + nextMonth + "-" + (payNum < 10 ? "0" + payNum : payNum);
                } else {
                    return yearNum + "-" + nextMonth + "-" + (payNum < 10 ? "0" + payNum : payNum);
                }
            }
        }
        //如果输入的数值等于82
        if (payNum == 82) {
            payNum -= 50;
            if (monthNum == 12) {
                return getLastDay(yearNum + 1, 1);
            }
            return getLastDay(yearNum, monthNum + 1);
        }
        return null;
    }

    /**
     * 输入年月，返回当月最后一天
     *
     * @return
     */
    public static String getLastDay(int year, int month) {
        Calendar calendar = DateUtil.calendarLocal.get();
        //设置年份
        calendar.set(Calendar.YEAR, year);
        //设置月份(中国月份-1)
        calendar.set(Calendar.MONTH, month - 1);
        //获取当前月最小值
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        //设置日历中的月份，当月+1月-1天等于当月最后一天
        calendar.set(Calendar.DAY_OF_MONTH, lastDay);
        //格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtil.YYYY_MM_DD);
        String date = sdf.format(calendar.getTime());
        return date;
    }

    /**
     * yyyy-MM
     * 对字符串进行切割，返回年和月
     */
    public static String[] getYearMonth(String nowMonth) {
        return nowMonth.split("-");
    }

    /**
     * 检验这个月份的用户输入日期是否合法
     *
     * @param year
     * @param month
     * @param day
     * @return
     */
    public static boolean validateDay(int year, int month, int day) {
        Calendar calendar = DateUtil.calendarLocal.get();
        //设置年份
        calendar.set(Calendar.YEAR, year);
        //设置月份(calendar月份是从0-11)
        calendar.set(Calendar.MONTH, month - 1);
//        calendar.set(Calendar.DAY_OF_MONTH, 1); // 设置日期
        int actualMaximum = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (day > actualMaximum) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 随机生成n位正整数，且保证全局唯一
     */
    public static String getRandomNumber(int n) {
        Random random = new Random();
//        n = 16-n;
        int x = random.nextInt(900) + 100;
        String srt = String.valueOf(x);
        String ti = String.valueOf(new Date().getTime()).substring(1, n);

        System.out.println(ti);
        System.out.println(new Date().getTime());
        return String.valueOf(new Date().getTime()).substring(1, n) + srt;

    }


    public static List<String> getMonths2(String date1, String date2) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse = sdf.parse(date1);
        Date parse2 = sdf.parse(date2);
        List<String> dateList = new ArrayList<>();
        Calendar c1 = Calendar.getInstance();
        c1.setTime(parse);
        //转为周一
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH);
        c1.set(year, month, 1, 0, 0, 0);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(parse2);
        int weekYear2 = c2.get(Calendar.YEAR);
        int weekOfYear2 = c2.get(Calendar.WEEK_OF_YEAR);
        c2.setWeekDate(weekYear2, weekOfYear2, Calendar.SUNDAY);
        while (true) {
            int tempMonth = c1.get(Calendar.MONTH);
            String date = c1.getWeekYear() + "-" + ((tempMonth + 1) <= 9 ? "0" + (tempMonth + 1) : tempMonth + 1);
            dateList.add(date);
            //下一个月<结束日期
            c1.set(Calendar.MONTH, tempMonth + 1);
            if (c1.getTimeInMillis() >= c2.getTimeInMillis()) {
                break;
            }
        }
        return dateList;
    }

    /**
     * 指定30天或者31天为一个月
     * 获取每个周期的开始与结束时间
     *
     * @param startDate
     * @param endDate
     * @param periodDays
     * @return
     */
    public static List<Period> getContractPeriods(LocalDateTime startDate, LocalDateTime endDate, int periodDays) {

        List<Period> periods = new ArrayList<>();

        LocalDateTime periodEndDate = startDate.plusDays(periodDays - 1);

        while (periodEndDate.isBefore(endDate)) {

            Period period = new Period(startDate, periodEndDate);

            periods.add(period);

            startDate = periodEndDate.plusDays(1);

            periodEndDate = startDate.plusDays(periodDays - 1);

        }

        periods.add(new Period(startDate, endDate));

        return periods;

    }

    /**
     * 以每个月的实际天数周期的开始与结束
     *
     * @param startDate
     * @param stopDate
     * @param gap
     * @return
     */
    public static Map<String, Map<String, Date>> getBeginAndEnd(Date startDate, Date stopDate, Integer gap) {
        // 合同开始时间
        LocalDateTime contractStartDateTime = DateUtil.convertDateToLocalDateTime(startDate);
        // 合同结束时间
        LocalDateTime contractEndDateTime = DateUtil.convertDateToLocalDateTime(stopDate);

        LocalDateTime periodStartDateTime = contractStartDateTime;
        LocalDateTime periodEndDateTime = periodStartDateTime.plusMonths(gap).minusSeconds(1);

        Map<String, Map<String, Date>> retMap = new HashMap<>();

        while (periodEndDateTime.isBefore(contractEndDateTime)) {
            retMap = extracted(periodStartDateTime, periodEndDateTime, retMap);
            periodStartDateTime = periodEndDateTime.plusSeconds(1);
            periodEndDateTime = periodStartDateTime.plusMonths(gap).minusSeconds(1);
        }

        retMap = extracted(periodStartDateTime, contractEndDateTime, retMap);

        //根据年月时进行排序，从小到大
        LinkedHashMap<String, Map<String, Date>> sortedDateMap = retMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new
                ));

        return sortedDateMap;
    }

    private static Map<String, Map<String, Date>> extracted(LocalDateTime periodStartDateTime, LocalDateTime periodEndDateTime, Map<String, Map<String, Date>> retMap) {
        Integer year = periodStartDateTime.getYear();
        Integer month = periodStartDateTime.getMonth().getValue();
        String monthStr = "";
        if (month < 10) {
            monthStr = "0" + month.toString();
        } else {
            monthStr = month.toString();
        }
        String key = year.toString() + "-" + monthStr;
        Map<String, Date> subMap = new HashMap<>();
        subMap.put("begin", DateUtil.toDate(periodStartDateTime));
        subMap.put("end", DateUtil.toDate(periodEndDateTime));
        retMap.put(key, subMap);
        return retMap;
    }

    public static Date endDayPlus(Date end_time, int day) {
        LocalDateTime localDateTime = DateUtil.convertDateToLocalDateTime(end_time).plusDays(day).minusSeconds(1);
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    public static Map<String, Map<String, Date>> getBeginAndEnd2(String begin, String end) throws ParseException {
        List<String> months = getMonthsBetween(begin, end);
        Map<String, Map<String, Date>> dateMap = new HashMap<String, Map<String, Date>>(24);
        for (int i = 0; i < months.size(); i++) {
            Map<String, Date> beginEndMap = new HashMap<String, Date>(24);
            String dateStr = months.get(i);
            Date beginDate = null;
            Date endDate = null;
            if (i == 0) {
                beginDate = DateUtil.getBegin(begin, true);
                if (months.size() == 1) {
                    if (begin.equals(end)) {
                        endDate = DateUtil.getEndOfDay(end);
                    }
                    //如果没有超过一个月的时间,合同到期的前1s钟作为结束时间
                    endDate = DateUtil.getEnd2(end, true);
                } else {
                    endDate = DateUtil.getEnd(dateStr);
                }
            } else if (i < months.size() - 1) {
                beginDate = DateUtil.getBegin(dateStr);
                endDate = DateUtil.getEnd(dateStr);
            } else {
                beginDate = DateUtil.getBegin(dateStr);
                endDate = DateUtil.getEnd2(end, true);
            }
            beginEndMap.put("begin", beginDate);
            beginEndMap.put("end", endDate);
            dateMap.put(dateStr, beginEndMap);
        }
        //根据年月进行排序
        LinkedHashMap<String, Map<String, Date>> sortedDateMap = dateMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new
                ));
        return sortedDateMap;
    }

    // 计算两个日期的月份数量
    public static int getMonthSpace(String date1, String date2) throws ParseException {

        int result = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();

        c1.setTime(sdf.parse(date1));
        c2.setTime(sdf.parse(date2));

        int i = c2.get(Calendar.YEAR) - c1.get(Calendar.YEAR);
        int month = 0;
        if (i < 0) {
            month = -i * 12;
        } else if (i > 0) {
            month = i * 12;
        }
        result = (c2.get(Calendar.MONDAY) - c1.get(Calendar.MONTH)) + month;

        return result == 0 ? 1 : Math.abs(result);

    }

    public static class Period {

        private LocalDateTime startDate;

        private LocalDateTime endDate;


        public Period(LocalDateTime startDate, LocalDateTime endDate) {

            this.startDate = startDate;

            this.endDate = endDate;

        }


        public LocalDateTime getStartDate() {

            return startDate;

        }


        public LocalDateTime getEndDate() {

            return endDate;

        }


        @Override
        public String toString() {

            return String.format("开始时间：%s，结束时间：%s", startDate, endDate);

        }

    }



    public static Date getStart(Date date) {
        Instant instant = date.toInstant();
        // 将 Instant 转换为当前时区的 ZonedDateTime
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        // 从 ZonedDateTime 中提取出日期部分，并转换为 LocalDateTime（时间部分设为0时0分0秒）
        LocalDateTime fromDate = zonedDateTime.toLocalDate().atStartOfDay();
        Date ret = DateUtil.toDate(fromDate);
        return ret;
    }



    public static Date getEnd(Date date) {
        Instant instant = date.toInstant();
        // 将 Instant 转换为当前时区的 ZonedDateTime
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        // 从 ZonedDateTime 中提取出日期部分，并转换为 LocalDateTime（时间部分设为0时0分0秒）
        LocalDateTime fromDate = zonedDateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = fromDate.plusDays(1).minusSeconds(1);
        Date ret = DateUtil.toDate(fromDate);
        return ret;
    }


}
