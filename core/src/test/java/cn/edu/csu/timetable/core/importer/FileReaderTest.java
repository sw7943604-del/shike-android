package cn.edu.csu.timetable.core.importer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import cn.edu.csu.timetable.core.*;
public class FileReaderTest {
 @Test public void readsParagraphDocx()throws Exception{String xml="<w:document xmlns:w='test'><w:body><w:p><w:r><w:t>数学</w:t></w:r></w:p><w:p><w:r><w:t>星期一 第1-2节</w:t></w:r></w:p><w:p><w:r><w:t>1-8周</w:t></w:r></w:p><w:p><w:r><w:t>A101</w:t></w:r></w:p></w:body></w:document>";assertEquals(1,FileTableReader.readDocx(zip("word/document.xml",xml)).courses.size());}
 @Test public void rejectsExternalEntity()throws Exception{String xml="<!DOCTYPE x [<!ENTITY x SYSTEM 'file:///does-not-exist'>]><worksheet><c r='A1'><v>&x;</v></c></worksheet>";ImportResult r=FileTableReader.readXlsx(zip("xl/worksheets/sheet1.xml",xml));assertTrue(r.courses.isEmpty());assertTrue(r.warnings.get(0).startsWith("文件读取失败"));}
 @Test public void readsInlineXlsx()throws Exception{String xml="<worksheet><sheetData><row><c r='B1' t='inlineStr'><is><t>1、2</t></is></c></row><row><c r='A2' t='inlineStr'><is><t>星期一</t></is></c><c r='B2' t='inlineStr'><is><t>数学&#10;1-8周&#10;A101&#10;车辆2401</t></is></c></row></sheetData></worksheet>";assertEquals(1,FileTableReader.readXlsx(zip("xl/worksheets/sheet1.xml",xml)).courses.size());}
 @Test public void readsDocx()throws Exception{String xml="<w:document xmlns:w='test'><w:tbl><w:tr><w:tc><w:p/></w:tc><w:tc><w:p><w:t>1、2</w:t></w:p></w:tc></w:tr><w:tr><w:tc><w:p><w:t>星期一</w:t></w:p></w:tc><w:tc><w:p><w:t>数学</w:t></w:p><w:p><w:t>1-8周</w:t></w:p><w:p><w:t>A101</w:t></w:p></w:tc></w:tr></w:tbl></w:document>";assertEquals(1,FileTableReader.readDocx(zip("word/document.xml",xml)).courses.size());}
 static InputStream zip(String name,String content)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(b)){z.putNextEntry(new ZipEntry(name));z.write(content.getBytes(StandardCharsets.UTF_8));z.closeEntry();}return new ByteArrayInputStream(b.toByteArray());}
 public static void main(String[] args)throws Exception{try(InputStream in=new FileInputStream(args[0])){ImportResult r=FileTableReader.readXls(in);System.out.println("courses="+r.courses.size()+" term="+r.termName+" date="+r.startDate+" times="+r.times.size()+" warnings="+r.warnings);assertEquals(21,r.courses.size());assertEquals(LocalDate.of(2026,9,6),r.startDate);assertEquals(10,r.times.size());assertTrue(r.courses.stream().allMatch(c->c.teacher.isEmpty()));assertTrue(r.courses.stream().anyMatch(c->c.weeks.size()==4&&c.weeks.contains(11)&&c.weeks.contains(17)));}}
}
