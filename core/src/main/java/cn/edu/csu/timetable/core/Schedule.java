package cn.edu.csu.timetable.core;

import java.time.LocalDate;
import java.util.*;
import java.time.temporal.ChronoUnit;

public final class Schedule {
    private Schedule() {}
    public static int weekOf(LocalDate start, LocalDate date) { return (int)Math.floorDiv(ChronoUnit.DAYS.between(start,date),7)+1; }
    public static List<Course> onDate(List<Course> courses, LocalDate start, LocalDate date) {
        int week=weekOf(start,date); List<Course> out=new ArrayList<>();
        for(Course c:courses) if(c.day==date.getDayOfWeek().getValue()&&c.weeks.contains(week)) out.add(c);
        out.sort(Comparator.comparingInt((Course c)->c.start).thenComparing(c->c.name));return out;
    }
}
