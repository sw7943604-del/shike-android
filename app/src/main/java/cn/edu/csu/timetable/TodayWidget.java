package cn.edu.csu.timetable;
import android.appwidget.*;
import android.content.*;
public class TodayWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){WidgetUpdater.refresh(c);}
}
