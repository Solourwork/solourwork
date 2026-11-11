package work.solour.pulse;

import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.android.controller.*;
import java.io.*;import java.time.Duration;import java.util.function.BooleanSupplier;
import org.json.*;
import static org.junit.Assert.*;import static org.robolectric.Shadows.shadowOf;

/** Android framework simulation. This suite does not claim real BLE, audio or physical touch validation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest="app/src/main/AndroidManifest.xml",qualifiers="w480dp-h800dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
public class PulseUiTest {
 MainActivity a;SessionService s;ActivityController<MainActivity> activity;ServiceController<SessionService> service;int operations;
 static int totalOperations,totalChecks;
 void check(boolean value,String message){totalChecks++;assertTrue(message,value);}
 void idle(){shadowOf(Looper.getMainLooper()).idle();}
 void step(int seconds){shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds));}
 void await(BooleanSupplier p,String label)throws Exception{long end=System.nanoTime()+8_000_000_000L;while(System.nanoTime()<end){idle();if(p.getAsBoolean())return;Thread.sleep(10);}fail("timeout: "+label+"; page="+a.screen);}
 @Before public void setup()throws Exception{
  service=Robolectric.buildService(SessionService.class).create();s=service.get();
  shadowOf(RuntimeEnvironment.getApplication()).setComponentNameAndServiceForBindService(new ComponentName(RuntimeEnvironment.getApplication(),SessionService.class),s.binder);
  shadowOf(RuntimeEnvironment.getApplication()).setBindServiceCallsOnServiceConnectedDirectly(true);
  activity=Robolectric.buildActivity(MainActivity.class).setup().visible();a=activity.get();idle();await(()->a.bound,"service bound");
  a.prefs.edit().clear().commit();a.demo=false;a.selected="单车";s.save(new JSONArray());a.home();idle();layout();
 }
 @After public void cleanup()throws Exception{if(a!=null){a.saving=false;s.e.running=false;activity.pause().stop().destroy();}if(service!=null)service.destroy();}
 @AfterClass public static void counts(){System.out.println("FRAMEWORK UI COUNTS: "+totalChecks+" checks, "+totalOperations+" control actions across 4 scenarios");}
 void layout(){View decor=a.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));decor.layout(0,0,480,800);}
 View view(String tag){return a.getWindow().getDecorView().findViewWithTag(tag);}
 void click(String tag){idle();layout();View v=view(tag);assertNotNull("control exists: "+tag,v);check(v.isEnabled(),"enabled: "+tag);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);layout();long now=SystemClock.uptimeMillis();MotionEvent d=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,v.getWidth()/2f,v.getHeight()/2f,0),u=MotionEvent.obtain(now,now+60,MotionEvent.ACTION_UP,v.getWidth()/2f,v.getHeight()/2f,0);d.setSource(InputDevice.SOURCE_TOUCHSCREEN);u.setSource(InputDevice.SOURCE_TOUCHSCREEN);boolean accepted=v.dispatchTouchEvent(d);v.dispatchTouchEvent(u);d.recycle();u.recycle();check(accepted,"control accepts framework touch: "+tag);idle();operations++;totalOperations++;}
 void page(String name){idle();check(a.screen.equals(name),"page "+name+", got "+a.screen);layout();}
 void value(String tag,String text){((EditText)view(tag)).setText(text);idle();}
 void back(){click("nav_back");}
 void dialog(int which){AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d);assertTrue(d.isShowing());d.getButton(which).performClick();idle();totalOperations++;}
 void shot(String name)throws Exception{idle();layout();File f=new File("build/framework-hierarchies");f.mkdirs();try(java.io.PrintWriter out=new java.io.PrintWriter(new File(f,name+".txt"),"UTF-8")){hierarchy(a.getWindow().getDecorView(),out,0);}}
 void hierarchy(View v,java.io.PrintWriter out,int depth){for(int i=0;i<depth;i++)out.print("  ");out.println(v.getClass().getSimpleName()+" tag="+v.getTag()+" bounds="+v.getLeft()+","+v.getTop()+","+v.getRight()+","+v.getBottom()+" enabled="+v.isEnabled()+(v instanceof TextView?" text="+((TextView)v).getText():""));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)hierarchy(((ViewGroup)v).getChildAt(i),out,depth+1);}

 void selectDemo(String scene){click("scene_"+scene);page("prepare");click("setup_mode");page("mode");click("mode_demo");page("prepare");check(a.demo,"explicit demo selected");}
 void finish()throws Exception{click("nav_more");page("options");click("end_session");dialog(DialogInterface.BUTTON_NEGATIVE);page("options");click("end_session");dialog(DialogInterface.BUTTON_POSITIVE);await(()->a.screen.equals("report"),"report saved");page("report");check(!s.e.running,"session stops after persist");}

 @Test public void navigationPreferencesAndInputs()throws Exception{
  page("home");shot("01-home");click("tab_history");page("history");await(()->a.body.getChildCount()>0,"empty history");shot("02-empty-history");click("tab_settings");page("settings");
  click("settings_sound");page("sound");click("toggle_music");check(!a.prefs.getBoolean("music",true),"music off feedback");click("toggle_music");check(a.prefs.getBoolean("music",false),"music on feedback");
  for(int i=0;i<3;i++){click("sound_voice");page("voice");click("voice_"+i);page("sound");check(a.prefs.getInt("style",-1)==i,"voice style "+i);}
  click("tts_settings");check(shadowOf(a).getNextStartedActivity()!=null,"system TTS settings intent requested");back();page("settings");
  click("settings_alert");page("number_alert");value("number_input","5");click("save_number");check(((EditText)view("number_input")).getError()!=null,"invalid alert feedback");value("number_input","175");click("save_number");page("settings");check(a.prefs.getInt("alert",0)==175,"alert preference saved");
  click("settings_about");page("about");shot("03-about");back();click("tab_train");page("home");
  click("scene_单车");click("setup_goal");page("goal");value("goal_input","-1");click("save_goal");page("goal");value("goal_input","12");click("save_goal");page("prepare");check(a.goal==12,"goal saved");
  click("setup_mode");click("mode_demo");check(a.demo,"demo choice visible");click("setup_mode");click("mode_real");check(!a.demo,"real mode choice visible");click("setup_device");page("devices");shot("04-device");shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION);click("scan_devices");check(a.afterPermission!=null&&s.ble.status.startsWith("等待"),"scan requests permission with immediate feedback");a.onRequestPermissionsResult(7,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},new int[]{android.content.pm.PackageManager.PERMISSION_DENIED});check(s.ble.status.contains("权限未授予"),"permission denial visible");dialog(DialogInterface.BUTTON_NEGATIVE);click("scan_devices");shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION);a.onRequestPermissionsResult(7,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},new int[]{android.content.pm.PackageManager.PERMISSION_GRANTED});idle();check(a.afterPermission==null&&!s.ble.status.startsWith("等待"),"grant automatically continues scanning or shows hardware reason");if(s.ble.scanning)step(11);a.updateValues();check(view("scan_devices").isEnabled(),"scan button restored after completion or hardware error");back();page("prepare");
  click("setup_more");page("preferences");click("pref_rest");value("number_input","20");click("save_number");page("number_rest_setup");check(((EditText)view("number_input")).getError()!=null,"invalid rest feedback");value("number_input","30");click("save_number");page("preferences");check(a.rest==30,"rest saved");click("pref_voice");click("voice_1");page("preferences");back();page("prepare");shot("05-prepare");
 }
 @Test public void cyclePauseRecoveryAndReport()throws Exception{
  selectDemo("单车");click("start_session");page("training");step(4);System.out.println("Timer sample: seconds="+s.e.seconds+", paused="+s.e.paused);check(s.e.seconds>=3&&s.e.seconds<=4,"timer advances within dispatch tolerance");shot("06-cycle");
  click("pause_session");check(s.e.paused,"pause feedback");int seconds=s.e.seconds;step(3);check(s.e.seconds==seconds,"pause freezes time");click("pause_session");check(!s.e.paused,"resume feedback");
  click("nav_back");dialog(DialogInterface.BUTTON_NEUTRAL);dialog(DialogInterface.BUTTON_NEGATIVE);page("training");click("nav_back");dialog(DialogInterface.BUTTON_POSITIVE);page("training");click("nav_back");dialog(DialogInterface.BUTTON_NEGATIVE);page("home");click("resume_session");page("training");check(s.e.running,"home return preserves training");
  click("begin_rest");page("rest");shot("07-rest");back();page("training");check(s.e.rest>0,"back preserves recovery");click("begin_rest");page("rest");
  click("pause_rest");check(s.e.restPaused,"rest paused");int left=s.e.rest;click("extend_rest");check(s.e.rest==left+30,"rest extended");s.e.rest=590;click("extend_rest");check(s.e.rest==600,"rest extension capped at 600 seconds");click("pause_rest");s.e.rest=1;step(2);check(s.e.rest==0&&s.e.groupStart<0&&a.screen.equals("rest"),"rest only reminds");click("exit_rest");page("training");
  click("nav_more");click("option_device");page("devices");back();page("options");click("option_music");page("sound");click("toggle_music");check(!s.audio.music,"live music applied");back();page("options");click("option_rest");value("number_input","90");click("save_number");page("options");check(s.e.restDefault==90,"live rest applied");back();page("training");
  finish();check(s.reports().length()==1,"report persisted");shot("08-report");click("report_done");page("home");click("tab_history");await(()->view("report_0")!=null,"history list populated");shot("09-history");click("report_0");page("report");back();await(()->view("report_0")!=null,"report back");click("report_0");click("nav_more");page("report_options");back();page("report");click("nav_more");page("report_options");click("delete_report");dialog(DialogInterface.BUTTON_NEGATIVE);page("report_options");click("delete_report");dialog(DialogInterface.BUTTON_POSITIVE);await(()->a.screen.equals("history"),"delete completes");check(s.reports().length()==0,"delete persisted");
 }
 @Test public void treadmillDistanceAndManualSpeed()throws Exception{
  selectDemo("跑步机");click("setup_goal");click("goal_time");click("goal_distance");value("goal_input","2");click("save_goal");check(a.km&&a.goal==2,"distance goal selected");
  click("setup_more");click("pref_speed");value("number_input","99");click("save_number");check(((EditText)view("number_input")).getError()!=null,"invalid speed feedback");value("number_input","6");click("save_number");back();click("start_session");page("training");step(3);check(s.e.distance>0,"simulation distance updates");shot("10-treadmill");click("nav_more");click("option_speed");value("number_input","8");click("save_number");check(s.e.manualKmh==8,"live manual speed applied");back();finish();check(a.currentReport.optBoolean("simulation"),"simulation report labelled");
 }
 @Test public void equipmentGroupsConfirmDeleteAndUnfinished()throws Exception{
  selectDemo("器械");click("start_session");page("training");click("nav_more");click("option_groups");page("groups");check(s.e.groups.isEmpty(),"empty groups page");shot("11-groups-empty");back();click("pause_session");check(!view("group_action").isEnabled(),"paused group action disabled");click("pause_session");click("group_action");check(s.e.groupStart>=0&&!s.e.inferred,"manual start");step(2);click("group_action");page("rest");check(s.e.confirmed==1,"manual finish confirmed");click("exit_rest");
  s.e.startGroup(true);step(2);s.e.finishGroup();s.e.rest=0;a.observedRest=0;click("open_groups");page("groups");click("group_1");page("group_detail");shot("12-group-pending");back();page("groups");click("group_1");click("confirm_group");check(s.e.confirmed==2&&s.e.pending()==0,"pending group confirmation");click("group_1");click("delete_group");dialog(DialogInterface.BUTTON_POSITIVE);page("groups");check(s.e.confirmed==1,"delete adjusts count");back();click("group_action");click("nav_more");click("discard_group");dialog(DialogInterface.BUTTON_POSITIVE);page("training");check(s.e.groupStart<0,"discard unfinished");click("group_action");finish();check(a.currentReport.optBoolean("unfinished")&&a.currentReport.optInt("confirmed")==1,"unfinished excluded from report");
 }
}
