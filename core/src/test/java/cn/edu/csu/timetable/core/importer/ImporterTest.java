package cn.edu.csu.timetable.core.importer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import cn.edu.csu.timetable.core.*;
public class ImporterTest {
 @Test public void weeks(){assertEquals(new TreeSet<>(Arrays.asList(11,13,15,17)),WeekParser.parse("11-18单周(8学时)")); assertEquals(16,WeekParser.parse("1-9,12-18(周)").size());}
 @Test public void exportedTable(){ImportResult r=TableImporter.parse(Arrays.asList(Arrays.asList("","1、2"),Arrays.asList("星期一","机械设计\n1-9,12-18周(64学时)\n新B101\n车辆2401-02\n---------\n控制基础\n11-18单周\n新B102\n车辆2401-02")));assertEquals(2,r.courses.size());assertEquals("",r.courses.get(0).teacher);assertEquals("新B101",r.courses.get(0).location);}
 @Test public void htmlSpans(){ImportResult r=HtmlImporter.parse("<table id=kbtable><tr><td>节次</td><td>星期一</td><td>星期二</td></tr><tr><td rowspan=2>1-2节</td><td></td><td>数学<br>张老师<br>1-8(周)<br>A101<br>车辆2401</td></tr><tr><td>英语<br>2-8(双周)<br>B102</td><td></td></tr></table>");assertEquals(2,r.courses.size());assertEquals(2,r.courses.get(0).day);assertEquals("张老师",r.courses.get(0).teacher);assertEquals(1,r.courses.get(1).day);}
 @Test public void textNeedsCoordinates(){assertTrue(TextImporter.parse("高等数学 1-8周 A101").courses.isEmpty());assertFalse(TextImporter.parse("高等数学 1-8周 A101").warnings.isEmpty());}
}
