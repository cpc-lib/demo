package cc.ivera.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

    private static Date date = new Date();
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String formatDate(){
        return sdf.format(date);
    }
}
