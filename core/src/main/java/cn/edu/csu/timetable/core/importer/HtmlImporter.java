package cn.edu.csu.timetable.core.importer;

import cn.edu.csu.timetable.core.*;
import java.util.*;
import java.util.regex.*;
import java.time.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

public final class HtmlImporter {
    public static ImportResult parse(String html) {
        Document doc=Jsoup.parse(html==null?"":html);
        ImportResult out=new ImportResult();Map<String,Course> unique=new LinkedHashMap<>();
        int dayHeaders=0,sectionHeaders=0;
        // 逐个读取表格，布局容器不参与子表行解析。
        for(Element table:doc.select("table")){
            List<List<String>> cells=grid(table);
            for(List<String> row:cells)for(String cell:row){if(TableImporter.day(cell)>0)dayHeaders++;if(TableImporter.periods(cell)!=null)sectionHeaders++;}
            ImportResult one=TableImporter.parse(cells);
            for(Course c:one.courses)unique.put(c.key(),c);
            out.times.putAll(one.times);
            if(!one.termName.isEmpty())out.termName=one.termName;
            if(one.startDate!=null)out.startDate=one.startDate;
            if(!one.courses.isEmpty())for(String w:one.warnings)if(!w.startsWith("请核对"))out.warnings.add(w);
        }
        out.courses.addAll(unique.values());
        Element selected=doc.selectFirst("select#xnxq01id option[selected]");
        String termText=selected==null?doc.text():selected.text();
        Matcher term=Pattern.compile("20\\d{2}-20\\d{2}-[12]").matcher(termText);
        if(term.find())out.termName=term.group();
        Matcher first=Pattern.compile("第\\s*1\\s*周\\s*(20\\d{2}-\\d{2}-\\d{2})日?\\s*至\\s*(20\\d{2}-\\d{2}-\\d{2})").matcher(doc.text());
        if(first.find())try{
            LocalDate a=LocalDate.parse(first.group(1)),b=LocalDate.parse(first.group(2));
            if(a.plusDays(6).equals(b))out.startDate=a;else out.warnings.add("网页教学周日期区间异常，请核对学期起始日");
        }catch(Exception e){out.warnings.add("网页校历日期无法读取，请手动设置");}
        for(Element cell:doc.select("td")){
            String note=cell.ownText().trim();
            if(note.contains("周") && cell.select("table").isEmpty() && (cell.id().equals("bz")||cell.previousElementSibling()!=null&&cell.previousElementSibling().text().startsWith("备注")))
                out.warnings.add("实践或补充安排，需确认具体星期和节次："+cell.text());
        }
        if(out.courses.isEmpty())out.warnings.add("未识别到课程（表格 "+doc.select("table").size()+"，星期表头 "+dayHeaders+"，节次表头 "+sectionHeaders+"）。请反馈此提示以定位网页格式。");
        if(out.startDate==null)out.warnings.add("请核对学期起始日（学校教学周从周日开始）");
        return out;
    }
    static List<List<String>> grid(Element table){
        List<List<String>> rows=new ArrayList<>();Set<String> occupied=new HashSet<>();int r=0;
        for(Element tr:table.select("tr")){
            if(tr.closest("table")!=table)continue;if(r>=1000)break;
            while(rows.size()<=r)rows.add(new ArrayList<>());int c=0;
            for(Element cell:tr.children()){
                if(!cell.tagName().matches("td|th"))continue;while(occupied.contains(r+":"+c))c++;
                if(c>500)break;int rs=span(cell,"rowspan"),cs=span(cell,"colspan");Element copy=cell.clone();
                copy.select("table").remove();copy.select("br").append("\n");copy.select("div,p").append("\n");
                // HTML 的不换行空格不被 Java 的 trim 和默认正则空白识别，保留换行并规范空格。
                String value=copy.wholeText().replaceAll("[\\p{Zs}]"," ").replace("\u200B", "").replace("\uFEFF", "").trim();
                for(int dr=0;dr<rs;dr++){while(rows.size()<=r+dr)rows.add(new ArrayList<>());for(int dc=0;dc<cs;dc++){List<String> row=rows.get(r+dr);while(row.size()<=c+dc)row.add("");row.set(c+dc,dc==0?value:"");occupied.add((r+dr)+":"+(c+dc));}}c+=cs;
            }r++;
        }return rows;
    }
    static int span(Element e,String a){try{return Math.max(1,Math.min(100,Integer.parseInt(e.attr(a))));}catch(Exception ex){return 1;}}
}
