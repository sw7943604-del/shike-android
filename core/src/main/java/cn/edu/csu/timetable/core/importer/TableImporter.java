package cn.edu.csu.timetable.core.importer;
import cn.edu.csu.timetable.core.*;
import java.util.*;
import java.util.regex.*;
import java.time.*;
public final class TableImporter {
 static int day(String s){Matcher m=Pattern.compile("(?:星期|周)([一二三四五六日天])").matcher(s.trim());return m.matches()?Math.min(7,"一二三四五六日天".indexOf(m.group(1))+1):0;}
 static int[] periods(String s){Matcher m=Pattern.compile("(?:第)?(\\d{1,2})\\s*[-—－、,，~至]\\s*(\\d{1,2})(?:节)?").matcher(s.trim());if(m.matches()){int a=Integer.parseInt(m.group(1)),b=Integer.parseInt(m.group(2));if(a>=1&&b>=a&&b<=16)return new int[]{a,b};}m=Pattern.compile("第?(\\d{1,2})节").matcher(s.trim());if(m.matches()){int a=Integer.parseInt(m.group(1));if(a>=1&&a<=16)return new int[]{a,a};}return null;}
 static String get(List<List<String>> t,int r,int c){return r>=0&&r<t.size()&&c>=0&&c<t.get(r).size()?t.get(r).get(c):"";}
 public static ImportResult parse(List<List<String>> t){ImportResult out=new ImportResult();if(t==null){out.warnings.add("没有可读取的表格");return out;}int header=-1;boolean daysAcross=false;
  for(int r=0;r<t.size();r++){int ds=0,ps=0;for(String s:t.get(r)){if(day(s)>0)ds++;if(periods(s)!=null)ps++;}if(ds>=2||ps>=1){header=r;daysAcross=ds>=2;break;}}
  if(header>=0){for(int r=header+1;r<t.size();r++){int d=0;int[] p=null;for(int c=0;c<Math.min(3,t.get(r).size());c++){if(c==0&&day(get(t,r,c))>0)d=day(get(t,r,c));if(periods(get(t,r,c))!=null)p=periods(get(t,r,c));}for(int c=0;c<t.get(r).size();c++){int dd=daysAcross?day(get(t,header,c)):d;int[] pp=daysAcross?p:periods(get(t,header,c));if(dd>0&&pp!=null)block(get(t,r,c),dd,pp[0],pp[1],out);}}}
  metadata(t,out);if(out.courses.isEmpty())out.warnings.add("未识别到具有星期、节次和周次的课程，请检查表格或手工补充");return out;
 }
 static void block(String cell,int day,int start,int end,ImportResult out){if(cell.trim().isEmpty())return;for(String part:cell.split("(?m)\\s*[-─━_]{3,}\\s*")){List<String> ls=new ArrayList<>();for(String l:part.split("[\\r\\n]+"))if(!l.trim().isEmpty())ls.add(l.trim());int wi=-1;SortedSet<Integer>w=new TreeSet<>();for(int i=0;i<ls.size();i++){if(ls.get(i).contains("周")){w=WeekParser.parse(ls.get(i));if(!w.isEmpty()){wi=i;break;}}}if(wi<1){if(!part.trim().isEmpty())out.warnings.add("课程块缺少可确认的课程名或周次："+part.trim().replace('\n',' '));continue;}String name=ls.get(0).replaceFirst("^课程[名称]*[:：]\\s*",""),teacher="",location="";
   // 导出文件周次前仅课程名；班级位于教室后，不作为教师。
   if(wi>1){String candidate=ls.get(1);if(!candidate.matches(".*(?:班|\\d{3,}).*"))teacher=candidate.replaceFirst("^(?:教师|老师)[:：]\\s*","");}
   if(wi+1<ls.size())location=ls.get(wi+1).replaceFirst("^(?:教室|地点)[:：]\\s*","");
   for(int i=wi+1;i<ls.size();i++)if(ls.get(i).contains("周")&&!WeekParser.parse(ls.get(i)).isEmpty())out.warnings.add("同一课程块存在多段周次，请补充核对其余安排："+part.trim().replace('\n',' '));
   Course course=new Course(name,teacher,location,day,start,end,w);if(course.valid())out.courses.add(course);
  }}
 static void metadata(List<List<String>> t,ImportResult out){for(int r=0;r<t.size();r++)for(int c=0;c<t.get(r).size();c++){String s=get(t,r,c);Matcher term=Pattern.compile("(20\\d{2})[-—](20\\d{2})[-—]([12])").matcher(s);if(term.find())out.termName=term.group();if(s.matches("\\d{2}:\\d{2}[-—]\\d{2}:\\d{2}")){try{int period=(int)Double.parseDouble(get(t,r-1,c));if(period>=1&&period<=16)out.times.put(period,s);}catch(Exception ignored){}}}
  for(int r=0;r<t.size();r++)for(int c=0;c<t.get(r).size();c++)if(get(t,r,c).equals("周次")){for(int j=c+1;j<t.get(r).size();j++)if(get(t,r,j).matches("1(?:\\.0)?")){try{int month=0;for(int k=0;k<=j;k++){Matcher m=Pattern.compile("^(\\d{1,2})月$").matcher(get(t,r-3,k));if(m.matches())month=Integer.parseInt(m.group(1));}int date=Integer.parseInt(get(t,r-2,j));int year=Integer.parseInt(out.termName.substring(0,4));if(month<7)year++;LocalDate first=LocalDate.of(year,month,date);if(first.getDayOfWeek()!=DayOfWeek.SUNDAY)throw new IllegalArgumentException();out.startDate=first;}catch(Exception e){out.warnings.add("校历第1周起始日无法可靠确认，请手动设置");}break;}}
  if(out.startDate==null)out.warnings.add("请核对学期起始日（学校教学周从周日开始）");
 }
}
