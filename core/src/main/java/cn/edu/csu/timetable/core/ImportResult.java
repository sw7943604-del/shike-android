package cn.edu.csu.timetable.core;

import java.time.LocalDate;
import java.util.*;

public class ImportResult {
    public final List<Course> courses = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
    public String termName = "";
    public LocalDate startDate;
    public final Map<Integer, String> times = new TreeMap<>();
}
