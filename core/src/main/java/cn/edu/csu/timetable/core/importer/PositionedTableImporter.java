package cn.edu.csu.timetable.core.importer;

import cn.edu.csu.timetable.core.ImportResult;
import java.util.*;

/** 将页面上的文字位置还原成课表单元格，坐标原点为左上角。 */
public final class PositionedTableImporter {
 public static final class Box {
  public final String text; public final float x,y,width,height;
  public Box(String text,float x,float y,float width,float height){this.text=text==null?"":text.trim();this.x=x;this.y=y;this.width=width;this.height=height;}
  float cx(){return x+width/2;} float cy(){return y+height/2;}
 }
 private static String label(Box b){return b.text.replaceAll("\\s+","");}
 private static List<Box> aligned(List<Box> boxes,boolean horizontal){
  List<Box> best=new ArrayList<>();
  for(Box anchor:boxes){List<Box> group=new ArrayList<>();for(Box b:boxes)if(Math.abs(horizontal?b.cy()-anchor.cy():b.cx()-anchor.cx())<=Math.max(8,Math.max(b.height,anchor.height)))group.add(b);if(group.size()>best.size())best=group;}
  best.sort(Comparator.comparingDouble(b->horizontal?b.cx():b.cy()));return best;
 }
 public static ImportResult parse(List<Box> input){
  List<Box> boxes=new ArrayList<>(),days=new ArrayList<>(),periods=new ArrayList<>();
  for(Box b:input)if(!b.text.isEmpty()){boxes.add(b);if(TableImporter.day(label(b))>0)days.add(b);if(TableImporter.periods(label(b))!=null)periods.add(b);}
  List<Box> across=aligned(days,true),down=aligned(days,false);boolean horizontal=across.size()>=down.size();
  List<Box> cols=horizontal?across:aligned(periods,true),rows=horizontal?aligned(periods,false):down;
  ImportResult empty=new ImportResult();
  if((horizontal?cols.size():rows.size())<2||cols.isEmpty()||rows.isEmpty()){empty.warnings.add("未能可靠定位PDF中的星期与节次表头，请人工校对或导入Excel。");return empty;}
  float top=cols.get(0).cy(),left=rows.get(0).cx();
  rows.removeIf(b->b.cy()<=top);cols.removeIf(b->b.cx()<=left);
  if(rows.isEmpty()||cols.isEmpty())return empty;
  List<List<String>> table=new ArrayList<>();List<String> header=new ArrayList<>();header.add("");for(Box c:cols)header.add(label(c));table.add(header);
  boolean multiple=false;List<String> uncertain=new ArrayList<>();
  for(int r=0;r<rows.size();r++){
   List<String> row=new ArrayList<>();row.add(label(rows.get(r)));
   for(int c=0;c<cols.size();c++){
    List<Box> cell=new ArrayList<>();
    for(Box b:boxes){if(cols.contains(b)||rows.contains(b))continue;if(b.cy()<=top+cols.get(0).height||b.cx()<=left+rows.get(0).width/2)continue;
     if(nearest(b.cy(),rows,false)==r&&nearest(b.cx(),cols,true)==c)cell.add(b);
    }
    cell.sort(Comparator.comparingDouble((Box b)->b.y).thenComparingDouble(b->b.x));StringBuilder text=new StringBuilder();Box previous=null;int weekLines=0;
    for(Box b:cell){if(previous!=null)text.append(Math.abs(b.cy()-previous.cy())<=Math.max(b.height,previous.height)*.5f?" ":"\n");String value=b.text;if(previous!=null&&value.matches("\\d{1,2}[-—－]\\d{1,2}")&&!WeekParser.parse(value).isEmpty()){uncertain.add("“"+cell.get(0).text+"”中的范围 "+value+" 缺少周单位，暂按周次显示，请确认或修改后再保存。");value+="周";}text.append(value);previous=b;if(value.contains("周")&&!WeekParser.parse(value).isEmpty())weekLines++;}
    if(weekLines>1&&!text.toString().matches("(?s).*[─━_-]{3,}.*"))multiple=true;
    row.add(text.toString());
   }table.add(row);
  }
  ImportResult result=TableImporter.parse(table);
  result.warnings.addAll(uncertain);
  result.warnings.add("PDF按文字坐标还原表格，请核对星期、节次、周次；合并单元格和跨页课程需人工检查。");
  if(multiple)result.warnings.add("检测到同一单元格可能包含多门课程且无明确分隔，可能有课程未导入，请逐项补充校对。");
  return result;
 }
 private static int nearest(float value,List<Box> anchors,boolean x){int index=0;float distance=Float.MAX_VALUE;for(int i=0;i<anchors.size();i++){float delta=Math.abs(value-(x?anchors.get(i).cx():anchors.get(i).cy()));if(delta<distance){distance=delta;index=i;}}return index;}
 private PositionedTableImporter(){}
}
