package cn.edu.csu.timetable.core;

import java.util.*;

public class Course {
    public String name, teacher, location;
    public int day, start, end;
    public SortedSet<Integer> weeks;
    public Course(String name, String teacher, String location, int day, int start, int end, Collection<Integer> weeks) {
        this.name = name == null ? "" : name.trim();
        this.teacher = teacher == null ? "" : teacher.trim();
        this.location = location == null ? "" : location.trim();
        this.day = day; this.start = start; this.end = end;
        this.weeks = new TreeSet<>(weeks);
    }
    public String key() { return name + "|" + day + "|" + start + "|" + end + "|" + location + "|" + weeks; }
    public String weeksText() { return weeks.toString().replace("[", "").replace("]", "").replace(" ", ""); }
    public Course copy() { return new Course(name, teacher, location, day, start, end, weeks); }
    public boolean valid() { return !name.isEmpty() && day >= 1 && day <= 7 && start >= 1 && end >= start && end <= 16 && !weeks.isEmpty() && weeks.first() >= 1 && weeks.last() <= 60; }
}
