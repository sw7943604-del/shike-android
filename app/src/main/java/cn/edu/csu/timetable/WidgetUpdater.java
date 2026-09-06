package cn.edu.csu.timetable;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.RemoteViews;
import java.time.*;
import java.util.*;
import cn.edu.csu.timetable.core.*;

/** 由系统周期广播与保存操作刷新，无精确闹钟或后台常驻服务。 */
public class WidgetUpdater extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){if(i==null)return;String action=i.getAction();if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_DATE_CHANGED.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)||"cn.edu.csu.timetable.UPDATE_WIDGET".equals(action))refresh(c);}
    public static void refresh(Context context){
        Context c=context.getApplicationContext();AppWidgetManager manager=AppWidgetManager.getInstance(c);
        CourseStore store=new CourseStore(c);LocalDate today=LocalDate.now();String daily="尚未导入课表\n点击打开拾课",next=daily;
        try{
            List<Course> all=store.load();
            if(!all.isEmpty()){
                List<Course> courses=Schedule.onDate(all,store.startDate(),today);StringBuilder body=new StringBuilder();
                for(int i=0;i<Math.min(5,courses.size());i++){Course x=courses.get(i);if(i>0)body.append("\n\n");body.append(x.name).append("\n").append(store.timeLabel(x)).append("  ").append(x.location.isEmpty()?"教室待定":x.location);}
                if(courses.size()>5)body.append("\n另有 ").append(courses.size()-5).append(" 门 · 点击查看");
                daily=body.length()==0?"今天没有课程\n留一点时间给自己":body.toString();next="本学期暂无后续课程";
                Map<Integer,String> times=store.times();boolean found=false;LocalTime now=LocalTime.now();
                for(int offset=0;offset<=420&&!found;offset++){
                    LocalDate date=today.plusDays(offset);
                    for(Course x:Schedule.onDate(all,store.startDate(),date)){
                        String start=times.get(x.start),end=times.get(x.end);
                        if(start==null||end==null){next="课程时间待设置\n"+x.name+"\n点击设置作息后查看下一节";found=true;break;}
                        LocalTime a=LocalTime.parse(start.split("-")[0]),b=LocalTime.parse(end.split("-")[1]);
                        if(offset==0&&!now.isBefore(b))continue;
                        String when=offset==0?(now.isBefore(a)?"今天 · 即将开始":"正在上课 · "+b+" 下课"):offset==1?"明天":date.getMonthValue()+"月"+date.getDayOfMonth()+"日";
                        next=when+"\n\n"+x.name+"\n"+store.timeLabel(x)+"\n"+(x.location.isEmpty()?"教室待定":x.location);found=true;break;
                    }
                }
            }
        }catch(Exception e){daily=next="课表数据需要检查\n点击打开应用";}
        update(c,manager,TodayWidget.class,"今日课程 · "+today.getMonthValue()+"/"+today.getDayOfMonth(),daily);
        update(c,manager,NextWidget.class,"下一节课",next);
    }
    private static void update(Context c,AppWidgetManager m,Class<?> type,String title,String body){
        int[] ids=m.getAppWidgetIds(new ComponentName(c,type));
        for(int id:ids){RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.widget_schedule);views.setTextViewText(R.id.widget_title,title);views.setTextViewText(R.id.widget_body,body);views.setTextViewText(R.id.widget_footer,"拾课  ·  点击查看 / 刷新");
            PendingIntent p=PendingIntent.getActivity(c,id,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);views.setOnClickPendingIntent(R.id.widget_root,p);m.updateAppWidget(id,views);}
    }
}
