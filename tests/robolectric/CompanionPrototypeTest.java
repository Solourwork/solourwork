package work.solour.pulse;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.android.controller.*;
import java.io.*;import java.time.Duration;import org.json.*;
import static org.junit.Assert.*;import static org.robolectric.Shadows.shadowOf;

/** Native Android framework UI prototype checks. No real ASR/sensor inference/microphone validation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,manifest=Config.NONE,qualifiers="w480dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class CompanionPrototypeTest {
 PulseCompanionActivity a;SessionService s;ActivityController<PulseCompanionActivity> activity;ServiceController<SessionService> service;
 static int checks,actions;
 void ok(boolean v,String message){checks++;assertTrue(message,v);}
 void idle(){shadowOf(Looper.getMainLooper()).idle();}
 void step(long ms){shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 @Before public void setup()throws Exception{
  RuntimeEnvironment.getApplication().getApplicationInfo().theme=android.R.style.Theme_Material_Light_NoActionBar;
  service=Robolectric.buildService(SessionService.class).create();s=service.get();
  shadowOf(RuntimeEnvironment.getApplication()).setComponentNameAndServiceForBindService(new ComponentName(RuntimeEnvironment.getApplication(),SessionService.class),s.binder);
  shadowOf(RuntimeEnvironment.getApplication()).setBindServiceCallsOnServiceConnectedDirectly(true);
  activity=Robolectric.buildActivity(PulseCompanionActivity.class).setup().visible();a=activity.get();idle();a.prefs.edit().clear().commit();s.save(new JSONArray());a.home();idle();layout();
 }
 @After public void cleanup(){if(a!=null)activity.pause().stop().destroy();if(service!=null)service.destroy();}
 @AfterClass public static void counts(){System.out.println("COMPANION UI COUNTS: "+checks+" checks, "+actions+" framework control actions; scripted voice demo only");}
 void layout(){View d=a.getWindow().getDecorView();d.measure(View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));d.layout(0,0,480,800);}
 View view(String tag){return a.getWindow().getDecorView().findViewWithTag(tag);}
 void click(String tag){idle();layout();View v=view(tag);assertNotNull(tag,v);ok(v.isEnabled(),"enabled "+tag);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);layout();long t=SystemClock.uptimeMillis();MotionEvent d=MotionEvent.obtain(t,t,0,v.getWidth()/2f,v.getHeight()/2f,0),u=MotionEvent.obtain(t,t+60,1,v.getWidth()/2f,v.getHeight()/2f,0);d.setSource(InputDevice.SOURCE_TOUCHSCREEN);u.setSource(InputDevice.SOURCE_TOUCHSCREEN);ok(v.dispatchTouchEvent(d),"accepts framework touch "+tag);v.dispatchTouchEvent(u);d.recycle();u.recycle();idle();actions++;}
 void page(String v){idle();ok(a.screen.equals(v),"route "+v+", got "+a.screen);}
 void dialog(int button){AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d);assertTrue(d.isShowing());d.getButton(button).performClick();idle();actions++;}
 void shot(String n)throws Exception{idle();String actualAudioStatus=s.audio.musicStatus;s.audio.musicStatus="框架预览不验证音频输出";a.refresh();layout();Bitmap b=Bitmap.createBitmap(480,800,Bitmap.Config.ARGB_8888);a.getWindow().getDecorView().draw(new Canvas(b));File dir=new File("build/companion-previews");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,n+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();s.audio.musicStatus=actualAudioStatus;a.refresh();}
 @Test public void oneTapMusicAndMediaControls()throws Exception{
  page("home");ok(view("scene_单车")==null&&view("setup_goal")==null,"no training project or goal gate");shot("01-home");click("start_companion");dialog(DialogInterface.BUTTON_POSITIVE);page("live");ok(s.companionMode&&s.e.running&&s.target==0,"companion starts with no training target");step(1500);a.refresh();shot("02-live");String savedStatus=s.audio.musicStatus;s.audio.musicStatus="音乐暂不可用，可稍后重试";a.refresh();ok(a.liveState.getText().toString().contains("不可用"),"audio failure has visible feedback");s.audio.musicStatus=savedStatus;a.refresh();
  assertEquals(PulseCompanionActivity.class.getName(),shadowOf(s.notification().contentIntent).getSavedIntent().getComponent().getClassName());
  click("pause_companion");ok(s.e.paused,"UI pauses music");shot("03-paused");s.mediaControls.onPlay();a.refresh();ok(!s.e.paused,"standard media play callback resumes");s.mediaControls.onPause();a.refresh();ok(s.e.paused,"standard media pause callback pauses");click("pause_companion");ok(!s.e.paused,"UI resume");
  click("preferences");click("music_preferences");page("music");for(int i=0;i<3;i++){click("music_"+i);ok(a.prefs.getInt("companion_music",-1)==i,"music preference "+i);}shot("04-music");click("nav_back");page("preferences");shot("05-preferences");
  click("devices");page("devices");shot("06-devices");shadowOf(RuntimeEnvironment.getApplication()).denyPermissions("android.permission.BLUETOOTH_SCAN","android.permission.BLUETOOTH_CONNECT");click("scan_devices");ok(s.ble.status.startsWith("等待"),"scan permission feedback");a.onRequestPermissionsResult(7,new String[]{"android.permission.BLUETOOTH_SCAN"},new int[]{-1});ok(s.ble.status.contains("权限未授予"),"denial visible");click("heart_reference");page("heart_reference");((EditText)view("heart_input")).setText("5");click("save_heart");ok(((EditText)view("heart_input")).getError()!=null,"invalid alert rejected");((EditText)view("heart_input")).setText("175");click("save_heart");page("devices");ok(s.e.alert==175,"alert reference updated");click("nav_back");click("nav_back");page("live");
  s.e.heart(180,SystemClock.elapsedRealtime());step(1500);a.refresh();ok(s.e.warning&&s.e.band==0,"safety relaxation overrides music preference");
  s.e.disconnect();step(1500);a.refresh();s.mediaControls.onStop();step(1100);page("goodbye");ok(!s.e.running&&s.reports().length()==0,"media stop produces no training scores");shot("07-goodbye");click("done");page("home");
 }
 @Test public void explicitScriptedDemoAndCancellation()throws Exception{
  click("demo_entry");page("demo");ok(a.demoActive,"demo explicitly active");shot("08-question");step(2600);ok(a.demoWindowStarted,"scripted reply window after question stage");shot("09-reply-window");step(2600);ok(a.demoTitle.getText().toString().contains("放轻"),"scripted affirmative feedback");ok(!s.e.running,"demo does not start real music session");shot("10-confirmed-demo");
  click("other_demo");page("demo_cases");shot("11-demo-cases");click("demo_silence");step(9200);ok(a.demoTitle.getText().toString().contains("保持"),"silence keeps original state");shot("12-silence");click("other_demo");click("demo_change");step(5300);ok(a.demoTitle.getText().toString().contains("更燃"),"change only scripted");click("other_demo");click("demo_end");step(5300);ok(a.demoTitle.getText().toString().contains("到这里"),"end only scripted");ok(!s.e.running&&s.reports().length()==0,"no recording or scores produced by script");click("nav_back");page("home");
  click("preferences");click("voice_design");page("voice_design");shot("13-voice-design");click("demo_cases");click("demo_rest");click("nav_back");step(12000);page("home");ok(!a.demoActive,"leaving cancels delayed demo");click("preferences");click("privacy");page("privacy");shot("14-privacy");
 }
}
