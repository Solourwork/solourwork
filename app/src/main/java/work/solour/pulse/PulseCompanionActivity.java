package work.solour.pulse;

import android.app.*;import android.bluetooth.*;import android.content.*;import android.content.res.ColorStateList;
import android.graphics.*;import android.graphics.drawable.*;import android.media.*;import android.os.*;
import android.view.*;import android.widget.*;import org.json.*;import java.util.*;

/** 1.2 product/interaction prototype. Music is real; voice conversations are explicitly scripted demos. */
public class PulseCompanionActivity extends Activity {
    static final int BG=0xff151218,PANEL=0xff26222D,INK=0xffFFF8F3,MUTED=0xffBDB5C7,
        ORANGE=0xffFF6638,PINK=0xffFF3C81,PURPLE=0xff8064FF;
    final Handler ui=new Handler(Looper.getMainLooper());
    SessionService s;boolean bound,visible;String screen="loading";int demoGeneration;boolean demoActive,demoWindowStarted;
    LinearLayout shell,body,footer,deviceRows;TextView liveTitle,liveState,deviceStatus,homeStatus,demoTitle,demoCaption,demoStage;
    Button pauseButton,scanButton;PulseArt art;SharedPreferences prefs;CompanionDialogue dialogue;
    final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName n,IBinder b){s=((SessionService.Local)b).get();bound=true;if(s.e.running&&s.companionMode)live();else home();}
        public void onServiceDisconnected(ComponentName n){bound=false;message("暂时断开连接，请重新打开应用。",null);}
    };
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("pulse-preferences",MODE_PRIVATE);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(0);page("loading","PULSE",null,null);text(body,"正在准备你的节奏…",19,INK);bindService(new Intent(this,SessionService.class),connection,BIND_AUTO_CREATE);}
    @Override protected void onResume(){super.onResume();visible=true;ui.post(tick);}
    @Override protected void onPause(){visible=false;ui.removeCallbacks(tick);if(demoActive)cancelDemo();super.onPause();}
    @Override public void onDestroy(){cancelDemo();ui.removeCallbacksAndMessages(null);if(bound)unbindService(connection);super.onDestroy();}
    int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    GradientDrawable background(int color,int radius){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));return b;}
    void ripple(View v,int color,int radius){v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x44FFFFFF),background(color,radius),null));}
    TextView text(LinearLayout p,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);t.setLineSpacing(dp(4),1);p.addView(t);return t;}
    void bold(TextView t){t.setTypeface(Typeface.create("sans-serif-medium",0));}
    void space(LinearLayout p,int h){p.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(h)));}
    void page(String name,String title,Runnable back,Runnable extra){
        if(!name.equals("demo"))cancelDemo();screen=name;liveTitle=liveState=deviceStatus=homeStatus=demoTitle=demoCaption=demoStage=null;pauseButton=scanButton=null;deviceRows=null;shownDevices="";art=null;
        shell=column();shell.setBackgroundColor(BG);setContentView(shell);LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(16),0,dp(16),0);
        if(back!=null){TextView b=new TextView(this);b.setText("‹");b.setTextColor(INK);b.setTextSize(32);b.setGravity(Gravity.CENTER);b.setTag("nav_back");b.setContentDescription("返回");bar.addView(b,new LinearLayout.LayoutParams(dp(48),dp(52)));ripple(b,BG,16);b.setOnClickListener(v->back.run());}
        TextView h=new TextView(this);h.setText(title);h.setTextColor(INK);h.setTextSize(back==null?24:18);bold(h);bar.addView(h,new LinearLayout.LayoutParams(0,-2,1));
        if(extra!=null){TextView e=new TextView(this);e.setText("偏好");e.setTag("preferences");e.setTextColor(INK);e.setTextSize(14);e.setGravity(Gravity.CENTER);ripple(e,PANEL,14);bar.addView(e,new LinearLayout.LayoutParams(dp(64),dp(44)));e.setOnClickListener(v->extra.run());}
        shell.addView(bar,new LinearLayout.LayoutParams(-1,dp(64)));ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);body=column();body.setPadding(dp(24),dp(12),dp(24),dp(20));scroll.addView(body);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));footer=column();footer.setPadding(dp(24),dp(10),dp(24),dp(18));shell.addView(footer);
    }
    Button button(LinearLayout p,String tag,String label,Runnable action){Button b=new Button(this);b.setTag(tag);b.setText(label);b.setTextSize(18);b.setAllCaps(false);b.setTextColor(BG);bold(b);b.setStateListAnimator(null);b.setElevation(0);ripple(b,ORANGE,20);p.addView(b,new LinearLayout.LayoutParams(-1,dp(60)));b.setOnClickListener(v->{if(bound)action.run();});return b;}
    void link(LinearLayout p,String tag,String label,Runnable action){TextView t=text(p,label,14,MUTED);t.setGravity(Gravity.CENTER);t.setTag(tag);t.setMinHeight(dp(48));ripple(t,BG,12);t.setOnClickListener(v->{if(bound)action.run();});}
    LinearLayout card(LinearLayout p){LinearLayout c=column();c.setPadding(dp(20),dp(18),dp(20),dp(18));c.setBackground(background(PANEL,24));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);p.addView(c,lp);return c;}
    void row(LinearLayout p,String tag,String title,String subtitle,Runnable action){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(18),dp(16),dp(16),dp(16));r.setTag(tag);ripple(r,PANEL,22);LinearLayout words=column();bold(text(words,title,17,INK));space(words,5);text(words,subtitle,13,MUTED);r.addView(words,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=new TextView(this);arrow.setText("↗");arrow.setTextColor(ORANGE);arrow.setTextSize(23);r.addView(arrow);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);p.addView(r,lp);r.setOnClickListener(v->action.run());}
    void headline(String eyebrow,String heading,String sub){text(body,eyebrow,12,ORANGE);space(body,12);bold(text(body,heading,34,INK));space(body,12);text(body,sub,14,MUTED);space(body,24);}
    void badge(LinearLayout p,String value){TextView t=text(p,value,12,ORANGE);t.setPadding(dp(12),dp(7),dp(12),dp(7));t.setBackground(background(0xff39251F,12));}
    void home(){
        page("home","PULSE / 同频",null,()->preferences());headline("YOUR MOVE. YOUR PULSE.","戴好耳机，\n开动就好。","让音乐跟上你，把注意力留给运动。");
        art=new PulseArt(this,false,false);body.addView(art,new LinearLayout.LayoutParams(-1,dp(210)));space(body,18);badge(body,"1.2 交互体验稿 · 完全离线方向");space(body,12);homeStatus=text(body,"",13,MUTED);
        button(footer,"start_companion",s.e.running&&s.companionMode?"回到陪伴":"开始音乐陪伴",()->start());link(footer,"demo_entry","先体验一次语音问答设计  ↗",()->showDemo(CompanionDialogue.Topic.REST,false));refresh();
    }
    boolean headphones(){try{AudioManager m=(AudioManager)getSystemService(AUDIO_SERVICE);for(AudioDeviceInfo d:m.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){int t=d.getType();if(t==AudioDeviceInfo.TYPE_WIRED_HEADSET||t==AudioDeviceInfo.TYPE_WIRED_HEADPHONES||t==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||t==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||t==AudioDeviceInfo.TYPE_USB_HEADSET||t==AudioDeviceInfo.TYPE_BLE_HEADSET)return true;}}catch(Exception ignored){}return false;}
    void start(){if(s.e.running&&s.companionMode){live();return;}if(!headphones()){new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert).setTitle("还没检测到耳机").setMessage("可以先用扬声器试听。此体验稿不会录音，语音问答在演示入口中体验。").setPositiveButton("试听开始",(d,w)->begin()).setNegativeButton("稍后",null).show();}else begin();}
    void begin(){try{s.companionMusic=prefs.getInt("companion_music",0);s.audio.music=true;s.beginCompanion();live();}catch(Exception ex){message("暂时没能开始，请检查系统权限与后台运行设置。",null);}}
    void live(){
        page("live","音乐陪伴中",()->home(),()->preferences());badge(body,"音乐陪伴 · 语音问答为演示");space(body,22);
        liveTitle=text(body,s.e.paused?"音乐先停一下。\n随时回来继续。":"跟着节奏。\n其他先放一边。",32,INK);bold(liveTitle);space(body,20);
        art=new PulseArt(this,true,s.e.paused);body.addView(art,new LinearLayout.LayoutParams(-1,dp(220)));space(body,20);liveState=text(body,"",14,MUTED);
        pauseButton=button(footer,"pause_companion",s.e.paused?"继续陪伴":"暂停音乐",()->{s.pause();refresh();if(art!=null)art.invalidate();});link(footer,"end_companion","结束这次陪伴",()->new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert).setTitle("这次就到这里？").setMessage("结束音乐陪伴，不生成里程、组数或训练成绩。").setPositiveButton("结束",(d,w)->{s.endCompanion();goodbye();}).setNegativeButton("继续陪伴",null).show());refresh();
    }
    void goodbye(){page("goodbye","PULSE",null,null);headline("PULSE OFF. ENERGY ON.","收工，\n下次继续燃。","今天没有成绩单。你动起来，就很好。");PulseArt a=new PulseArt(this,false,true);body.addView(a,new LinearLayout.LayoutParams(-1,dp(200)));button(footer,"done","回到首页",()->home());}
    void preferences(){page("preferences","你的陪伴方式",()->{if(s.e.running&&s.companionMode)live();else home();},null);
        row(body,"music_preferences","音乐节奏",new String[]{"自动 · 无心率时使用默认律动","燃一点 · 136 BPM","轻一点 · 88 BPM"}[prefs.getInt("companion_music",0)],()->music());
        row(body,"voice_design","离线语音交互","先听完问题，再用一句话回答 · 设计演示",()->voiceDesign());
        row(body,"devices","设备与数据","心率可选，不需要选择训练项目",()->devices());
        row(body,"privacy","隐私与版本说明","当前不收音，不上传健康数据",()->privacy());
    }
    void music(){page("music","选一种节奏",()->preferences(),null);headline("SET YOUR VIBE","喜欢怎么燃，\n就怎么来。","音乐只提供陪伴，不替你判断运动强度。");String[] names={"自动陪伴","燃一点","轻一点"},subs={"有心率时参考节奏，无数据时用默认律动。","更明亮、更有劲的 136 BPM 循环。","轻松一点的 88 BPM 循环。"};for(int i=0;i<3;i++){final int n=i;row(body,"music_"+i,names[i],(prefs.getInt("companion_music",0)==i?"已选 · ":"")+subs[i],()->{prefs.edit().putInt("companion_music",n).apply();s.companionMusic=n;music();});}text(body,"达到心率警示参考值时，舒缓提醒优先。",12,MUTED);}
    void voiceDesign(){page("voice_design","问一句，就够",()->preferences(),null);headline("VOICE FIRST · OFFLINE","你专心运动，\n我只在必要时问。","完整版本计划：变化持续、信号足够，才询问；没有回答就维持原状。");
        LinearLayout c=card(body);badge(c,"当前为交互设计，未接入真实识别");space(c,12);text(c,"本体验稿没有麦克风权限，不实际收音。完整离线中文识别、运动变化推测、耳机收音和锁屏问答仍需开发与实机验证。",14,MUTED);
        row(body,"demo_cases","看问答设计","休息 / 换节奏 / 没回答 / 结束",()->demoCases());
        LinearLayout p=card(body);text(p,"完整版本的边界",17,INK);space(p,8);text(p,"只在问完后短时听回答；不常驻录音，不接云端识别。不确定、断连或听不清时，不擅自改变状态。",14,MUTED);
    }
    void demoCases(){page("demo_cases","选一段对话演示",()->voiceDesign(),null);badge(body,"以下都是演示脚本，不是传感器识别结果");space(body,20);
        row(body,"demo_rest","疑似在休息","问一句，收到肯定回复再放轻。",()->showDemo(CompanionDialogue.Topic.REST,false));
        row(body,"demo_change","疑似换了节奏","不猜具体器械，询问是否换音乐节奏。",()->showDemo(CompanionDialogue.Topic.CHANGE,false));
        row(body,"demo_silence","没有听到回答","不追问，不替你确认，保持原来的节奏。",()->showDemo(CompanionDialogue.Topic.REST,true));
        row(body,"demo_end","准备收工","明确询问并收到确认后结束。",()->showDemo(CompanionDialogue.Topic.END,false));
    }
    void showDemo(CompanionDialogue.Topic topic,boolean silence){
        cancelDemo();page("demo","语音交互演示",()->home(),null);demoActive=true;int generation=++demoGeneration;dialogue=new CompanionDialogue();dialogue.offer(topic,0,true,2,20000);
        badge(body,"脚本演示 · 不实际收音");space(body,22);demoStage=text(body,"① 演示：有足够的变化信号",13,ORANGE);space(body,12);demoTitle=text(body,dialogue.question(),30,INK);bold(demoTitle);space(body,20);art=new PulseArt(this,false,false);body.addView(art,new LinearLayout.LayoutParams(-1,dp(190)));space(body,18);demoCaption=text(body,"演示：音乐压低，先把问题说完。",15,MUTED);
        button(footer,"other_demo","换个场景",()->demoCases());
        demoWindowStarted=false;
        boolean speaking=false;if(!s.e.warning){s.audio.request();speaking=s.audio.previewSpeech("交互演示。"+dialogue.question(),()->beginDemoWindow(generation,silence,false));}
        if(!speaking)ui.postDelayed(()->beginDemoWindow(generation,silence,true),2500);
        else ui.postDelayed(()->beginDemoWindow(generation,silence,true),12000);
    }
    void beginDemoWindow(int generation,boolean silence,boolean captionOnly){
        if(!validDemo(generation)||demoWindowStarted)return;demoWindowStarted=true;dialogue.beginReplyWindow(0);demoStage.setText("② 演示：问完后才听回答");demoCaption.setText(captionOnly?"字幕脚本演示。此处不会打开麦克风。":"完整版本将短时听回答。此处只是演示，不实际收音。");
        ui.postDelayed(()->{if(!validDemo(generation))return;if(silence){dialogue.expire(6500);demoStage.setText("③ 演示：没有回复");demoTitle.setText("没关系，\n保持刚才的节奏。");demoCaption.setText("不会认定你在休息，也不会自动切换或结束。");}else{CompanionDialogue.Outcome result=dialogue.answer("对",2500);demoStage.setText("③ 演示脚本回复：对");demoTitle.setText(result==CompanionDialogue.Outcome.REST_CONFIRMED?"好，节奏放轻。":result==CompanionDialogue.Outcome.CHANGE_CONFIRMED?"好，换个更燃的节奏。":"好，这次就到这里。");demoCaption.setText("仅演示确认后的反馈，不代表真实识别，也不改变正在播放的音乐。");}},silence?6500:2500);
    }

    boolean validDemo(int generation){return demoActive&&generation==demoGeneration&&screen.equals("demo")&&!isFinishing();}
    void cancelDemo(){if(demoActive&&bound&&!s.e.warning){s.audio.onCompleted.clear();s.audio.tts.stop();s.audio.talking=false;if(!s.e.running){s.audio.manager.abandonAudioFocusRequest(s.audio.req);s.audio.focus=false;}}demoGeneration++;demoActive=false;if(dialogue!=null)dialogue.interrupt();}
    void devices(){page("devices","设备与数据",()->preferences(),null);headline("LESS SETUP. MORE MOVE.","有信号就参考，\n没信号就陪你听。","心率可选。手机步数尚未接入，也不判断运动项目。");LinearLayout c=card(body);deviceStatus=text(c,s.ble.status,14,INK);space(c,10);text(c,headphones()?"耳机输出已检测到":"尚未检测到耳机输出",13,MUTED);deviceRows=column();body.addView(deviceRows);renderDevices();text(body,"标准 BLE 心率可作音乐节奏参考；华为、小米为适配目标，未验证全型号。",12,MUTED);scanButton=button(footer,"scan_devices","扫描心率设备",()->scan());link(footer,"heart_reference","心率提醒参考："+prefs.getInt("alert",170)+" BPM",()->heartReference());}
    void heartReference(){page("heart_reference","心率提醒参考",()->devices(),null);headline("JUST A REFERENCE","参考值，\n不是安全线。","请按个人情况调整；它不代表通用安全阈值。");EditText field=new EditText(this);field.setTag("heart_input");field.setInputType(2);field.setText(""+prefs.getInt("alert",170));field.setTextColor(INK);field.setTextSize(32);field.setSingleLine(true);field.setPadding(dp(18),dp(18),dp(18),dp(18));field.setBackground(background(PANEL,20));body.addView(field,new LinearLayout.LayoutParams(-1,dp(90)));button(footer,"save_heart","保存",()->{try{int n=Integer.parseInt(field.getText().toString());if(n<60||n>240)throw new Exception();prefs.edit().putInt("alert",n).apply();if(s.e.running)s.e.alert=n;devices();}catch(Exception error){field.setError("请输入60–240的整数");}});}
    void scan(){String[] p=Build.VERSION.SDK_INT>=31?new String[]{"android.permission.BLUETOOTH_SCAN","android.permission.BLUETOOTH_CONNECT"}:new String[]{"android.permission.ACCESS_FINE_LOCATION"};for(String v:p)if(checkSelfPermission(v)!=android.content.pm.PackageManager.PERMISSION_GRANTED){s.ble.status="等待蓝牙权限授权…";requestPermissions(p,7);refresh();return;}s.ble.scan();refresh();}
    @Override public void onRequestPermissionsResult(int code,String[] p,int[] result){super.onRequestPermissionsResult(code,p,result);if(code!=7)return;boolean ok=result.length>0;for(int r:result)ok&=r==android.content.pm.PackageManager.PERMISSION_GRANTED;if(ok)s.ble.scan();else s.ble.status="权限未授予，音乐仍可使用";refresh();}
    String shownDevices="";
    void renderDevices(){if(deviceRows==null)return;String signature=s.ble.found.size()+":"+s.ble.status;if(signature.equals(shownDevices))return;shownDevices=signature;deviceRows.removeAllViews();if(s.ble.found.isEmpty()){LinearLayout c=card(deviceRows);text(c,"还没有发现设备",16,INK);space(c,8);text(c,"开启设备的蓝牙广播，再点击扫描。没有心率也能开始音乐陪伴。",13,MUTED);}for(int i=0;i<s.ble.found.size();i++){final int index=i;String name="未命名设备";try{if(s.ble.found.get(i).getName()!=null)name=s.ble.found.get(i).getName();}catch(Exception ignored){}row(deviceRows,"connect_"+i,name,"点击连接标准 BLE 服务",()->{s.ble.connect(index);refresh();});}}
    void privacy(){page("privacy","说清楚，才安心",()->preferences(),null);headline("OFFLINE BY DESIGN","耳朵里的陪伴，\n边界也要清楚。","1.2.0-preview · UI 与交互体验稿");LinearLayout c=card(body);text(c,"这版已经能做什么",18,INK);space(c,10);text(c,"一键音乐陪伴、三档原创离线循环、可选BLE心率参考、标准媒体播放/暂停回调、新版页面和标注的对话脚本演示。",14,MUTED);LinearLayout p=card(body);text(p,"尚未完成",18,ORANGE);space(p,10);text(p,"真实的运动状态推测、离线中文回复识别、耳机收音、锁屏语音闭环均未接入。不会把演示脚本当成真实识别结果。",14,MUTED);LinearLayout d=card(body);text(d,"本地与隐私",18,INK);space(d,10);text(d,"本体验稿不申请麦克风权限，没有网络权限，不上传健康数据，不生成新的训练成绩。默认170 BPM只作可调警示参考，并非通用安全阈值。体验稿独立安装，不覆盖或读写旧版数据。未来语音也只在提问后的短窗口内本地识别。",14,MUTED);}
    void message(String value,Runnable done){new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert).setMessage(value).setPositiveButton("知道了",(d,w)->{if(done!=null)done.run();}).show();}
    void refresh(){if(!bound)return;if(homeStatus!=null)homeStatus.setText((headphones()?"耳机输出已连接":"未检测到耳机 · 可扬声器试听")+"\n"+"此体验稿不收音 · 问答为脚本演示");if(liveTitle!=null){liveTitle.setText(s.e.paused&&!s.audio.focus?"音频暂被占用。\n准备好再继续。":s.e.warning?"先把节奏放轻。\n留意身体感受。":s.e.paused?"音乐先停一下。\n随时回来继续。":"跟着节奏。\n其他先放一边。");liveState.setText((s.e.warning?"达到心率警示参考值，请降低强度。\n":"")+(s.e.paused&&!s.audio.focus?"音频被占用，可点击继续重试":s.e.paused?"已暂停":new String[]{"轻松律动","明快律动","高能律动"}[s.e.band])+" · "+(s.e.freshHr(SystemClock.elapsedRealtime())?"心率仅作节奏参考":"未接心率，使用默认节奏")+"\n问答尚未接入，此体验稿不会收音。"+((s.audio.musicStatus.contains("加载中")||s.audio.musicStatus.contains("失败")||s.audio.musicStatus.contains("不可用")||s.audio.musicStatus.contains("中断"))?"\n"+s.audio.musicStatus:""));}if(pauseButton!=null)pauseButton.setText(s.e.paused?"继续陪伴":"暂停音乐");if(art!=null)art.paused=s.e.paused;if(deviceStatus!=null)deviceStatus.setText(s.ble.status);if(scanButton!=null){scanButton.setEnabled(!s.ble.scanning);scanButton.setText(s.ble.scanning?"正在扫描…":"扫描心率设备");}if(screen.equals("devices"))renderDevices();}
    final Runnable tick=new Runnable(){public void run(){if(!visible)return;if(bound){if(screen.equals("demo")&&s.e.warning&&demoActive){cancelDemo();demoStage.setText("演示已暂停：心率提醒优先");demoCaption.setText("不会继续问答演示。");}if(screen.equals("live")&&!s.e.running){goodbye();}refresh();}ui.postDelayed(this,1000);}};
    @Override public void onBackPressed(){if(screen.equals("home")){super.onBackPressed();return;}if(screen.equals("live")||screen.equals("demo")||screen.equals("goodbye")){home();return;}View v=shell.findViewWithTag("nav_back");if(v!=null)v.performClick();else home();}
    class PulseArt extends View {final Paint p=new Paint(3);final boolean moving;boolean paused;PulseArt(Context c,boolean moving,boolean paused){super(c);this.moving=moving;this.paused=paused;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),phase=moving&&!paused?SystemClock.uptimeMillis()/700f:0;c.save();Path clip=new Path();clip.addRoundRect(new RectF(0,0,w,h),dp(28),dp(28),Path.Direction.CW);c.clipPath(clip);p.setShader(new LinearGradient(0,h,w,0,new int[]{ORANGE,PINK,PURPLE},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);p.setAlpha(255);c.drawRoundRect(0,0,w,h,dp(28),dp(28),p);p.setShader(null);p.setColor(0x44FFFFFF);c.drawCircle(w*.86f,h*.15f,h*.55f,p);p.setColor(0x22FFFFFF);c.drawCircle(w*.12f,h*.94f,h*.55f,p);
            c.save();c.translate(w*.5f,h*.45f);float scale=Math.min(w/330f,h/180f);c.scale(scale,scale);p.setColor(INK);p.setStrokeWidth(6);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);c.drawArc(-55,-58,55,48,180,180,false,p);c.drawRoundRect(-65,-5,-39,50,10,10,p);c.drawRoundRect(39,-5,65,50,10,10,p);Path wave=new Path();for(int i=-28;i<=28;i++){float y=(float)Math.sin(i*.22+phase)*(paused?3:13)*(1-Math.abs(i)/35f);if(i==-28)wave.moveTo(i,y+8);else wave.lineTo(i,y+8);}p.setStrokeWidth(3);c.drawPath(wave,p);c.restore();p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif-black",Typeface.ITALIC));p.setTextSize(dp(27));p.setColor(BG);c.drawText("PULSE ON.",dp(22),h-dp(24),p);p.setTypeface(null);c.restore();if(moving&&visible)postInvalidateDelayed(160);
        }
    }
}
