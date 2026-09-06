package cn.edu.csu.timetable.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.util.*;

public class ScheduleTest {
    private final LocalDate start = LocalDate.parse("2026-09-06");
    @Test public void sundayStartsNewTeachingWeek() {
        assertEquals(1, Schedule.weekOf(start, start));
        assertEquals(1, Schedule.weekOf(start, start.plusDays(6)));
        assertEquals(2, Schedule.weekOf(start, start.plusDays(7)));
        assertEquals(0, Schedule.weekOf(start, start.minusDays(1)));
        assertEquals(18, Schedule.weekOf(start, LocalDate.parse("2027-01-03")));
    }
    @Test public void respectsWeekGapsAndSortsPeriods() {
        Course a = new Course("甲", "", "", 1, 3, 4, Arrays.asList(1, 3));
        Course b = new Course("乙", "", "", 1, 1, 2, Arrays.asList(1));
        assertEquals(Arrays.asList(b, a), Schedule.onDate(Arrays.asList(a,b), start, start.plusDays(1)));
        assertTrue(Schedule.onDate(Arrays.asList(a,b), start, start.plusDays(8)).isEmpty());
        assertEquals(Arrays.asList(a), Schedule.onDate(Arrays.asList(a,b), start, start.plusDays(15)));
    }
}
