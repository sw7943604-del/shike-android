package cn.edu.csu.timetable;

import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.util.*;
import cn.edu.csu.timetable.core.*;

@RunWith(AndroidJUnit4.class)
public class WidgetTest {
    @Test public void bothWidgetsBindAndReceiveCourseUpdates() throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context c=instrumentation.getTargetContext();
        CourseStore store=new CourseStore(c);LocalDate today=LocalDate.now();store.saveImport(Arrays.asList(new Course("组件测试课程","","A101",today.getDayOfWeek().getValue(),1,2,Arrays.asList(1))),"组件测试",today,Map.of(1,"08:00-08:45",2,"08:55-09:40"));
        // 仅为隔离模拟器中的测试宿主临时允许绑定，结束后撤回。
        try(ParcelFileDescriptor fd=instrumentation.getUiAutomation().executeShellCommand("appwidget grantbind --package cn.edu.csu.timetable --user 0");java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){in.readAllBytes();}
        AppWidgetHost host=new AppWidgetHost(c,812);AppWidgetManager manager=AppWidgetManager.getInstance(c);
        try{
            for(Class<?> type:Arrays.asList(TodayWidget.class,NextWidget.class)){
                int id=host.allocateAppWidgetId();assertTrue(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c,type)));
                final AppWidgetHostView[] view={null};instrumentation.runOnMainSync(()->{host.startListening();view[0]=host.createView(c,id,manager.getAppWidgetInfo(id));WidgetUpdater.refresh(c);});
                boolean rendered=false;
                for(int tries=0;tries<30&&!rendered;tries++){
                    instrumentation.waitForIdleSync();final String[] body={""};instrumentation.runOnMainSync(()->{TextView text=view[0].findViewById(R.id.widget_body);if(text!=null)body[0]=text.getText().toString();});
                    rendered=type==TodayWidget.class?body[0].contains("组件测试课程"):body[0].contains("组件测试课程")||body[0].contains("暂无后续课程");
                    if(!rendered)Thread.sleep(100);
                }
                assertTrue(type.getSimpleName()+"应显示本地课表状态",rendered);host.deleteAppWidgetId(id);
            }
        }finally{
            host.stopListening();host.deleteHost();try(ParcelFileDescriptor fd=instrumentation.getUiAutomation().executeShellCommand("appwidget revokebind --package cn.edu.csu.timetable --user 0");java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){in.readAllBytes();}
        }
    }
}
