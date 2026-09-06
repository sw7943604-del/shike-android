package cn.edu.csu.timetable.core.importer;
import cn.edu.csu.timetable.core.*;
import java.util.*;
import java.util.regex.*;
public final class TextImporter {
 public static ImportResult parse(String text){ImportResult out=new ImportResult();if(text==null)text="";for(String part:text.split("(?:\\r?\\n){2,}|[-─]{3,}")){Matcher d=Pattern.compile("(?:星期|周)[一二三四五六日天]").matcher(part),p=Pattern.compile("第?\\d{1,2}\\s*[-、至]\\s*\\d{1,2}节").matcher(part);if(d.find()&&p.find()){int[] pp=TableImporter.periods(p.group());if(pp!=null)TableImporter.block(part.replace(d.group(),"").replace(p.group(),"").trim(),TableImporter.day(d.group()),pp[0],pp[1],out);}else if(!part.trim().isEmpty())out.warnings.add("以下文本缺少明确星期或节次，需手工补充："+part.trim().replace('\n',' '));}if(out.courses.isEmpty()&&out.warnings.isEmpty())out.warnings.add("文本为空或未识别到课程");out.warnings.add("文本识别结果需校对；请设置学期起始日");return out;}
}
