package cc.ivera.test.base;

import cc.ivera.model.dto.TimePeriodDTO;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Demo31 {

    public static boolean isOverlap(Date start1, Date end1, Date start2, Date end2) {
        return start1.before(end2) && end1.after(start2);
    }

    public static boolean checkOverlapForAllPeriods(TimePeriodDTO basePeriod, List<TimePeriodDTO> otherPeriods) {
        for (TimePeriodDTO period : otherPeriods) {
            if (isOverlap(basePeriod.getStartTime(), basePeriod.getEndTime(), period.getStartTime(), period.getEndTime())) {
                return true; // Overlap found
            }
        }
        return false; // No overlap found
    }

    public static void main(String[] args) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            TimePeriodDTO basePeriod = new TimePeriodDTO(dateFormat.parse("2023-10-11 00:00:00"), dateFormat.parse("2023-11-11 23:59:59"));
            List<TimePeriodDTO> otherPeriods = new ArrayList<>();
            otherPeriods.add(new TimePeriodDTO(dateFormat.parse("2023-11-12 00:00:00"), dateFormat.parse("2023-11-13 23:59:59")));
            otherPeriods.add(new TimePeriodDTO(dateFormat.parse("2023-11-14 00:00:00"), dateFormat.parse("2023-11-15 23:59:59")));
            System.out.println("Overlap with base period found: " + checkOverlapForAllPeriods(basePeriod, otherPeriods));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
