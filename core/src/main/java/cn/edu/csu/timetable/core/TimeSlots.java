package cn.edu.csu.timetable.core;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** 统一所有来源的作息格式，禁止无效时刻进入本地存储。 */
public final class TimeSlots {
    public static String normalize(String input) {
        String[] parts=input.trim().replace('—','-').replace('－','-').replace('～','-').split("-");
        if(parts.length!=2)throw new IllegalArgumentException("作息格式应为08:00-08:45");
        DateTimeFormatter f=DateTimeFormatter.ofPattern("H:mm");
        LocalTime a=LocalTime.parse(parts[0].trim(),f),b=LocalTime.parse(parts[1].trim(),f);
        if(!a.isBefore(b))throw new IllegalArgumentException("下课时间应晚于上课时间");
        return a.toString()+"-"+b.toString();
    }
}
