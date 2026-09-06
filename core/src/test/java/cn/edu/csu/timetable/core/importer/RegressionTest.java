package cn.edu.csu.timetable.core.importer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.util.*;
import cn.edu.csu.timetable.core.*;

public class RegressionTest {
    @Test public void htmlNonBreakingSpacesDoNotHideDayOrSectionHeaders(){
        String html="<table><tr><th>&nbsp;</th><th>星期日&nbsp;</th><th>星期一&nbsp;</th><th>星期二&nbsp;</th></tr>"
            +"<tr><th>1&nbsp;-&nbsp;2</th><td></td><td><div>测试技术与信号处理<br><font>肖友刚[教授]</font><br>1-8(周)<br>世B101<br>--------------------<br>工程流体力学<br>王家斌[副教授],杨明智[教授]<br>9,12-18(周)<br>世A104</div></td><td>测试技术与信号处理<br>肖友刚[教授]<br>1-8(周)<br>世B101</td></tr></table>";
        ImportResult r=HtmlImporter.parse(html);
        assertEquals(3,r.courses.size());assertEquals(1,r.courses.get(0).day);
        assertEquals(1,r.courses.get(0).start);assertEquals(2,r.courses.get(0).end);
        assertEquals("肖友刚[教授]",r.courses.get(0).teacher);
        assertEquals("世A104",r.courses.get(1).location);assertFalse(r.courses.get(1).weeks.contains(10));
    }
    @Test public void clockSeparatorsAreNormalized(){assertEquals("08:00-08:45",TimeSlots.normalize("08:00—08:45"));}
    @Test public void sundayAliasIsSunday(){assertEquals(7,TableImporter.day("星期天"));}
    @Test public void nestedSchoolTableAndCalendar(){
        String html="<select id=xnxq01id><option>2025-2026-2</option><option selected>2026-2027-1</option></select><table><tr><td><table id=kbtable><tr><td></td><td>星期日</td><td>星期一</td></tr><tr><td>1－2</td><td></td><td><div>数学<br>张老师<br>1-8(周)<br>A101</div></td></tr></table></td></tr></table><table id=kbtable><tr><td>第1周 2026-09-06日至2026-09-12日</td></tr></table>";
        ImportResult r=HtmlImporter.parse(html);assertEquals(1,r.courses.size());assertEquals(1,r.courses.get(0).day);assertEquals("2026-2027-1",r.termName);assertEquals(LocalDate.of(2026,9,6),r.startDate);
    }
    @Test public void unseparatedSecondCourseIsNotSilentlyLost(){
        ImportResult r=TableImporter.parse(Arrays.asList(Arrays.asList("","1-2"),Arrays.asList("星期一","数学\n1-8周\nA101\n英语\n9-16周\nB102")));
        assertTrue(r.courses.size()==2||r.warnings.stream().anyMatch(w->w.contains("多段周次")));
    }
}
