package cn.edu.csu.timetable;

import android.content.Context;
import android.content.SharedPreferences;
import cn.edu.csu.timetable.core.*;
import org.json.*;
import java.time.LocalDate;
import java.util.*;

/** 手机私有存储；导入确认后才写入。 */
public class CourseStore {
    private final SharedPreferences prefs;
    public CourseStore(Context context) { prefs = context.getSharedPreferences("shike", Context.MODE_PRIVATE); }
    public List<Course> load() {
        List<Course> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString("courses", "[]"));
            for (int i=0; i<array.length(); i++) {
                JSONObject c = array.getJSONObject(i); List<Integer> weeks = new ArrayList<>();
                JSONArray w = c.getJSONArray("weeks"); for(int j=0;j<w.length();j++) weeks.add(w.getInt(j));
                Course course = new Course(c.getString("name"), c.optString("teacher"), c.optString("location"), c.getInt("day"), c.getInt("start"), c.getInt("end"), weeks);
                if (course.valid()) out.add(course);
            }
        } catch (JSONException e) { throw new IllegalStateException("课程存储无法读取，请先导出备份后再处理", e); }
        return out;
    }
    public void save(List<Course> courses) {
        if(!prefs.edit().putString("courses",encode(courses).toString()).commit()) throw new IllegalStateException("保存失败，请检查手机存储空间");
    }
    private JSONArray encode(List<Course> courses) {
        JSONArray array = new JSONArray();
        try {
            for(Course c:courses) {
                if(!c.valid()) throw new IllegalArgumentException("课程信息不完整："+c.name);
                JSONObject o = new JSONObject(); o.put("name",c.name);o.put("teacher",c.teacher);o.put("location",c.location);
                o.put("day",c.day);o.put("start",c.start);o.put("end",c.end);o.put("weeks",new JSONArray(c.weeks));array.put(o);
            }
        } catch(JSONException e) { throw new IllegalStateException(e); }
        return array;
    }
    public LocalDate startDate() { return LocalDate.parse(prefs.getString("startDate","2026-09-06")); }
    public String termName() { return prefs.getString("termName","2026—2027 · 秋季学期"); }
    public String theme() { return prefs.getString("theme","system"); }
    public void setTheme(String theme) { prefs.edit().putString("theme",theme).apply(); }
    public Map<Integer,String> times() {
        Map<Integer,String> times = new TreeMap<>();
        String[] defaults={"08:00-08:45","08:55-09:40","10:00-10:45","10:55-11:40","14:00-14:45","14:55-15:40","16:00-16:45","16:55-17:40","19:00-19:45","19:55-20:40"};
        for(int i=0;i<defaults.length;i++) times.put(i+1,defaults[i]);
        if(prefs.contains("times")) {
            times.clear();
            try { JSONObject t=new JSONObject(prefs.getString("times","{}"));Iterator<String> keys=t.keys();while(keys.hasNext()){String key=keys.next();times.put(Integer.parseInt(key),t.getString(key));} }
            catch(Exception e){throw new IllegalStateException("作息数据无法读取",e);}
        }
        for(Map.Entry<Integer,String> e:times.entrySet())e.setValue(TimeSlots.normalize(e.getValue()));
        return times;
    }
    public void settings(String term, LocalDate start, Map<Integer,String> times) {
        JSONObject t=validatedTimes(times);
        if(!prefs.edit().putString("termName",term).putString("startDate",start.toString()).putString("times",t.toString()).commit()) throw new IllegalStateException("设置保存失败");
    }
    private JSONObject validatedTimes(Map<Integer,String> times){
        JSONObject t=new JSONObject();try{for(Map.Entry<Integer,String> e:times.entrySet()){
            if(e.getKey()<1||e.getKey()>16)throw new IllegalArgumentException("节次应为1—16");
            t.put(e.getKey().toString(),TimeSlots.normalize(e.getValue()));
        }}catch(JSONException e){throw new IllegalStateException(e);}return t;
    }
    public void saveImport(List<Course> courses,String term,LocalDate start,Map<Integer,String> times){
        JSONArray data=encode(courses);JSONObject clocks=validatedTimes(times);
        if(!prefs.edit().putString("courses",data.toString()).putString("termName",term).putString("startDate",start.toString()).putString("times",clocks.toString()).remove("preview").commit())throw new IllegalStateException("导入保存失败，请检查存储空间");
    }
    public void savePreview(ImportResult result){
        if(result==null){prefs.edit().remove("preview").apply();return;}
        try{JSONObject o=new JSONObject();o.put("courses",encode(result.courses));o.put("warnings",new JSONArray(result.warnings));o.put("term",result.termName);o.put("date",result.startDate==null?"":result.startDate.toString());JSONObject clocks=new JSONObject();for(Map.Entry<Integer,String> e:result.times.entrySet())clocks.put(e.getKey().toString(),e.getValue());o.put("times",clocks);prefs.edit().putString("preview",o.toString()).apply();}catch(JSONException e){throw new IllegalStateException(e);}
    }
    public ImportResult loadPreview(){
        String value=prefs.getString("preview",null);if(value==null)return null;
        try{JSONObject o=new JSONObject(value);ImportResult r=new ImportResult();JSONArray cs=o.getJSONArray("courses");for(int i=0;i<cs.length();i++){JSONObject c=cs.getJSONObject(i);List<Integer> ws=new ArrayList<>();JSONArray w=c.getJSONArray("weeks");for(int j=0;j<w.length();j++)ws.add(w.getInt(j));r.courses.add(new Course(c.getString("name"),c.optString("teacher"),c.optString("location"),c.getInt("day"),c.getInt("start"),c.getInt("end"),ws));}JSONArray warnings=o.getJSONArray("warnings");for(int i=0;i<warnings.length();i++)r.warnings.add(warnings.getString(i));r.termName=o.optString("term");if(!o.optString("date").isEmpty())r.startDate=LocalDate.parse(o.getString("date"));JSONObject t=o.getJSONObject("times");Iterator<String> keys=t.keys();while(keys.hasNext()){String k=keys.next();r.times.put(Integer.parseInt(k),t.getString(k));}return r;}catch(Exception e){throw new IllegalStateException("上次导入预览无法读取",e);}
    }
    public String timeLabel(Course c) {
        Map<Integer,String> t=times(); String a=t.get(c.start),b=t.get(c.end);
        if(a==null||b==null) return "第 "+c.start+"—"+c.end+" 节 · 时间待设置";
        return a.split("-")[0]+"—"+b.split("-")[1];
    }
}
