package cn.edu.csu.timetable.core.importer;
import cn.edu.csu.timetable.core.ImportResult;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PositionedTableImporterTest {
 @Test public void numericWeekRangeInsideCellIsKeptForReview(){
  ImportResult r=PositionedTableImporter.parse(Arrays.asList(box("星期一",100,10),box("星期二",220,10),box("1-2节",10,90),box("3-4节",10,210),box("数学",100,65),box("张老师",100,80),box("1-16",100,95),box("A101",100,110)));
  assertEquals(1,r.courses.size());assertTrue(r.warnings.stream().anyMatch(w->w.contains("缺少周单位")));
 }
 private PositionedTableImporter.Box box(String text,float x,float y){return new PositionedTableImporter.Box(text,x,y,40,10);}
 @Test public void horizontalDays(){
  ImportResult r=PositionedTableImporter.parse(Arrays.asList(box("星期一",100,10),box("星期二",220,10),box("1-2节",10,90),box("3-4节",10,210),box("高等数学",100,65),box("张老师",100,80),box("1-16周",100,95),box("A101",100,110)));
  assertEquals(1,r.courses.size());assertEquals(1,r.courses.get(0).day);assertEquals(2,r.courses.get(0).end);assertEquals("高等数学",r.courses.get(0).name);
 }
 @Test public void verticalDays(){
  ImportResult r=PositionedTableImporter.parse(Arrays.asList(box("1-2节",100,10),box("3-4节",220,10),box("星期一",10,90),box("星期二",10,210),box("大学物理",220,185),box("1-8周",220,205),box("B202",220,225)));
  assertEquals(1,r.courses.size());assertEquals(2,r.courses.get(0).day);assertEquals(3,r.courses.get(0).start);
 }
 @Test public void noAnchorsDoesNotGuess(){assertTrue(PositionedTableImporter.parse(Arrays.asList(box("数学",10,10),box("1-8周",10,30))).courses.isEmpty());}
 @Test public void warnsForMultipleUnseparatedCourses(){
  ImportResult r=PositionedTableImporter.parse(Arrays.asList(box("星期一",100,10),box("星期二",220,10),box("1-2节",10,90),box("数学",100,50),box("1-8周",100,65),box("A101",100,80),box("物理",100,95),box("9-16周",100,110),box("A102",100,125)));
  assertTrue(r.warnings.stream().anyMatch(s->s.contains("多门")));
 }
}
