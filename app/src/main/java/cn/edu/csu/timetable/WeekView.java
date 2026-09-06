package cn.edu.csu.timetable;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.view.*;
import cn.edu.csu.timetable.core.Course;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/** 以教学周日期为准绘制七列，课程卡片可点开编辑。 */
final class WeekView extends View {
    private final Ui ui;
    private final List<Course> courses;
    private final LocalDate first;
    private final Consumer<Course> click;
    private final Paint paint=new Paint(3);
    private final List<RectF> hitRects=new ArrayList<>();
    private final List<Course> hitCourses=new ArrayList<>();
    private final int[] colors={0xffdae7d4,0xffe8dfed,0xffd7e6ed,0xfff0e3cb,0xffe9d8d2,0xffd9e6df};
    WeekView(Context c,Ui ui,List<Course> courses,LocalDate first,Consumer<Course> click){super(c);this.ui=ui;this.courses=courses;this.first=first;this.click=click;setContentDescription("七日课程表，点击课程查看详情");}
    @Override protected void onMeasure(int w,int h){int max=10;for(Course c:courses)max=Math.max(max,c.end);setMeasuredDimension(MeasureSpec.getSize(w),ui.dp(48+max*47));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);hitRects.clear();hitCourses.clear();float left=ui.dp(27),top=ui.dp(48),rh=ui.dp(47),cw=(getWidth()-left)/7f;
        paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);
        for(int col=0;col<7;col++){
            LocalDate date=first.plusDays(col);boolean today=date.equals(LocalDate.now());float x=left+col*cw;
            if(today){paint.setColor(ui.line);canvas.drawRoundRect(x+2,0,x+cw-2,getHeight(),ui.dp(9),ui.dp(9),paint);}
            paint.setColor(today?ui.accent:ui.muted);paint.setTextSize(ui.dp(10));canvas.drawText(new String[]{"一","二","三","四","五","六","日"}[date.getDayOfWeek().getValue()-1],x+cw/2,ui.dp(15),paint);
            paint.setTextSize(ui.dp(13));canvas.drawText(String.valueOf(date.getDayOfMonth()),x+cw/2,ui.dp(34),paint);
        }
        for(int period=1;period<=(getHeight()-top)/rh;period++){
            float y=top+(period-1)*rh;paint.setColor(ui.line);paint.setStrokeWidth(1);canvas.drawLine(left,y,getWidth(),y,paint);
            paint.setColor(ui.muted);paint.setTextSize(ui.dp(10));canvas.drawText(String.format(Locale.ROOT,"%02d",period),ui.dp(12),y+ui.dp(25),paint);
        }
        for(Course c:courses){
            int col=-1;for(int i=0;i<7;i++)if(first.plusDays(i).getDayOfWeek().getValue()==c.day)col=i;
            if(col<0)continue;
            float x=left+col*cw+ui.dp(2),y=top+(c.start-1)*rh+ui.dp(2);
            RectF rect=new RectF(x,y,x+cw-ui.dp(4),top+c.end*rh-ui.dp(2));
            paint.setColor(colors[Math.floorMod(c.name.hashCode(),colors.length)]);canvas.drawRoundRect(rect,ui.dp(8),ui.dp(8),paint);
            hitRects.add(rect);hitCourses.add(c);
            TextPaint tp=new TextPaint(3);tp.setColor(0xff293f33);tp.setTextSize(ui.dp(10));tp.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            String label=c.name+"\n\n"+(c.location.isEmpty()?"地点待定":c.location);
            int width=Math.max(ui.dp(15),(int)rect.width()-ui.dp(8));
            StaticLayout layout=StaticLayout.Builder.obtain(label,0,label.length(),tp,width).setMaxLines(Math.max(2,(int)(rect.height()/ui.dp(13))-1)).setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false).build();
            canvas.save();canvas.clipRect(rect);canvas.translate(x+ui.dp(4),y+ui.dp(7));layout.draw(canvas);canvas.restore();
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP){for(int i=hitRects.size()-1;i>=0;i--)if(hitRects.get(i).contains(e.getX(),e.getY())){performClick();click.accept(hitCourses.get(i));return true;}}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
