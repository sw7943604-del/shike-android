package cn.edu.csu.timetable;

import android.app.*;
import android.os.Bundle;
import android.net.Uri;
import android.webkit.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import cn.edu.csu.timetable.core.ImportResult;
import cn.edu.csu.timetable.core.importer.HtmlImporter;

/** 校内登录浏览器，不向网页暴露任何原生接口。 */
public class PortalActivity extends androidx.activity.ComponentActivity {
    private WebView web;
    private TextView status;
    private boolean reading;
    // 统一认证可能先经过站点首页或其他登录路径，不能只允许课表目录。
    private static boolean allowed(String url) {
        try { Uri u=Uri.parse(url); String h=u.getHost(); if(h==null)return false; boolean secure="https".equalsIgnoreCase(u.getScheme()) && (h.equalsIgnoreCase("csu.edu.cn")||h.toLowerCase(java.util.Locale.ROOT).endsWith(".csu.edu.cn")); boolean legacy="http".equalsIgnoreCase(u.getScheme())&&h.equalsIgnoreCase("csujwc.its.csu.edu.cn"); return secure||legacy; }
        catch(Exception e){return false;}
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(247,245,240));
        status=new TextView(this);status.setPadding(20,16,20,12);status.setText("登录中南大学门户，打开教务系统的学期课表后读取。");root.addView(status);
        LinearLayout bar=new LinearLayout(this);
        Button back=new Button(this);back.setText("返回");back.setOnClickListener(v->{if(web.canGoBack())web.goBack();else finish();});bar.addView(back);
        Button read=new Button(this);read.setText("读取课表");read.setOnClickListener(v->readSchedule());bar.addView(read);
        Button clear=new Button(this);clear.setText("清除登录");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("清除登录状态？").setMessage("将清除此应用内的登录 Cookie、缓存和网页存储。").setNegativeButton("取消",null).setPositiveButton("清除",(d,w)->{CookieManager.getInstance().removeAllCookies(done->{CookieManager.getInstance().flush();web.clearCache(true);web.clearHistory();WebStorage.getInstance().deleteAllData();web.loadUrl("https://my.csu.edu.cn/");});}).show());bar.addView(clear);
        root.addView(bar);Button direct=new Button(this);direct.setText("已登录教务？直接打开本科课表");direct.setOnClickListener(v->web.loadUrl("http://csujwc.its.csu.edu.cn/jsxsd/xskb/xskb_list.do"));root.addView(direct);web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        if(android.os.Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);return insets;});
        else root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        setContentView(root);root.requestApplyInsets();
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){@Override public void handleOnBackPressed(){if(web.canGoBack())web.goBack();else finish();}});
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSaveFormData(false);s.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return blockNavigation(r.getUrl().toString());}
            @Override public boolean shouldOverrideUrlLoading(WebView v,String url){return blockNavigation(url);}
            @Override public void onReceivedSslError(WebView v,android.webkit.SslErrorHandler handler,android.net.http.SslError error){handler.cancel();status.setText("网站证书验证失败，已停止访问。");}
            @Override public void onPageFinished(WebView v,String url){status.setText("当前页面："+Uri.parse(url).getHost()+" · 打开学期课表后点击读取");}
        });
        web.loadUrl("https://my.csu.edu.cn/");
    }
    private boolean blockNavigation(String url) {
        if(allowed(url))return false;
        // 不显示查询参数和片段，避免把登录票据带入诊断提示。
        Uri uri=Uri.parse(url);
        String address=uri.getScheme()+"://"+(uri.getHost()==null?"未知站点":uri.getHost());
        status.setText("已阻止跳转："+address+"。若点击本科生教务后出现此提示，请反馈此地址。");
        return true;
    }
    private void readSchedule(){
        if(reading)return;
        if(!allowed(web.getUrl())){status.setText("请先打开校内教务课表。");return;}
        reading=true;status.setText("正在读取课表…");
        // 只读同源框架；跨域框架请用户直接进入其页面。
        String js="(function(){var out=[],size=0,blocked=0;function walk(w,n){if(n>5)return;try{var h=w.document.documentElement.outerHTML;size+=h.length;if(size>2097152)throw 'large';out.push(h);for(var i=0;i<w.frames.length;i++)walk(w.frames[i],n+1);}catch(e){if(e==='large')throw e;blocked++;}}try{walk(window,0);return JSON.stringify({html:out.join('\\n'),blocked:blocked});}catch(e){return JSON.stringify({error:'课表页面过大，请打开单独的学期课表页面。'});}})()";
        web.evaluateJavascript(js,value->{
            try{
                Object decoded=new JSONTokener(value).nextValue();if(!(decoded instanceof String))throw new Exception("页面尚未加载完成");
                JSONObject payload=new JSONObject((String)decoded);if(payload.has("error"))throw new Exception(payload.getString("error"));
                String html=payload.getString("html");if(html.getBytes(StandardCharsets.UTF_8).length>2*1024*1024)throw new Exception("页面超过 2 MB，请打开单独课表页面");
                ImportResult result=HtmlImporter.parse(html);
                if(result.courses.isEmpty())throw new Exception(result.warnings.get(0)+" 无法读取的网页框架 "+payload.optInt("blocked",0)+"。");
                new CourseStore(this).savePreview(result);MainActivity.pendingImport=result;setResult(RESULT_OK);finish();
            }catch(Exception e){status.setText("读取失败："+e.getMessage());}finally{reading=false;}
        });
    }
    @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
}
