package cn.edu.csu.timetable.core.importer;
import java.util.*;
import java.util.regex.*;
public final class WeekParser {
 public static SortedSet<Integer> parse(String text) {
  SortedSet<Integer> result=new TreeSet<>(); if(text==null)return result;
  String s=text.replace('，',',').replace('、',',').replace('－','-').replace('—','-').replace('～','-').replace('~','-');
  int week=s.indexOf('周'); if(week>=0)s=s.substring(0,week+1); s=s.replaceAll("[()（）\\s第单双周]","");
  if(!s.matches("\\d+(?:-\\d+)?(?:,\\d+(?:-\\d+)?)*"))return result;
  for(String p:s.split(",")){String[] a=p.split("-");try{int lo=Integer.parseInt(a[0]),hi=a.length==2?Integer.parseInt(a[1]):lo;if(lo<1||hi>60||lo>hi)return new TreeSet<>();for(int i=lo;i<=hi;i++)if((!text.contains("单")||i%2==1)&&(!text.contains("双")||i%2==0))result.add(i);}catch(NumberFormatException e){return new TreeSet<>();}}
  return result;
 }
}
