package cn.edu.csu.timetable;
import android.view.*;
import android.webkit.WebView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import java.util.concurrent.*;

@RunWith(AndroidJUnit4.class)
public class PortalTest {
    @Test public void schoolEntryAllowsSsoPathsAndRejectsOutsideHosts() {
        try(ActivityScenario<PortalActivity> scenario=ActivityScenario.launch(PortalActivity.class)){
            scenario.onActivity(a->{
                WebView w=findWeb(a.getWindow().getDecorView());w.stopLoading();
                android.webkit.WebViewClient client=w.getWebViewClient();
                for(String url:new String[]{"http://csujwc.its.csu.edu.cn/","http://csujwc.its.csu.edu.cn/framework/main.jsp","http://csujwc.its.csu.edu.cn/sso/login?ticket=test","https://my.csu.edu.cn/newcsuxszd/index.html#/ServiceShow"})
                    {assertFalse("校内登录入口不应被拦截："+url,client.shouldOverrideUrlLoading(w,url));assertFalse(client.shouldOverrideUrlLoading(w,request(url)));}
                for(String url:new String[]{"https://csu.edu.cn.evil.example/","http://my.csu.edu.cn/","file:///sdcard/a.html","javascript:alert(1)","https://evil.example/?next=csu.edu.cn"})
                    {assertTrue("不应放行此地址："+url,client.shouldOverrideUrlLoading(w,url));assertTrue(client.shouldOverrideUrlLoading(w,request(url)));}
                client.shouldOverrideUrlLoading(w,request("https://outside.example/?ticket=private-ticket#secret"));
                assertFalse(nativeText(a.getWindow().getDecorView()).contains("private-ticket"));
            });
        }
    }
    private android.webkit.WebResourceRequest request(String url){return new android.webkit.WebResourceRequest(){
        public android.net.Uri getUrl(){return android.net.Uri.parse(url);}
        public boolean isForMainFrame(){return true;}public boolean isRedirect(){return false;}public boolean hasGesture(){return true;}
        public String getMethod(){return "GET";}public java.util.Map<String,String> getRequestHeaders(){return java.util.Collections.emptyMap();}
    };}
    private WebView findWeb(View view){if(view instanceof WebView)return (WebView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){WebView w=findWeb(group.getChildAt(i));if(w!=null)return w;}}return null;}
    private String nativeText(View v){if(v instanceof android.widget.TextView)return ((android.widget.TextView)v).getText().toString()+"\n";StringBuilder text=new StringBuilder();if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)text.append(nativeText(g.getChildAt(i)));}return text.toString();}
    @Test public void loadedSchoolHtmlReturnsImportPreview() throws Exception {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.getSharedPreferences("shike",0).edit().clear().commit();MainActivity.pendingImport=null;
        String html="<html><head><title>fixture-ready</title></head><body><select id=xnxq01id><option selected>2026-2027-1</option></select><table style='width:3000px'><tr><td>节次</td><td>星期一&nbsp;</td><td>星期二&nbsp;</td></tr><tr><td>1&nbsp;-&nbsp;2</td><td>数学<br>张老师<br>1-16(周)<br>A101</td><td></td></tr></table><table><tr><td>第1周 2026-09-06日至2026-09-12日</td></tr></table></body></html>";
        try(ActivityScenario<PortalActivity> scenario=ActivityScenario.launch(PortalActivity.class)){
            scenario.onActivity(a->{WebView w=findWeb(a.getWindow().getDecorView());w.stopLoading();w.loadDataWithBaseURL("http://csujwc.its.csu.edu.cn/jsxsd/xskb/xskb_list.do",html,"text/html","UTF-8","http://csujwc.its.csu.edu.cn/jsxsd/xskb/xskb_list.do");});
            boolean loaded=false;for(int i=0;i<50&&!loaded;i++){boolean[] ready={false};scenario.onActivity(a->ready[0]="fixture-ready".equals(findWeb(a.getWindow().getDecorView()).getTitle()));loaded=ready[0];if(!loaded)Thread.sleep(100);}assertTrue("测试课表页面应加载完成",loaded);
            onView(withText("读取课表")).perform(click());
            for(int i=0;i<30&&MainActivity.pendingImport==null;i++)Thread.sleep(100);
            if(MainActivity.pendingImport==null){String[] diagnostics={""};scenario.onActivity(a->diagnostics[0]=nativeText(a.getWindow().getDecorView())+"URL="+findWeb(a.getWindow().getDecorView()).getUrl());fail(diagnostics[0]);}
            assertEquals(1,MainActivity.pendingImport.courses.size());assertEquals("张老师",MainActivity.pendingImport.courses.get(0).teacher);assertEquals("2026-09-06",MainActivity.pendingImport.startDate.toString());assertNotNull(new CourseStore(context).loadPreview());
        }
    }
}
