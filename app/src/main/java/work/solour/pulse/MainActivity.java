package work.solour.pulse;

import android.Manifest;
import android.app.*;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Small, focused native screens. Stable view tags also identify actual UI controls in tests. */
public class MainActivity extends Activity {
    static final int INK=Color.rgb(24,60,47), MUTED=Color.rgb(112,129,119), BG=Color.rgb(247,249,245),
        ACCENT=Color.rgb(208,240,150), PALE=Color.rgb(234,242,226), WHITE=Color.WHITE, LINE=Color.rgb(229,235,224);
    final Handler ui=new Handler(Looper.getMainLooper());
    final ExecutorService io=Executors.newSingleThreadExecutor();
    SessionService s;
    boolean bound, visible, saving, demo, km;
    String screen="loading", selected="单车", devicesBack="prepare";
    int goal=20, rest=60;
    double manual;
    android.content.SharedPreferences prefs;
    LinearLayout shell, body, footer;
    ScrollView scroll;
    TextView clock, heart, cadence, distance, sessionState, deviceState, restClock, restState, groupState, audioState;
    Button pauseButton, groupButton, restPause, scanButton;
    LinearLayout deviceList;
    String shownDevices="";
    int observedRest;
    JSONObject currentReport;
    Runnable afterPermission;

    final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName n,IBinder b){
            s=((SessionService.Local)b).get(); bound=true;
            if(s.e.running) training(); else home();
        }
        public void onServiceDisconnected(ComponentName n){bound=false;note("训练服务已断开，请重新打开应用。尚未保存的数据可能丢失。");}
    };
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("pulse-preferences",MODE_PRIVATE);
        selected=prefs.getString("scene","单车"); demo=prefs.getBoolean("demo",false);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|(Build.VERSION.SDK_INT>=26?View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0));
        page("loading","同频 PULSE",null,null); label(body,"正在准备你的运动空间…",17,MUTED);
        bindService(new Intent(this,SessionService.class),connection,BIND_AUTO_CREATE);
    }
    @Override protected void onResume(){super.onResume();visible=true;ui.removeCallbacks(refresh);ui.post(refresh);}
    @Override protected void onPause(){visible=false;ui.removeCallbacks(refresh);super.onPause();}
    @Override public void onDestroy(){ui.removeCallbacksAndMessages(null);if(bound)unbindService(connection);io.shutdown();super.onDestroy();}
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    LinearLayout col(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    void ripple(View v,int color,int radius){v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22183C2F),bg(color,radius),null));}
    TextView label(LinearLayout parent,String text,int size,int color){
        TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);parent.addView(t);return t;
    }
    void gap(LinearLayout parent,int h){Space v=new Space(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    void bold(TextView t){t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));}
    void line(LinearLayout p){View v=new View(this);v.setBackgroundColor(LINE);p.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    Button button(LinearLayout p,String tag,String text,boolean primary,Runnable action){
        Button b=new Button(this);b.setTag(tag);b.setText(text);b.setTextSize(16);b.setAllCaps(false);b.setTextColor(primary?INK:INK);
        b.setTypeface(Typeface.create("sans-serif-medium",0));b.setElevation(0);b.setMinHeight(0);b.setMinimumHeight(0);
        b.setStateListAnimator(null);b.setPadding(dp(12),0,dp(12),0);ripple(b,primary?ACCENT:PALE,18);
        p.addView(b,new LinearLayout.LayoutParams(-1,dp(54)));b.setOnClickListener(v->{if(bound&&!saving)action.run();});return b;
    }
    TextView link(LinearLayout p,String tag,String text,Runnable action){
        TextView t=label(p,text,14,MUTED);t.setTag(tag);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));ripple(t,BG,12);
        t.setOnClickListener(v->{if(bound&&!saving)action.run();});return t;
    }
    LinearLayout card(LinearLayout p,int color){
        LinearLayout c=col();c.setPadding(dp(20),dp(20),dp(20),dp(20));c.setBackground(bg(color,24));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);p.addView(c,lp);return c;
    }
    void item(LinearLayout p,String tag,String title,String value,Runnable action){
        LinearLayout r=row();r.setTag(tag);r.setPadding(dp(18),dp(16),dp(14),dp(16));ripple(r,WHITE,20);r.setMinimumHeight(dp(76));
        LinearLayout txt=col();TextView head=label(txt,title,16,INK);bold(head);if(value!=null&&!value.isEmpty()){gap(txt,5);label(txt,value,13,MUTED);}
        r.addView(txt,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=new TextView(this);arrow.setText("›");arrow.setTextSize(26);arrow.setTextColor(MUTED);r.addView(arrow);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);p.addView(r,lp);
        r.setOnClickListener(v->{if(bound&&!saving)action.run();});
    }
    void page(String route,String title,Runnable back,Runnable more){
        if(getCurrentFocus()!=null)((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
        screen=route;clock=heart=cadence=distance=sessionState=deviceState=restClock=restState=groupState=audioState=null;
        pauseButton=groupButton=restPause=scanButton=null;deviceList=null;
        shell=col();shell.setBackgroundColor(BG);setContentView(shell);
        LinearLayout bar=row();bar.setPadding(dp(16),dp(4),dp(16),dp(4));
        if(back!=null){TextView b=new TextView(this);b.setText("‹");b.setTextSize(32);b.setGravity(Gravity.CENTER);b.setTextColor(INK);b.setTag("nav_back");b.setContentDescription("返回");ripple(b,BG,16);bar.addView(b,new LinearLayout.LayoutParams(dp(44),dp(48)));b.setOnClickListener(v->{if(!saving)back.run();});}
        TextView heading=new TextView(this);heading.setText(title);heading.setTextColor(INK);heading.setTextSize(back==null?22:17);bold(heading);bar.addView(heading,new LinearLayout.LayoutParams(0,dp(48),1));heading.setGravity(Gravity.CENTER_VERTICAL);
        if(more!=null){TextView m=new TextView(this);m.setText("···");m.setTextSize(25);m.setTextColor(INK);m.setGravity(Gravity.CENTER);m.setTag("nav_more");m.setContentDescription("更多选项");ripple(m,BG,16);bar.addView(m,new LinearLayout.LayoutParams(dp(48),dp(48)));m.setOnClickListener(v->more.run());}
        shell.addView(bar,new LinearLayout.LayoutParams(-1,dp(60)));
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        body=col();body.setPadding(dp(24),dp(14),dp(24),dp(20));scroll.addView(body);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        footer=col();footer.setPadding(dp(24),dp(12),dp(24),dp(16));shell.addView(footer,new LinearLayout.LayoutParams(-1,-2));
    }
    void tabs(String active){
        footer.removeAllViews();footer.setPadding(dp(12),dp(4),dp(12),dp(8));line(footer);LinearLayout nav=row();footer.addView(nav);
        String[] names={"训练","记录","我的"},tags={"tab_train","tab_history","tab_settings"};
        for(int i=0;i<3;i++){final int index=i;TextView t=new TextView(this);t.setTag(tags[i]);t.setText(names[i]);t.setTextSize(15);t.setGravity(Gravity.CENTER);t.setTextColor(active.equals(names[i])?INK:MUTED);if(active.equals(names[i]))bold(t);ripple(t,active.equals(names[i])?PALE:BG,18);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1);lp.setMargins(dp(4),dp(8),dp(4),0);nav.addView(t,lp);t.setOnClickListener(v->{if(index==0){if(s.e.running)training();else home();}else if(index==1)history();else settings();});}
    }
    String sceneTitle(){return selected.equals("单车")?"动感单车":selected;}
    void title(String eyebrow,String title,String subtitle){label(body,eyebrow,12,MUTED);gap(body,10);TextView t=label(body,title,29,INK);bold(t);gap(body,10);if(subtitle!=null)label(body,subtitle,14,MUTED);gap(body,24);}
    void home(){
        page("home","同频 PULSE",null,null);title("MOVE AT YOUR PACE","今天，动一下。","不用赶上谁，跟着自己的节奏就好。");if(s.e.running)item(body,"resume_session","训练进行中",s.e.scene+" · 点击继续查看",()->training());
        LinearLayout hero=card(body,PALE);LinearLayout hr=row();LinearLayout words=col();bold(label(words,"每一次开始",20,INK));label(words,"都值得被陪伴",20,INK);gap(words,14);label(words,"呼吸 · 节奏 · 同频",12,MUTED);hr.addView(words,new LinearLayout.LayoutParams(0,-2,1));hr.addView(new SceneArt(this,"单车",false),new LinearLayout.LayoutParams(dp(126),dp(125)));hero.addView(hr);
        gap(body,10);bold(label(body,"选择今天的运动",17,INK));gap(body,16);
        sceneCard("单车","动感单车","跟随心跳，找到骑行的节奏","01");sceneCard("跑步机","跑步机","一步一步，把时间留给自己","02");sceneCard("器械","器械训练","专注一组，认真休息","03");tabs("训练");
    }
    void sceneCard(String value,String name,String sub,String n){
        LinearLayout r=row();r.setTag("scene_"+value);r.setPadding(dp(18),dp(16),dp(18),dp(16));ripple(r,WHITE,22);
        TextView number=new TextView(this);number.setText(n);number.setTextColor(MUTED);number.setTextSize(13);r.addView(number,new LinearLayout.LayoutParams(dp(35),-2));
        LinearLayout words=col();bold(label(words,name,18,INK));gap(words,5);label(words,sub,12,MUTED);r.addView(words,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=new TextView(this);arrow.setText("↗");arrow.setTextColor(INK);arrow.setTextSize(23);r.addView(arrow);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(92));lp.bottomMargin=dp(12);body.addView(r,lp);r.setOnClickListener(v->{if(s.e.running){training();return;}selected=value;rest=prefs.getInt("rest_"+value,value.equals("单车")?60:value.equals("跑步机")?120:90);goal=value.equals("器械")?4:20;km=false;manual=0;prefs.edit().putString("scene",value).apply();prepare();});
    }
    void prepare(){
        page("prepare",sceneTitle(),()->home(),null);title("READY WHEN YOU ARE","按你的节奏开始","目标和陪伴，都可以轻一点。");
        LinearLayout art=card(body,PALE);art.addView(new SceneArt(this,selected,false),new LinearLayout.LayoutParams(-1,dp(110)));
        item(body,"setup_goal","本次目标",goal==0?"自由训练":goal+(selected.equals("器械")?" 组":km?" 公里":" 分钟"),()->goalEditor());
        item(body,"setup_device","运动设备",demo?"模拟模式 · 数据仅作演示":s.ble.status,()->{devicesBack="prepare";devices();});
        item(body,"setup_mode","数据模式",demo?"模拟演示":"真实测量",()->modeEditor());
        item(body,"setup_more","训练偏好","休息 "+rest+" 秒 · "+styleName(),()->preferences());
        if(demo)label(body,"模拟数据不代表真实运动，报告会单独标记。",12,MUTED);
        button(footer,"start_session","开始训练",true,()->startSession());
    }
    String styleName(){return new String[]{"温柔陪伴","活力搭子","专业教练"}[prefs.getInt("style",0)];}
    void goalEditor(){
        page("goal","本次目标",()->prepare(),null);title("A LITTLE DIRECTION","定一个小目标","达到后只提醒，训练由你决定何时结束。");
        if(selected.equals("跑步机")){item(body,"goal_time","按时长",km?"":"当前选择",()->{km=false;goalEditor();});item(body,"goal_distance","按里程",km?"当前选择":"",()->{km=true;goalEditor();});}
        EditText input=input(body,"goal_input",selected.equals("器械")?"已确认组数":km?"公里":"分钟",""+goal,false);
        label(body,"输入 0 为自由训练。器械推测组确认后才计入目标。",13,MUTED);
        button(footer,"save_goal","保存目标",true,()->{try{int v=Integer.parseInt(input.getText().toString());if(v<0||v>999)throw new Exception();goal=v;prepare();}catch(Exception ex){input.setError("请输入 0–999 的整数");}});
    }
    EditText input(LinearLayout p,String tag,String label,String value,boolean decimal){
        label(p,label,14,MUTED);gap(p,12);EditText x=new EditText(this);x.setTag(tag);x.setText(value);x.setTextSize(32);x.setTextColor(INK);x.setSingleLine(true);x.setInputType(decimal?8194:2);x.setPadding(dp(18),dp(12),dp(18),dp(12));x.setBackground(bg(WHITE,20));p.addView(x,new LinearLayout.LayoutParams(-1,dp(80)));gap(p,20);return x;
    }
    void modeEditor(){
        page("mode","数据模式",()->prepare(),null);title("CLEAR & HONEST","每一份数据，都说清楚","断连不会自动切换成模拟数据。");
        item(body,"mode_real","真实测量",demo?"连接标准 BLE 设备获取数据":"已选择 · 无数据时显示缺失",()->{demo=false;prefs.edit().putBoolean("demo",false).apply();prepare();});
        item(body,"mode_demo","模拟演示",demo?"已选择 · 所有数据仅作演示":"无需设备，体验完整训练流程",()->{demo=true;prefs.edit().putBoolean("demo",true).apply();prepare();});
    }
    void preferences(){
        page("preferences","训练偏好",()->prepare(),null);item(body,"pref_rest","默认休息",rest+" 秒",()->numberEditor("rest_setup"));
        item(body,"pref_voice","陪伴风格",styleName(),()->voiceEditor("preferences"));
        if(selected.equals("跑步机"))item(body,"pref_speed","手动速度",manual>0?manual+" km/h · 估算":"未设置 · 仅无设备速度时使用",()->numberEditor("speed_setup"));
        label(body,"心率提醒和声音开关可在「我的」中设置。",13,MUTED);
    }
    void startSession(){
        if(s.e.running){training();return;}
        try{s.begin(selected,demo,prefs.getInt("alert",170),rest,goal,km,prefs.getInt("style",0),manual);s.audio.music=prefs.getBoolean("music",true);observedRest=0;training();}
        catch(Exception ex){note("暂时无法开始训练："+ex.getClass().getSimpleName()+"。请检查系统权限后重试。");}
    }
    void permissionScan(){
        String[] permissions=Build.VERSION.SDK_INT>=31?new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT}:new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        for(String p:permissions)if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED){afterPermission=()->startScan();s.ble.status="等待系统权限授权…";if(deviceState!=null)deviceState.setText(s.ble.status);requestPermissions(permissions,7);return;}startScan();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);if(request!=7)return;boolean granted=results.length>0;for(int r:results)granted&=r==PackageManager.PERMISSION_GRANTED;
        Runnable next=afterPermission;afterPermission=null;if(granted&&next!=null)next.run();else{s.ble.status="权限未授予 · 可在系统设置开启";if(deviceState!=null)deviceState.setText(s.ble.status);new AlertDialog.Builder(this).setTitle("需要附近设备权限").setMessage("扫描需蓝牙权限。Android 8–11 还需位置权限及系统定位。可再次扫描授权，或前往系统设置开启。").setPositiveButton("系统设置",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())))).setNegativeButton("返回",null).show();}
    }
    void devices(){
        page("devices","连接设备",()->{s.ble.stopScan();if(devicesBack.equals("prepare"))prepare();else options();},null);
        title("STAY CONNECTED","连接你的运动节奏","支持开放标准 BLE 服务的心率与运动设备。");
        LinearLayout c=card(body,PALE);deviceState=label(c,demo&&!s.e.running?"已选择模拟模式；可返回更换为真实测量。":s.ble.status,15,INK);gap(body,6);
        deviceList=col();body.addView(deviceList);shownDevices="";renderDevices();
        label(body,"华为、小米为适配目标，未经全型号验证。Android 8–11 请同时开启系统定位。",12,MUTED);
        scanButton=button(footer,"scan_devices","扫描附近设备",true,()->permissionScan());
    }
    void startScan(){s.ble.scan();if(deviceState!=null)deviceState.setText(s.ble.status);renderDevices();}
    void renderDevices(){
        if(deviceList==null)return;String key=""+s.ble.found.size()+s.ble.status;if(key.equals(shownDevices))return;shownDevices=key;deviceList.removeAllViews();
        if(s.ble.found.isEmpty()){LinearLayout empty=card(deviceList,WHITE);label(empty,"还没有发现设备",17,INK);gap(empty,8);label(empty,"让设备进入可连接状态，然后点击下方扫描。扫描结果会直接出现在这里。",13,MUTED);}
        for(int i=0;i<s.ble.found.size();i++){final int index=i;BluetoothDevice d=s.ble.found.get(i);String name="未命名设备",address="";try{if(d.getName()!=null)name=d.getName();address=d.getAddress();}catch(SecurityException ignored){}item(deviceList,"connect_"+i,name,address+" · 点击连接",()->{s.ble.connect(index);deviceState.setText(s.ble.status);});}
    }
    void training(){
        if(!s.e.running){home();return;}if(s.e.rest>0){restScreen();return;}
        page("training",s.e.scene.equals("单车")?"动感单车":s.e.scene,()->leaveTraining(),()->options());
        label(body,s.e.simulated?"模拟模式 · 非真实测量":"真实测量 · 按自己的节奏",12,MUTED);gap(body,16);
        clock=label(body,time(s.e.seconds),56,INK);clock.setTypeface(Typeface.create("sans-serif-light",0));sessionState=label(body,"正在训练",14,MUTED);gap(body,12);
        LinearLayout a=card(body,PALE);a.addView(new SceneArt(this,s.e.scene,true),new LinearLayout.LayoutParams(-1,dp(92)));
        LinearLayout metrics=row();LinearLayout h=col(),cad=col();label(h,"心率 · BPM",12,MUTED);heart=label(h,"—",28,INK);label(cad,s.e.scene.equals("单车")?"踏频 · rpm":"步频 · spm",12,MUTED);cadence=label(cad,"—",28,INK);metrics.addView(h,new LinearLayout.LayoutParams(0,-2,1));metrics.addView(cad,new LinearLayout.LayoutParams(0,-2,1));body.addView(metrics);gap(body,18);
        if(s.e.scene.equals("跑步机")){distance=label(body,"",18,INK);gap(body,12);}
        if(s.e.scene.equals("器械")){groupState=label(body,"",16,INK);link(body,"open_groups","查看组记录 ›",()->groups());label(body,"自动辅助结果为推测，确认后才计入目标。",12,MUTED);}
        pauseButton=button(footer,"pause_session",s.e.paused?"继续训练":"暂停训练",true,()->togglePause());gap(footer,10);
        if(s.e.scene.equals("器械"))groupButton=button(footer,"group_action",s.e.groupStart<0?"开始本组":"完成本组",false,()->groupAction());
        else button(footer,"begin_rest",s.e.rest>0?"查看恢复":"主动恢复",false,()->{if(s.e.rest==0)s.e.startRest();restScreen();});
        updateValues();
    }
    String time(int seconds){return String.format(Locale.CHINA,"%02d:%02d",seconds/60,seconds%60);}
    void togglePause(){if(s.e.paused)s.audio.request();s.pause();updateValues();}
    void groupAction(){
        if(s.e.paused){note("训练已暂停。请先点击继续训练。");return;}
        if(s.e.groupStart<0){s.e.startGroup(false);updateValues();}
        else{s.e.finishGroup();if(!s.e.warning)s.audio.say("本组已记录，现在休息。",false);restScreen();}
    }
    void leaveTraining(){new AlertDialog.Builder(this).setTitle("训练还在继续").setMessage("返回首页后仍会采集和计时。你也可以结束并保存本次训练。").setPositiveButton("回到训练",(d,w)->training()).setNeutralButton("结束训练",(d,w)->endConfirm()).setNegativeButton("返回首页",(d,w)->resumeHome()).show();}
    void resumeHome(){home();}
    void options(){
        page("options","训练选项",()->training(),null);
        item(body,"option_device","运动设备",s.e.simulated?"模拟模式 · 设备不参与测量":s.ble.status,()->{devicesBack="options";devices();});
        item(body,"option_music","音乐与陪伴",s.audio.music?"音乐已开启":"音乐已关闭",()->soundScreen("options"));
        item(body,"option_rest","休息时长",s.e.restDefault+" 秒",()->numberEditor("rest_live"));
        if(s.e.scene.equals("跑步机"))item(body,"option_speed","手动速度",s.e.manualKmh+" km/h · 无设备速度时估算",()->numberEditor("speed_live"));
        if(s.e.scene.equals("器械")){item(body,"option_groups","组记录",s.e.confirmed+" 已确认 · "+s.e.pending()+" 待确认",()->groups());if(s.e.groupStart>=0)item(body,"discard_group","丢弃当前组","未完成，不计入目标",()->new AlertDialog.Builder(this).setTitle("丢弃未完成组？").setMessage("已保存的组不会受影响。").setPositiveButton("丢弃",(d,w)->{s.e.groupStart=-1;s.e.rise=s.e.fall=0;training();}).setNegativeButton("取消",null).show());}
        button(footer,"end_session","结束并保存",true,()->endConfirm());
    }
    void restScreen(){
        page("rest","轻松恢复",()->trainingWithoutRestRedirect(),()->options());
        title("TAKE A BREATH","歇一会，也很好。","休息结束只提醒，不会自动开始下一组。");
        restClock=label(body,time(s.e.rest),64,INK);restClock.setGravity(Gravity.CENTER);restClock.setTypeface(Typeface.create("sans-serif-light",0));gap(body,12);
        restState=label(body,"放松肩膀，慢慢呼吸。",14,MUTED);restState.setGravity(Gravity.CENTER);gap(body,20);
        LinearLayout art=card(body,PALE);art.addView(new BreathArt(this),new LinearLayout.LayoutParams(-1,dp(90)));
        restPause=button(body,"pause_rest","暂停倒计时",false,()->{s.e.restPaused=!s.e.restPaused;updateValues();});gap(body,10);
        link(body,"extend_rest","延长 30 秒",()->{s.e.rest=Math.min(600,s.e.rest+30);updateValues();});
        button(footer,"exit_rest","回到训练",true,()->{s.e.rest=0;s.e.rise=s.e.fall=0;training();});updateValues();
    }
    void trainingWithoutRestRedirect(){int left=s.e.rest;s.e.rest=0;training();s.e.rest=left;observedRest=left;updateValues();}
    void groups(){
        page("groups","本次组记录",()->training(),null);title("ONE SET AT A TIME","每一组，都看得见。",s.e.confirmed+" 组已确认 · "+s.e.pending()+" 组待确认");
        if(s.e.groups.isEmpty()){LinearLayout empty=card(body,WHITE);label(empty,"还没有完成的组",19,INK);gap(empty,10);label(empty,"回到训练，手动开始并完成一组，或等待自动辅助推测。未完成的组不会出现在这里。",14,MUTED);}
        for(int i=0;i<s.e.groups.size();i++){final int index=i;Engine.Group g=s.e.groups.get(i);item(body,"group_"+i,"第 "+(i+1)+" 组 · "+g.duration+" 秒",(g.guessed?"推测 · ":"手动 · ")+(g.confirmed?"已确认":"待确认"),()->groupDetail(index));}
        label(body,"推测为未经实机验证的启发式，不代表次数、负重或动作质量。",12,MUTED);
    }
    void groupDetail(int index){
        if(index>=s.e.groups.size()){groups();return;}Engine.Group g=s.e.groups.get(index);page("group_detail","第 "+(index+1)+" 组",()->groups(),null);
        title(g.guessed?"推测记录":"手动记录",g.duration+" 秒",g.confirmed?"已确认，计入本次目标。":"待确认，暂不计入目标。");
        label(body,"开始于训练第 "+g.start+" 秒。你可以保留这组，也可以删除误判。",15,MUTED);
        if(!g.confirmed){button(footer,"confirm_group","确认这组",true,()->{s.e.confirm(index);if(!s.e.warning)s.audio.say("已确认本组。"+s.audio.encouragement(),false);groups();});gap(footer,8);}
        button(footer,"delete_group","删除这组",false,()->new AlertDialog.Builder(this).setTitle("删除这组？").setMessage("删除后将重新计算已确认组数。").setPositiveButton("删除",(d,w)->{s.e.delete(index);groups();}).setNegativeButton("取消",null).show());
    }
    void endConfirm(){
        if(saving)return;
        new AlertDialog.Builder(this).setTitle("收下今天的努力？").setMessage("结束后保存本次报告。未完成组不会算作完成，推测组会保留待确认标记。").setPositiveButton("结束并保存",(d,w)->saveSession()).setNegativeButton("继续训练",null).show();
    }
    void saveSession(){
        boolean wasPaused=s.e.paused;if(!wasPaused)s.pause();JSONObject snapshot;
        try{snapshot=s.report();}catch(Exception ex){note("无法生成报告，请重试。");if(!wasPaused)s.pause();return;}
        saving=true;page("saving","保存训练",null,null);label(body,"正在把今天的努力，留在本机…",18,INK);
        io.execute(()->{try{s.persistReport(snapshot);ui.post(()->{s.lastReport=snapshot.toString();s.completeFinish();saving=false;if(!isFinishing()&&!isDestroyed())showReport(snapshot);});}
        catch(Exception ex){ui.post(()->{saving=false;if(!wasPaused)s.pause();if(!isFinishing()&&!isDestroyed()){training();note("保存失败，训练仍然保留。请检查可用存储后重试。");}});}});
    }
    void settings(){
        page("settings","我的",null,null);title("A LITTLE MORE YOU","舒服的陪伴，刚刚好。","偏好保存在本机，下次训练继续使用。");
        item(body,"settings_sound","音乐与陪伴",styleName(),()->soundScreen("settings"));
        item(body,"settings_alert","心率提醒",prefs.getInt("alert",170)+" BPM · 参考值",()->numberEditor("alert"));
        item(body,"settings_about","关于同频","版本 1.1.1 · 使用说明与数据隐私",()->about());tabs("我的");
    }
    void soundScreen(String from){
        page("sound","音乐与陪伴",()->{if(from.equals("options"))options();else settings();},null);
        boolean on=s.e.running?s.audio.music:prefs.getBoolean("music",true);
        item(body,"toggle_music","离线节奏音乐",on?"已开启 · 点击关闭":"已关闭 · 点击开启",()->{boolean next=!(s.e.running?s.audio.music:prefs.getBoolean("music",true));prefs.edit().putBoolean("music",next).apply();s.audio.music=next;soundScreen(from);});
        item(body,"sound_voice","陪伴风格",styleName(),()->voiceEditor("sound_"+from));
        LinearLayout c=card(body,PALE);audioState=label(c,s.audio.ttsStatus,14,INK);gap(c,10);label(c,"中文播报依赖系统离线中文语音包。三种风格改变措辞和语速，使用系统音色。",13,MUTED);
        link(body,"tts_settings","打开系统语音设置 ›",()->{try{startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}catch(ActivityNotFoundException ex){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){note("此设备没有可打开的系统设置，请在手机设置中搜索文字转语音。");}}});
    }
    void voiceEditor(String back){
        page("voice","陪伴风格",()->voiceBack(back),null);String[] names={"温柔陪伴","活力搭子","专业教练"},subs={"慢一点，也没关系。","稳住，我们一起完成！","关注呼吸，保持可控强度。"};
        for(int i=0;i<3;i++){final int index=i;item(body,"voice_"+i,names[i],(prefs.getInt("style",0)==i?"当前选择 · ":"")+subs[i],()->{prefs.edit().putInt("style",index).apply();s.style=index;voiceBack(back);});}
    }
    void voiceBack(String back){if(back.equals("preferences"))preferences();else soundScreen(back.endsWith("options")?"options":"settings");}
    void numberEditor(String type){
        boolean isRest=type.startsWith("rest"),isSpeed=type.startsWith("speed"),live=type.endsWith("live");
        Runnable back=()->{if(type.equals("alert"))settings();else if(live)options();else preferences();};
        page("number_"+type,isRest?"休息时长":isSpeed?"手动速度":"心率提醒",back,null);
        title(isRest?"REST WELL":isSpeed?"ESTIMATED DISTANCE":"LISTEN TO YOUR BODY",isRest?"让恢复也有节奏。":isSpeed?"这份里程，标为估算。":"关注身体的提醒。",isRest?"30–600 秒，下次休息生效。":isSpeed?"仅在设备速度缺失时使用；输入 0 关闭估算。":"默认 170 仅为参考，不代表通用安全阈值。");
        String initial=isRest?""+(live?s.e.restDefault:rest):isSpeed?""+(live?s.e.manualKmh:manual):""+prefs.getInt("alert",170);
        EditText x=input(body,"number_input",isRest?"秒":isSpeed?"km/h":"BPM",initial,isSpeed);
        button(footer,"save_number","保存",true,()->{try{double n=Double.parseDouble(x.getText().toString());if(!Double.isFinite(n)||(isRest?(n<30||n>600||n%1!=0):isSpeed?(n<0||n>30):(n<60||n>240||n%1!=0)))throw new Exception();
            if(isRest){if(live)s.e.restDefault=(int)n;else{rest=(int)n;prefs.edit().putInt("rest_"+selected,rest).apply();}}else if(isSpeed){if(live)s.e.manualKmh=n;else manual=n;}else{prefs.edit().putInt("alert",(int)n).apply();if(s.e.running)s.e.alert=(int)n;}back.run();
        }catch(Exception ex){x.setError(isRest?"请输入 30–600 的整数":isSpeed?"请输入 0–30 的有效速度":"请输入 60–240 的整数");}});
    }
    void about(){
        page("about","关于同频",()->settings(),null);title("PULSE · 1.1.1","把节奏，留给自己。","每一份记录只属于你。");
        LinearLayout c=card(body,WHITE);label(c,"数据与隐私",18,INK);gap(c,10);label(c,"无需登录，无网络权限，不上传健康数据。本地保留最近100份报告，可删除。卸载或清除应用数据会删除报告。",14,MUTED);
        LinearLayout b=card(body,WHITE);label(b,"能力边界",18,INK);gap(b,10);label(b,"标准BLE心率、可选RSC速度/步频和CSC踏频。华为、小米是适配目标，未经全型号或实机验证。自动判组仅为推测。心率超过10秒或速度超过5秒无更新时停止使用过期数据。",14,MUTED);
        LinearLayout d=card(body,WHITE);label(d,"使用须知",18,INK);gap(d,10);label(d,"锁屏训练使用前台服务；系统省电与强杀仍可能中止。音频焦点丢失、来电或耳机断开会暂停，需手动继续。中文语音须系统支持离线中文。个人曲库、云端大模型鼓励、真人音色及厂商私有SDK尚未实现。",14,MUTED);
    }
    void history(){
        page("history","运动记录",null,null);title("EVERY EFFORT COUNTS","你的每一步，都在这里。","最近100份报告，仅保存在本机。");LinearLayout list=col();body.addView(list);label(list,"正在读取记录…",14,MUTED);tabs("记录");
        io.execute(()->{JSONArray all=s.reports();ui.post(()->{if(isFinishing()||!screen.equals("history"))return;list.removeAllViews();if(all.length()==0){LinearLayout empty=card(list,WHITE);label(empty,"还没有运动记录",20,INK);gap(empty,10);label(empty,"完成一次训练后，时长、心率和场景记录会出现在这里。",14,MUTED);}for(int i=0;i<all.length();i++){final JSONObject r=all.optJSONObject(i);if(r==null)continue;String date=new java.text.SimpleDateFormat("MM月dd日  HH:mm",Locale.CHINA).format(new Date(r.optLong("date")));item(list,"report_"+i,r.optString("scene")+" · "+time(r.optInt("seconds")),date+(r.optBoolean("simulation")?" · 模拟":""),()->showReport(r));}});});
    }
    String val(JSONObject r,String key){return r.isNull(key)||!r.has(key)?"—":r.optString(key);}
    void showReport(JSONObject r){
        currentReport=r;page("report","训练报告",()->history(),()->reportOptions());title(r.optBoolean("simulation")?"模拟数据 · 不代表真实运动":"本地训练报告","今天的努力，已收下。",r.optString("scene"));
        LinearLayout top=card(body,PALE);label(top,"训练时长",13,MUTED);label(top,time(r.optInt("seconds")),44,INK);gap(top,14);label(top,"平均心率 "+val(r,"avg")+"  /  最高 "+val(r,"max")+" BPM",16,INK);gap(top,8);label(top,"有效心率覆盖 "+(r.optInt("seconds")==0?0:r.optInt("valid")*100/r.optInt("seconds"))+"%",13,MUTED);
        if(r.optString("scene").equals("跑步机")){LinearLayout c=card(body,WHITE);label(c,String.format(Locale.CHINA,"%.3f km",r.optLong("metres")/1000.0),27,INK);label(c,r.optString("distanceLabel"),13,MUTED);}
        label(body,"平均步频 / 踏频  "+val(r,"cadence"),15,MUTED);gap(body,20);bold(label(body,"心率轨迹",18,INK));gap(body,10);LinearLayout chart=card(body,WHITE);chart.addView(new HeartChart(this,r.optJSONArray("curve")),new LinearLayout.LayoutParams(-1,dp(140)));label(chart,"横轴为活动训练时间，缺失数据处断开。",11,MUTED);
        if(r.optString("scene").equals("器械")){label(body,"已确认 "+r.optInt("confirmed")+" 组 · 待确认 "+r.optInt("pending")+" 组",17,INK);if(r.optBoolean("unfinished"))label(body,"未完成的当前组已排除。",12,MUTED);JSONArray a=r.optJSONArray("groups");if(a!=null)for(int i=0;i<a.length();i++){JSONObject g=a.optJSONObject(i);if(g!=null){LinearLayout c=card(body,WHITE);label(c,"第 "+(i+1)+" 组 · "+g.optInt("duration")+" 秒",16,INK);label(c,(g.optBoolean("guessed")?"推测 · ":"手动 · ")+(g.optBoolean("confirmed")?"已确认":"待确认")+" · 开始于 "+g.optInt("start")+" 秒",12,MUTED);}}}
        button(footer,"report_done","完成",true,()->{if(s.e.running)training();else home();});
    }
    void reportOptions(){page("report_options","报告选项",()->showReport(currentReport),null);label(body,"报告只保存在当前设备，删除后无法恢复。",15,MUTED);button(footer,"delete_report","删除这份报告",false,()->new AlertDialog.Builder(this).setTitle("删除这份报告？").setMessage("此操作无法撤销。").setPositiveButton("删除",(d,w)->deleteReport()).setNegativeButton("取消",null).show());}
    String key(JSONObject r){return r.optString("id",r.optLong("date")+":"+r.optString("scene"));}
    void deleteReport(){
        saving=true;String id=key(currentReport);io.execute(()->{try{JSONArray all=s.reports(),next=new JSONArray();for(int i=0;i<all.length();i++){JSONObject r=all.optJSONObject(i);if(r!=null&&!key(r).equals(id))next.put(r);}s.save(next);ui.post(()->{saving=false;if(!isFinishing())history();});}catch(Exception ex){ui.post(()->{saving=false;note("删除失败，请稍后重试。");});}});
    }
    void note(String message){new AlertDialog.Builder(this).setMessage(message).setPositiveButton("知道了",null).show();}
    void updateValues(){
        if(!bound)return;Engine e=s.e;long now=SystemClock.elapsedRealtime();
        if(clock!=null)clock.setText(time(e.seconds));
        if(heart!=null){heart.setText(e.freshHr(now)?""+e.hr:"—");heart.setTextColor(e.warning?0xffAD5636:INK);}
        if(cadence!=null)cadence.setText(now>=e.cadenceAt&&now-e.cadenceAt<=5000?""+e.cadence:"—");
        if(sessionState!=null)sessionState.setText(e.warning?"达到警示参考值，请降低强度":e.paused?"已暂停 · 准备好再继续":e.rest>0?"恢复中 · 剩余 "+e.rest+" 秒":!e.freshHr(now)?"心率数据缺失 · 等待设备更新":"正在训练 · 音乐 "+new int[]{88,116,136}[e.band]+" BPM");
        if(distance!=null)distance.setText(String.format(Locale.CHINA,"%.3f km",e.distance/1000)+" · "+e.distanceLabel());
        if(groupState!=null)groupState.setText(e.confirmed+" 组已确认 · "+e.pending()+" 组待确认\n"+(e.groupStart>=0?(e.inferred?"推测组":"手动组")+"进行中 · "+(e.seconds-e.groupStart)+" 秒":"准备好，再开始下一组"));
        if(pauseButton!=null)pauseButton.setText(e.paused?"继续训练":"暂停训练");
        if(groupButton!=null){groupButton.setText(e.groupStart<0?"开始本组":"完成本组");groupButton.setEnabled(!e.paused&&e.rest==0);groupButton.setAlpha(e.paused||e.rest>0?.45f:1f);}
        if(restClock!=null)restClock.setText(time(e.rest));
        if(restState!=null)restState.setText(e.rest==0?"恢复时间到了，准备好后手动返回。":e.paused?"训练暂停中 · 倒计时已冻结":e.restPaused?"倒计时已暂停":"放松肩膀，慢慢呼吸。");
        if(restPause!=null){restPause.setText(e.restPaused?"继续倒计时":"暂停倒计时");restPause.setEnabled(e.rest>0&&!e.paused);}
        if(deviceState!=null)deviceState.setText(s.ble.status);
        if(scanButton!=null){boolean scanning=s.ble.status.startsWith("扫描中");scanButton.setText(scanning?"正在扫描…":"扫描附近设备");scanButton.setEnabled(!scanning);}
        if(audioState!=null)audioState.setText(s.audio.ttsStatus);
        if(screen.equals("devices"))renderDevices();
    }
    final Runnable refresh=new Runnable(){public void run(){if(!visible)return;if(bound){if(!s.e.running&&(screen.equals("training")||screen.equals("rest")||screen.equals("options")||screen.equals("groups"))){try{showReport(new JSONObject(s.lastReport));}catch(Exception ignored){home();}}updateValues();if(screen.equals("training")&&s.e.rest>0&&observedRest==0)restScreen();observedRest=s.e.rest;}ui.postDelayed(this,1000);}};
    @Override public void onBackPressed(){
        if(saving)return;if(!bound){super.onBackPressed();return;}
        switch(screen){case "home":if(s.e.running)training();else super.onBackPressed();break;case "training":case "rest":leaveTraining();break;case "prepare":case "history":case "settings":home();break;case "goal":case "mode":case "preferences":prepare();break;case "devices":s.ble.stopScan();if(devicesBack.equals("prepare"))prepare();else options();break;case "groups":case "options":training();break;case "group_detail":groups();break;case "report":history();break;case "report_options":showReport(currentReport);break;default:View back=shell.findViewWithTag("nav_back");if(back!=null)back.performClick();else home();}
    }
    class SceneArt extends View {
        final String kind;final boolean moving;final Paint p=new Paint(3);SceneArt(Context c,String kind,boolean moving){super(c);this.kind=kind;this.moving=moving;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float scale=Math.min(getWidth()/240f,getHeight()/140f);c.save();c.translate(getWidth()/2f,getHeight()/2f);c.scale(scale,scale);float t=moving?SystemClock.uptimeMillis()/1000f:0;
            p.setColor(0xffDAEBC4);p.setStyle(Paint.Style.FILL);c.drawCircle(36,-8,59,p);p.setColor(0xffA8C881);p.setStrokeWidth(2);c.drawLine(-106,51,106,51,p);p.setColor(INK);p.setStrokeWidth(5);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
            if(kind.equals("单车")){c.drawCircle(-55,24,27,p);c.drawCircle(57,24,27,p);c.drawLine(-55,24,-18,-24,p);c.drawLine(-18,-24,17,24,p);c.drawLine(-55,24,17,24,p);c.drawLine(-7,-8,43,-8,p);c.drawLine(43,-8,57,24,p);c.drawLine(43,-8,34,-28,p);c.drawLine(27,-28,44,-28,p);c.drawLine(-18,-24,-23,-36,p);c.drawLine(-36,-36,-12,-36,p);c.drawLine(17,24,17+(float)Math.cos(t)*14,24+(float)Math.sin(t)*14,p);}
            else if(kind.equals("跑步机")){c.drawLine(-86,43,68,43,p);c.drawLine(68,43,84,-43,p);c.drawLine(62,-43,94,-43,p);c.drawCircle(-13,-43,10,p);c.drawLine(-14,-30,-24,0,p);c.drawLine(-24,0,-53,25+(float)Math.sin(t)*4,p);c.drawLine(-24,0,9,27,p);c.drawLine(-17,-18,12,-6,p);c.drawLine(12,-6,28,-24,p);}
            else{float z=(float)Math.sin(t)*4;c.drawLine(-53,z,53,z,p);c.drawRoundRect(-77,-28+z,-53,28+z,5,5,p);c.drawRoundRect(53,-28+z,77,28+z,5,5,p);c.drawLine(-85,-15+z,-85,15+z,p);c.drawLine(85,-15+z,85,15+z,p);}
            p.setStyle(Paint.Style.FILL);c.restore();if(moving&&isShown()&&visible)postInvalidateDelayed(1200);
        }
    }
    class BreathArt extends View {Paint p=new Paint(3);BreathArt(Context c){super(c);}protected void onDraw(Canvas c){p.setColor(0xffD7E8C3);c.drawCircle(getWidth()/2f,getHeight()/2f,dp(39),p);p.setColor(0xffB5D58D);c.drawCircle(getWidth()/2f,getHeight()/2f,dp(28),p);p.setColor(INK);c.drawCircle(getWidth()/2f,getHeight()/2f,dp(13),p);}}
    class HeartChart extends View {final JSONArray data;final Paint p=new Paint(3);HeartChart(Context c,JSONArray a){super(c);data=a;}protected void onDraw(Canvas c){p.setColor(MUTED);p.setTextSize(dp(10));c.drawText("250",0,dp(12),p);c.drawText("25",0,getHeight()-dp(4),p);p.setColor(LINE);p.setStrokeWidth(dp(1));for(int i=0;i<3;i++)c.drawLine(dp(28),dp(10)+i*(getHeight()-dp(20))/2f,getWidth(),dp(10)+i*(getHeight()-dp(20))/2f,p);if(data==null||data.length()<2){p.setColor(MUTED);c.drawText("等待有效心率数据",dp(40),getHeight()/2f,p);return;}p.setColor(INK);p.setStrokeWidth(dp(2));for(int i=1;i<data.length();i++){int a=data.optInt(i-1),b=data.optInt(i);if(a>0&&b>0)c.drawLine(dp(28)+(i-1)*(getWidth()-dp(28))/(float)(data.length()-1),getHeight()-dp(10)-(a-25)*(getHeight()-dp(20))/225f,dp(28)+i*(getWidth()-dp(28))/(float)(data.length()-1),getHeight()-dp(10)-(b-25)*(getHeight()-dp(20))/225f,p);}}}
}
