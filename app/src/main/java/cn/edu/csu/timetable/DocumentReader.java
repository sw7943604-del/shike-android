package cn.edu.csu.timetable;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.android.gms.tasks.Tasks;
import cn.edu.csu.timetable.core.*;
import cn.edu.csu.timetable.core.importer.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.*;

/** 文档仅在本地处理，扫描件识别运行在后台线程。 */
final class DocumentReader {
    static ImportResult read(Context context,Uri uri,String name) throws Exception {
        byte[] bytes;
        try(InputStream in=context.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException("无法打开文件");byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1){if(out.size()+n>32*1024*1024)throw new IOException("文件超过32MB，请只保留课表页面后重试");out.write(buffer,0,n);}bytes=out.toByteArray();
        }
        String lower=name.toLowerCase(java.util.Locale.ROOT);
        if(lower.endsWith(".xls"))return FileTableReader.readXls(new ByteArrayInputStream(bytes));
        if(lower.endsWith(".xlsx"))return FileTableReader.readXlsx(new ByteArrayInputStream(bytes));
        if(lower.endsWith(".docx"))return FileTableReader.readDocx(new ByteArrayInputStream(bytes));
        if(lower.endsWith(".doc"))throw new IOException("旧版 .doc 请先在 Word 中另存为 .docx 或 PDF，再导入");
        if(lower.endsWith(".pdf") || (bytes.length>4&&new String(bytes,0,4,StandardCharsets.US_ASCII).equals("%PDF"))){
            PDFBoxResourceLoader.init(context);List<ImportResult> pages=new ArrayList<>();List<Integer> scanPages=new ArrayList<>();
            try(PDDocument doc=PDDocument.load(bytes)){
                if(doc.getNumberOfPages()>30)throw new IOException("PDF超过30页，请只导出课表页面");
                for(int i=0;i<doc.getNumberOfPages();i++){
                    PositionStripper stripper=new PositionStripper();stripper.setStartPage(i+1);stripper.setEndPage(i+1);
                    String text=stripper.getText(doc);ImportResult page=PositionedTableImporter.parse(stripper.boxes);
                    if(page.courses.isEmpty()){ImportResult plain=TextImporter.parse(text);if(!plain.courses.isEmpty())page=plain;}
                    pages.add(page);if(page.courses.isEmpty())scanPages.add(i);
                }
            }
            if(scanPages.isEmpty())return combine(pages);
            // 无可用文字或布局无法解析时尝试中文OCR；仍不确定则保留原文供校对。
            File temp=File.createTempFile("schedule-", ".pdf",context.getCacheDir());
            try{
                try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);}
                try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(temp,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd);TextRecognizer recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build())){
                    if(scanPages.size()>12)throw new IOException("待识别扫描页超过12页，请拆分后导入");
                    for(int i:scanPages){
                        try(PdfRenderer.Page page=renderer.openPage(i)){
                            float scale=Math.min(2.5f,2200f/Math.max(page.getWidth(),page.getHeight()));
                            Bitmap bitmap=Bitmap.createBitmap(Math.max(1,(int)(page.getWidth()*scale)),Math.max(1,(int)(page.getHeight()*scale)),Bitmap.Config.ARGB_8888);
                            try{
                                bitmap.eraseColor(Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                                Text text=Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)),45,TimeUnit.SECONDS);
                                List<PositionedTableImporter.Box> boxes=new ArrayList<>();
                                // OCR可能把相邻单元格合为一行，按元素间距重新切分。
                                for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines()){
                                    Rect bounds=null;StringBuilder run=new StringBuilder();
                                    for(Text.Element element:line.getElements()){
                                        Rect b=element.getBoundingBox();if(b==null)continue;
                                        if(bounds!=null&&b.left-bounds.right>Math.max(12,Math.max(bounds.height(),b.height())*1.2f)){
                                            boxes.add(new PositionedTableImporter.Box(run.toString(),bounds.left,bounds.top,bounds.width(),bounds.height()));bounds=null;run.setLength(0);
                                        }
                                        if(bounds==null)bounds=new Rect(b);else bounds.union(b);run.append(element.getText());
                                    }
                                    if(bounds!=null)boxes.add(new PositionedTableImporter.Box(run.toString(),bounds.left,bounds.top,bounds.width(),bounds.height()));
                                }
                                ImportResult parsed=PositionedTableImporter.parse(boxes);
                                if(parsed.courses.isEmpty()){ImportResult plain=TextImporter.parse(text.getText());if(!plain.courses.isEmpty())parsed=plain;}
                                parsed.warnings.add("第"+(i+1)+"页使用本地中文OCR，请核对识别文字和全部课程。");pages.set(i,parsed);
                            }catch(Exception error){pages.get(i).warnings.add("第"+(i+1)+"页本地OCR失败，请手动补充或拆分重试。");}finally{bitmap.recycle();}
                        }
                    }
                }
                return combine(pages);
            }finally{if(!temp.delete())temp.deleteOnExit();}
        }
        if(lower.endsWith(".txt"))return TextImporter.parse(new String(bytes,StandardCharsets.UTF_8));
        throw new IOException("请选择 XLS、XLSX、DOCX、PDF 或 UTF-8 TXT 文件");
    }

    private static ImportResult combine(List<ImportResult> pages){
        ImportResult result=new ImportResult();Set<String> keys=new HashSet<>();
        for(int i=0;i<pages.size();i++){
            ImportResult page=pages.get(i);for(Course course:page.courses)if(keys.add(course.key()))result.courses.add(course);
            for(String warning:page.warnings)result.warnings.add("第"+(i+1)+"页："+warning);
            if(!page.termName.isEmpty())result.termName=page.termName;if(page.startDate!=null)result.startDate=page.startDate;result.times.putAll(page.times);
        }
        return result;
    }

    /** 按字符间距分段，避免PDF抽取器把不同列连接为同一句。 */
    private static final class PositionStripper extends PDFTextStripper {
        final List<PositionedTableImporter.Box> boxes=new ArrayList<>();
        PositionStripper() throws IOException {setSortByPosition(true);}
        @Override protected void writeString(String text,List<TextPosition> positions) throws IOException {
            StringBuilder run=new StringBuilder();float left=0,top=0,right=0,bottom=0;TextPosition previous=null;
            for(TextPosition p:positions){
                float x=p.getXDirAdj(),y=p.getYDirAdj()-p.getHeightDir();
                if(previous!=null&&(x-right>Math.max(3,p.getWidthOfSpace()*1.8f)||Math.abs(p.getYDirAdj()-previous.getYDirAdj())>Math.max(p.getHeightDir(),previous.getHeightDir())*.6f)){
                    boxes.add(new PositionedTableImporter.Box(run.toString(),left,top,right-left,bottom-top));run.setLength(0);
                }
                if(run.length()==0){left=x;top=y;right=x;bottom=p.getYDirAdj();}
                run.append(p.getUnicode());right=Math.max(right,x+p.getWidthDirAdj());top=Math.min(top,y);bottom=Math.max(bottom,p.getYDirAdj());previous=p;
            }
            if(run.length()>0)boxes.add(new PositionedTableImporter.Box(run.toString(),left,top,right-left,bottom-top));
            super.writeString(text,positions);
        }
    }
}
