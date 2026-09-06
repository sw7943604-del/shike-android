package cn.edu.csu.timetable;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import cn.edu.csu.timetable.core.*;
import cn.edu.csu.timetable.core.importer.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    public static ImportResult pendingImport;
    private CourseStore store;
    private Ui ui;
    private LinearLayout root,body;
    private List<Course> courses=new ArrayList<>();
    private String tab="week";
    private int week;
    private static boolean busy;
    private static java.lang.ref.WeakReference<MainActivity> active=new java.lang.ref.WeakReference<>(null);
    private static final ExecutorService executor=Executors.newSingleThreadExecutor();
    private static final String[] DAYS={"周一","周二","周三","周四","周五","周六","周日"};

    @Override public void onCreate(Bundle saved){store=new CourseStore(this);setTheme(new Ui(this,store.theme()).dark?R.style.AppTheme_Dark:R.style.AppTheme);super.onCreate(saved);active=new java.lang.ref.WeakReference<>(this);courses=store.load();if(pendingImport==null)pendingImport=store.loadPreview();if(pendingImport!=null||busy)tab="import";week=Math.max(1,Schedule.weekOf(store.startDate(),LocalDate.now()));if(saved!=null){tab=saved.getString("tab",tab);week=saved.getInt("week",week);}render();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("tab",tab);out.putInt("week",week);}
    @Override protected void onResume(){super.onResume();if(store!=null){courses=store.load();render();WidgetUpdater.refresh(this);}}
    @Override protected void onDestroy(){super.onDestroy();if(active.get()==this)active.clear();}
    private void render(){
        if(isFinishing()||isDestroyed())return;
        store.savePreview(pendingImport);
        ui=new Ui(this,store.theme());root=ui.column();root.setBackgroundColor(ui.bg);
        if(android.os.Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        else root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        setContentView(root);root.requestApplyInsets();getWindow().setStatusBarColor(ui.bg);getWindow().setNavigationBarColor(ui.bg);getWindow().getDecorView().setSystemUiVisibility(ui.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout header=ui.row();ui.pad(header,22,16);LinearLayout titles=ui.column();titles.addView(ui.text("SHIKE  /  拾课",11,ui.muted));ui.gap(titles,5);titles.addView(ui.title(tab.equals("week")?"让时间，有条不紊。":tab.equals("today")?"今天，从容一点。":tab.equals("import")?"把课程，带进来。":"按你的节奏。",24));header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);body=ui.column();ui.pad(body,18,2);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(tab.equals("week"))weekPage();else if(tab.equals("today"))todayPage();else if(tab.equals("import"))importPage();else settingsPage();
        ui.gap(body,20);LinearLayout nav=ui.row();ui.pad(nav,12,10);nav.setBackgroundColor(ui.surface);
        String[] keys={"week","today","import","settings"},names={"▦  课表","◷  今日","＋  导入","☷  设置"};
        for(int i=0;i<keys.length;i++){String key=keys[i];TextView b=ui.button(names[i],()->{tab=key;render();});b.setTextColor(tab.equals(key)?ui.accent:ui.muted);if(tab.equals(key))b.setBackground(ui.shape(ui.line,14));nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);
    }
    private void weekPage(){
        body.addView(ui.text(store.termName(),13,ui.muted));ui.gap(body,14);
        LinearLayout control=ui.row();control.addView(ui.button("‹",()->{week=Math.max(1,week-1);render();}));TextView label=ui.title("第 "+week+" 周",18);label.setGravity(17);control.addView(label,new LinearLayout.LayoutParams(0,-2,1));control.addView(ui.button("今天",()->{week=Math.max(1,Schedule.weekOf(store.startDate(),LocalDate.now()));render();}));control.addView(ui.button("›",()->{week=Math.min(60,week+1);render();}));body.addView(control);ui.gap(body,10);
        LocalDate first=store.startDate().plusWeeks(week-1);body.addView(ui.text(first.format(DateTimeFormatter.ofPattern("M月d日"))+" — "+first.plusDays(6).format(DateTimeFormatter.ofPattern("M月d日"))+"  ·  按学校教学周",12,ui.muted));ui.gap(body,14);
        if(courses.isEmpty()){empty("你的第一张课表","导入学校导出的文件，或登录教务系统。\n课程会留在手机里，离线也能查看。","导入课程",()->{tab="import";render();});return;}
        List<Course> visible=new ArrayList<>();for(Course c:courses)if(c.weeks.contains(week))visible.add(c);
        if(visible.isEmpty()){body.addView(ui.text("这一周没有安排，留一点时间给自己。",14,ui.muted));ui.gap(body,12);}
        body.addView(new WeekView(this,ui,visible,first,c->courseDetail(c)));
        ui.gap(body,12);body.addView(ui.button("＋ 手动添加课程",()->editCourse(null,c->{courses.add(c);save();})));
    }
    private void todayPage(){
        LocalDate today=LocalDate.now();int current=Schedule.weekOf(store.startDate(),today);body.addView(ui.text(today.format(DateTimeFormatter.ofPattern("M月d日"))+" · "+DAYS[today.getDayOfWeek().getValue()-1]+" · "+(current<1?"尚未开学":"第 "+current+" 周"),14,ui.muted));ui.gap(body,18);
        List<Course> list=Schedule.onDate(courses,store.startDate(),today);
        if(list.isEmpty()){empty("今天没有课","好好安排这段属于自己的时间。","查看周课表",()->{tab="week";render();});return;}
        body.addView(ui.title("今日 "+list.size()+" 项课程",18));ui.gap(body,14);for(Course c:list){LinearLayout card=courseCard(c,true);card.setOnClickListener(v->courseDetail(c));body.addView(card);ui.gap(body,12);}
    }
    private LinearLayout courseCard(Course c,boolean clock){
        LinearLayout card=ui.card();card.addView(ui.text(clock?store.timeLabel(c):DAYS[c.day-1]+" · 第 "+c.start+"—"+c.end+" 节",12,ui.accent));ui.gap(card,9);card.addView(ui.title(c.name,18));ui.gap(card,8);card.addView(ui.text((c.location.isEmpty()?"地点待定":c.location)+(c.teacher.isEmpty()?"":"  ·  "+c.teacher),13,ui.muted));if(!clock){ui.gap(card,8);card.addView(ui.text("第 "+c.weeksText()+" 周",11,ui.muted));}return card;
    }
    private void courseDetail(Course c){
        new AlertDialog.Builder(this).setTitle(c.name).setMessage(DAYS[c.day-1]+" · "+store.timeLabel(c)+"\n"+(c.location.isEmpty()?"地点待定":c.location)+"\n"+(c.teacher.isEmpty()?"教师未填写":c.teacher)+"\n第 "+c.weeksText()+" 周")
        .setPositiveButton("编辑",(d,w)->editCourse(c,edited->{courses.set(courses.indexOf(c),edited);save();})).setNeutralButton("删除",(d,w)->new AlertDialog.Builder(this).setTitle("删除这项课程安排？").setMessage("仅删除手机里的这项安排，不影响教务系统。").setPositiveButton("删除",(dd,ww)->{courses.remove(c);save();}).setNegativeButton("取消",null).show()).setNegativeButton("关闭",null).show();
    }
    private void save(){try{store.save(courses);WidgetUpdater.refresh(this);render();}catch(Exception e){error(e.getMessage());}}
    private EditText field(LinearLayout parent,String label,String value){parent.addView(ui.text(label,12,ui.muted));EditText edit=new EditText(this);edit.setTextColor(ui.ink);edit.setTextSize(15);edit.setSingleLine(true);edit.setText(value);parent.addView(edit,new LinearLayout.LayoutParams(-1,ui.dp(50)));ui.gap(parent,8);return edit;}
    private void editCourse(Course existing,java.util.function.Consumer<Course> done){
        LinearLayout form=ui.column();ui.pad(form,22,12);ScrollView scroll=new ScrollView(this);scroll.addView(form);form.setBackgroundColor(ui.bg);
        EditText name=field(form,"课程名称",existing==null?"":existing.name),teacher=field(form,"教师（选填）",existing==null?"":existing.teacher),location=field(form,"教室（选填）",existing==null?"":existing.location);
        EditText day=field(form,"星期：1为周一，7为周日",existing==null?"1":""+existing.day),start=field(form,"开始节次",existing==null?"1":""+existing.start),end=field(form,"结束节次",existing==null?"2":""+existing.end),weeks=field(form,"上课周次，例如 1-9,12-18 或 1-18单周",existing==null?"1-18":existing.weeksText());
        for(EditText e:Arrays.asList(day,start,end))e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"添加课程":"编辑课程").setView(scroll).setPositiveButton("保存",null).setNegativeButton("取消",null).create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{Course c=new Course(name.getText().toString(),teacher.getText().toString(),location.getText().toString(),Integer.parseInt(day.getText().toString()),Integer.parseInt(start.getText().toString()),Integer.parseInt(end.getText().toString()),WeekParser.parse(weeks.getText().toString()));if(!c.valid())throw new IllegalArgumentException("请检查课程名称、星期、节次与周次（1—60）");done.accept(c);dialog.dismiss();}catch(Exception e){name.setError("请检查输入："+e.getMessage());}}));dialog.show();
    }
    private void importPage(){
        body.addView(ui.text("本地识别 · 先校对，再保存",13,ui.muted));ui.gap(body,18);
        if(busy){LinearLayout card=ui.card();card.addView(new ProgressBar(this));ui.gap(card,12);card.addView(ui.title("正在识别课表…",18));card.addView(ui.text("扫描PDF可能需要一些时间，请保持应用开启。",13,ui.muted));body.addView(card);return;}
        if(pendingImport!=null){preview();return;}
        importOption("01","选择课表文件","支持 Excel、Word（DOCX）、PDF。\n学校导出的 XLS 可直接使用。",()->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,101);});
        importOption("02","从中南大学教务导入","在手机内完成登录，再读取本科生课表。",()->startActivityForResult(new Intent(this,PortalActivity.class),102));
        importOption("03","粘贴课程文字","适合通知中的课程安排，识别后可编辑。",this::pasteImport);
        body.addView(ui.button("＋ 手动添加一门课",()->editCourse(null,c->{courses.add(c);store.save(courses);WidgetUpdater.refresh(this);tab="week";render();})));
        ui.gap(body,18);body.addView(ui.text("教师信息可以留空。复杂扫描件如无法还原表格，请改用Excel或补充星期、节次后粘贴识别。",12,ui.muted));
    }
    private void importOption(String n,String title,String desc,Runnable action){LinearLayout card=ui.card();card.addView(ui.text(n+"  /  IMPORT",11,ui.accent));ui.gap(card,10);card.addView(ui.title(title,20));ui.gap(card,9);card.addView(ui.text(desc,13,ui.muted));card.setOnClickListener(v->action.run());body.addView(card);ui.gap(body,12);}
    private void pasteImport(){EditText text=new EditText(this);text.setMinLines(7);text.setGravity(48);text.setHint("示例：\n高等数学\n星期一 第1-2节\n1-16周\n教室：A101");new AlertDialog.Builder(this).setTitle("粘贴课程安排").setView(text).setPositiveButton("识别",(d,w)->{pendingImport=TextImporter.parse(text.getText().toString());render();}).setNegativeButton("取消",null).show();}
    private void preview(){
        ImportResult result=pendingImport;body.addView(ui.title("识别到 "+result.courses.size()+" 项上课安排",21));ui.gap(body,10);body.addView(ui.text("同一门课在不同时间上课，会分别列出。点选条目可校对或排除。",12,ui.muted));ui.gap(body,12);
        if(!result.warnings.isEmpty()){LinearLayout warn=ui.card();warn.addView(ui.title("请留意",15));for(String w:result.warnings){ui.gap(warn,6);warn.addView(ui.text(w,12,ui.muted));}body.addView(warn);ui.gap(body,12);}
        for(Course c:new ArrayList<>(result.courses)){LinearLayout card=courseCard(c,false);card.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(c.name).setItems(new String[]{"编辑这项安排","从本次导入中排除"},(d,pos)->{if(pos==0)editCourse(c,edited->{result.courses.set(result.courses.indexOf(c),edited);render();});else{result.courses.remove(c);render();}}).show());body.addView(card);ui.gap(body,10);}
        body.addView(ui.button("＋ 补充课程",()->editCourse(null,c->{result.courses.add(c);render();})));ui.gap(body,10);
        if(!result.courses.isEmpty())body.addView(ui.button("确认导入 "+result.courses.size()+" 项安排",()->confirmImport(result)));
        ui.gap(body,10);body.addView(ui.button("放弃本次导入",()->{pendingImport=null;render();}));
    }
    private void confirmImport(ImportResult result){
        LinearLayout form=ui.column();ui.pad(form,22,10);EditText term=field(form,"学期名称",result.termName.isEmpty()?store.termName():result.termName);EditText start=field(form,"第1周起始日期（中南大学为周日）",(result.startDate==null?store.startDate():result.startDate).toString());
        CheckBox replace=new CheckBox(this);replace.setText("替换手机中的全部课程");replace.setTextColor(ui.ink);replace.setChecked(courses.isEmpty());form.addView(replace);form.addView(ui.text("不勾选则合并，完全相同的安排不会重复导入。请确认起始日期。",12,ui.muted));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("保存到我的课表").setView(form).setPositiveButton("保存",null).setNegativeButton("继续校对",null).create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{
            LocalDate date=LocalDate.parse(start.getText().toString().trim());LinkedHashMap<String,Course> map=new LinkedHashMap<>();if(!replace.isChecked())for(Course c:courses)map.put(c.key(),c);
            for(Course c:result.courses){Course previous=map.get(c.key());Course copy=c.copy();if(previous!=null&&copy.teacher.isEmpty())copy.teacher=previous.teacher;map.put(copy.key(),copy);}
            List<Course> updated=new ArrayList<>(map.values());Map<Integer,String> times=new TreeMap<>(store.times());times.putAll(result.times);store.saveImport(updated,term.getText().toString(),date,times);courses=updated;pendingImport=null;WidgetUpdater.refresh(this);week=Math.max(1,Schedule.weekOf(date,LocalDate.now()));tab="week";dialog.dismiss();render();Toast.makeText(this,"课程已保存",Toast.LENGTH_SHORT).show();
        }catch(Exception e){start.setError("请检查日期，格式为2026-09-06；"+e.getMessage());}}));dialog.show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK)return;
        if(request==102){tab="import";render();return;}
        if(request==101&&data!=null&&data.getData()!=null){Uri uri=data.getData();String name="";try(Cursor cursor=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}catch(Exception ignored){}String filename=name;
            busy=true;tab="import";render();Context app=getApplicationContext();executor.execute(()->{try{ImportResult parsed=DocumentReader.read(app,uri,filename);new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{busy=false;pendingImport=parsed;new CourseStore(app).savePreview(parsed);MainActivity activity=active.get();if(activity!=null)activity.render();});}catch(Exception e){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{busy=false;MainActivity activity=active.get();if(activity!=null){activity.render();activity.error("无法导入："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}});}});
        }
    }
    private void settingsPage(){
        body.addView(ui.text("离线保存 · 为自己而用",13,ui.muted));ui.gap(body,18);
        LinearLayout semester=ui.card();semester.addView(ui.title("学期与作息",19));ui.gap(semester,8);semester.addView(ui.text(store.termName()+"\n第1周从 "+store.startDate()+" 开始\n已设置 "+store.times().size()+" 节课的作息",13,ui.muted));semester.setOnClickListener(v->editSettings());body.addView(semester);ui.gap(body,12);
        LinearLayout theme=ui.card();theme.addView(ui.title("外观",19));ui.gap(theme,8);theme.addView(ui.text("跟随系统 / 暖白 / 石墨黑",13,ui.muted));theme.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("选择外观").setItems(new String[]{"跟随系统","暖白","石墨黑"},(d,i)->{store.setTheme(new String[]{"system","light","dark"}[i]);recreate();}).show());body.addView(theme);ui.gap(body,12);
        LinearLayout widgets=ui.card();widgets.addView(ui.title("让课表住在桌面",19));ui.gap(widgets,8);widgets.addView(ui.text("也可以长按手机桌面，选择小组件 → 拾课。\n系统按周期刷新，点击组件会打开课表。",13,ui.muted));ui.gap(widgets,12);widgets.addView(ui.button("添加「今日课程」",()->pin(TodayWidget.class)));ui.gap(widgets,8);widgets.addView(ui.button("添加「下一节课」",()->pin(NextWidget.class)));body.addView(widgets);ui.gap(body,12);
        body.addView(ui.text("拾课 0.1\n课程与导入文件均在手机本地处理。教师信息可选。\n识别结果以教务系统最终安排为准。",12,ui.muted));
    }
    private void pin(Class<?> type){AppWidgetManager manager=AppWidgetManager.getInstance(this);if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(this,type),null,null);else Toast.makeText(this,"请长按桌面 → 小组件 → 拾课",Toast.LENGTH_LONG).show();}
    private void editSettings(){
        LinearLayout form=ui.column();ui.pad(form,22,12);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        EditText term=field(form,"学期名称",store.termName()),date=field(form,"第1周起始日期",store.startDate().toString());form.addView(ui.text("作息：每行 节次=开始时间-结束时间\n如 1=08:00-08:45；未设置的节次不猜测钟点。",12,ui.muted));EditText times=new EditText(this);times.setTextColor(ui.ink);times.setMinLines(10);StringBuilder value=new StringBuilder();for(Map.Entry<Integer,String> e:store.times().entrySet())value.append(e.getKey()).append('=').append(e.getValue()).append('\n');times.setText(value.toString());form.addView(times);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("学期与作息").setView(scroll).setPositiveButton("保存",null).setNegativeButton("取消",null).create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{try{LocalDate d=LocalDate.parse(date.getText().toString().trim());Map<Integer,String> parsed=new TreeMap<>();for(String line:times.getText().toString().split("\\n")){if(line.trim().isEmpty())continue;String[] pair=line.trim().split("=");if(pair.length!=2)throw new IllegalArgumentException("作息格式错误");int period=Integer.parseInt(pair[0].trim());String[] clock=pair[1].trim().split("-");if(clock.length!=2||period<1||period>16)throw new IllegalArgumentException("节次或时刻格式错误");LocalTime a=LocalTime.parse(clock[0]),z=LocalTime.parse(clock[1]);if(!a.isBefore(z))throw new IllegalArgumentException("下课时间应晚于上课时间");parsed.put(period,a+"-"+z);}if(parsed.isEmpty())throw new IllegalArgumentException("至少设置一节课的作息");store.settings(term.getText().toString(),d,parsed);WidgetUpdater.refresh(this);dialog.dismiss();render();}catch(Exception e){times.setError("请检查输入："+e.getMessage());}}));dialog.show();
    }
    private void empty(String title,String desc,String button,Runnable action){LinearLayout card=ui.card();ui.pad(card,24,32);card.addView(ui.text("▦",40,ui.accent));ui.gap(card,18);card.addView(ui.title(title,22));ui.gap(card,12);card.addView(ui.text(desc,14,ui.muted));ui.gap(card,24);card.addView(ui.button(button,action));body.addView(card);}
    private void error(String message){if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setTitle("暂时无法完成").setMessage(message).setPositiveButton("知道了",null).show();}
}
