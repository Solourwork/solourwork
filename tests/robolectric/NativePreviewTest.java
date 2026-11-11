package work.solour.pulse;
import android.graphics.*;import android.content.pm.ServiceInfo;import android.os.*;import android.view.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import java.io.*;
import static org.junit.Assert.*;import static org.robolectric.Shadows.shadowOf;

/** Android 14 framework/Skia previews, not screenshots from a booted Android VM. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,manifest=Config.NONE,qualifiers="w480dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class NativePreviewTest {
 void image(PulseUiTest h,String name)throws Exception{h.idle();h.layout();Bitmap b=Bitmap.createBitmap(480,800,Bitmap.Config.ARGB_8888);h.a.getWindow().getDecorView().draw(new Canvas(b));File dir=new File("build/native-previews");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();}
 @Test public void renderScreensAndCheckForegroundTypes()throws Exception{
  RuntimeEnvironment.getApplication().getApplicationInfo().theme=android.R.style.Theme_Material_Light_NoActionBar;
  RuntimeEnvironment.getApplication().setTheme(android.R.style.Theme_Material_Light_NoActionBar);
  PulseUiTest h=new PulseUiTest();h.setup();try{
   image(h,"01-home");h.selectDemo("单车");image(h,"02-prepare");h.click("start_session");h.step(4);image(h,"03-cycle");
   h.s.refreshForeground();assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,h.s.getForegroundServiceType());
   h.s.e.simulated=false;h.s.refreshForeground();assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,h.s.getForegroundServiceType());
   shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("android.permission.BLUETOOTH_CONNECT");h.s.refreshForeground();assertTrue((h.s.getForegroundServiceType()&ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)!=0);h.s.e.simulated=true;
   h.click("begin_rest");image(h,"04-rest");h.click("exit_rest");h.finish();image(h,"05-report");h.click("report_done");h.click("tab_history");h.await(()->h.view("report_0")!=null,"history loaded");image(h,"06-history");h.click("tab_settings");image(h,"07-settings");h.click("tab_train");
   h.selectDemo("跑步机");h.click("start_session");h.step(4);image(h,"08-treadmill");h.finish();h.click("report_done");
   h.selectDemo("器械");h.click("start_session");h.click("group_action");h.step(3);image(h,"09-equipment");h.click("group_action");h.click("exit_rest");h.s.e.startGroup(true);h.step(2);h.s.e.finishGroup();h.s.e.rest=0;h.a.observedRest=0;h.click("open_groups");image(h,"10-groups");h.click("group_1");image(h,"11-pending-group");
   System.out.println("NATIVE FRAMEWORK PREVIEWS: 11 screens rendered; foreground media/device type conditions checked");
  }finally{h.cleanup();}
 }
}
