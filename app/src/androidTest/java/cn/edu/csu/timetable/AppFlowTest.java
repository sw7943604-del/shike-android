package cn.edu.csu.timetable;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import cn.edu.csu.timetable.core.*;
import cn.edu.csu.timetable.core.importer.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.*;

@RunWith(AndroidJUnit4.class)
public class AppFlowTest {
    private Context context;
    @Before public void reset(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.getSharedPreferences("shike",0).edit().clear().commit();MainActivity.pendingImport=null;}
    @Test public void realXlsOnAndroid(){
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("private-schedule.xls")){
            ImportResult r=FileTableReader.readXls(in);assertEquals(r.warnings.toString(),21,r.courses.size());assertEquals(LocalDate.of(2026,9,6),r.startDate);assertEquals(10,r.times.size());
            CourseStore store=new CourseStore(context);store.save(r.courses);store.settings(r.termName,r.startDate,r.times);assertEquals(21,new CourseStore(context).load().size());assertTrue(store.load().stream().allMatch(c->c.teacher.isEmpty()));
        }catch(IOException e){throw new AssertionError(e);}
    }
    @Test public void docxWorksWithAndroidXmlFactory() throws Exception {
        String xml="<?xml version='1.0' encoding='UTF-8'?><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body><w:tbl><w:tr><w:tc><w:p><w:r><w:t>节次</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>星期一</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>星期二</w:t></w:r></w:p></w:tc></w:tr><w:tr><w:tc><w:p><w:r><w:t>1-2</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>数学</w:t></w:r></w:p><w:p><w:r><w:t>1-16周</w:t></w:r></w:p><w:p><w:r><w:t>A101</w:t></w:r></w:p></w:tc><w:tc><w:p/></w:tc></w:tr></w:tbl></w:body></w:document>";
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(bytes)){z.putNextEntry(new ZipEntry("word/document.xml"));z.write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));z.closeEntry();}
        ImportResult r=FileTableReader.readDocx(new ByteArrayInputStream(bytes.toByteArray()));assertEquals(r.warnings.toString(),1,r.courses.size());
    }
    @Test public void normalizesClockAndRejectsInvalidSettings(){
        CourseStore store=new CourseStore(context);store.settings("学期",LocalDate.of(2026,9,6),Collections.singletonMap(1,"08:00—08:45"));
        Course c=new Course("课程","","",1,1,1,Arrays.asList(1));assertEquals("08:00—08:45",store.timeLabel(c));
        try{store.settings("错误学期",LocalDate.of(2026,9,6),Collections.singletonMap(1,"10:00-09:00"));fail("应拒绝倒序时间");}catch(IllegalArgumentException expected){}
        assertEquals("学期",store.termName());
    }
    @Test public void invalidImportIsAtomicAndPreviewSurvivesNewStore(){
        CourseStore store=new CourseStore(context);Course a=new Course("原课程","","",1,1,2,Arrays.asList(1));store.save(Arrays.asList(a));
        try{store.saveImport(Collections.emptyList(),"错误",LocalDate.now(),Collections.singletonMap(1,"10:00-09:00"));fail("无效作息不能保存");}catch(IllegalArgumentException expected){}
        assertEquals("原课程",new CourseStore(context).load().get(0).name);
        ImportResult r=new ImportResult();r.courses.add(a);r.warnings.add("待校对");store.savePreview(r);ImportResult restored=new CourseStore(context).loadPreview();assertEquals("原课程",restored.courses.get(0).name);assertEquals("待校对",restored.warnings.get(0));
    }
    @Test public void previewConfirmPersistAndTheme() throws Exception {
        ImportResult r=new ImportResult();r.courses.add(new Course("高等数学","","A101",1,1,2,Arrays.asList(1,2,3)));r.startDate=LocalDate.of(2026,9,6);r.termName="测试学期";MainActivity.pendingImport=r;
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            onView(withText("＋  导入")).perform(click());onView(withText("识别到 1 项上课安排")).check(matches(isDisplayed()));assertEquals(0,new CourseStore(context).load().size());
            onView(withText("确认导入 1 项安排")).perform(scrollTo(),click());onView(withText("保存")).perform(click());assertEquals(1,new CourseStore(context).load().size());
            scenario.recreate();onView(withText("第 1 周")).check(matches(isDisplayed()));
            onView(withText("☷  设置")).perform(click());onView(withText("外观")).perform(click());onView(withText("石墨黑")).perform(click());assertEquals("dark",new CourseStore(context).theme());
        }
    }
    @Test public void captureImportedWeek() throws Exception {
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("private-schedule.xls")){
            ImportResult r=FileTableReader.readXls(in);CourseStore store=new CourseStore(context);store.save(r.courses);store.settings(r.termName,r.startDate,r.times);
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                onView(withText("第 1 周")).check(matches(isDisplayed()));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),"week-light.png"))){screenshot.compress(Bitmap.CompressFormat.PNG,100,out);}screenshot.recycle();
            }
        }
    }
    @Test public void pdfTextAndChineseScanBothImport() throws Exception {
        for(String name:Arrays.asList("text-table.pdf","scan-table.pdf")){
            File file=new File(context.getCacheDir(),name);
            try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name);FileOutputStream out=new FileOutputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            ImportResult r=DocumentReader.read(context,android.net.Uri.fromFile(file),name);assertEquals(name+": "+r.warnings,2,r.courses.size());assertTrue(r.courses.stream().anyMatch(c->c.name.equals("高等数学")&&c.day==1&&c.start==1));assertTrue(r.courses.stream().anyMatch(c->c.name.equals("大学英语")&&c.day==2&&c.start==3));file.delete();
        }
    }
}
