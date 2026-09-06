package cn.edu.csu.timetable;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;

final class Ui {
    final Context context;
    final boolean dark;
    final int bg, surface, ink, muted, line, accent;
    Ui(Context c, String theme) {
        context=c; dark=theme.equals("dark") || (theme.equals("system") && (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        bg=Color.parseColor(dark?"#171C1A":"#F5F4EF");surface=Color.parseColor(dark?"#242C28":"#FFFFFF");
        ink=Color.parseColor(dark?"#F0F1E9":"#24362E");muted=Color.parseColor(dark?"#B0BCB3":"#737F76");line=Color.parseColor(dark?"#39433C":"#E3E7DF");accent=Color.parseColor(dark?"#A8CCAD":"#426F57");
    }
    int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    TextView text(String s,float size,int color){TextView v=new TextView(context);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("tnum");return v;}
    TextView title(String s,float size){TextView v=text(s,size,ink);v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return v;}
    LinearLayout column(){LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.VERTICAL);return v;}
    LinearLayout row(){LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(android.view.Gravity.CENTER_VERTICAL);return v;}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    TextView button(String s,Runnable action){TextView v=title(s,14);v.setGravity(17);pad(v,16,12);v.setBackground(shape(surface,14));v.setOnClickListener(w->action.run());v.setMinHeight(dp(48));return v;}
    void gap(LinearLayout parent,int h){View v=new View(context);parent.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    LinearLayout card(){LinearLayout c=column();pad(c,18,18);c.setBackground(shape(surface,20));return c;}
}
