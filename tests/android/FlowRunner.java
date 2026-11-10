package work.solour.pulse;
import android.app.*;import android.content.*;import android.os.*;import org.json.*;import java.util.concurrent.*;
/** Device-only integration tests, invoked with am instrument; never included in release APK. */
public class FlowRunner extends Instrumentation {
 JSONArray original;SessionService s;CountDownLatch bound=new CountDownLatch(1);int checks=0;ServiceConnection c=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder b){s=((SessionService.Local)b).get();bound.countDown();}public void onServiceDisconnected(ComponentName n){}};
 void ok(boolean v,String m){checks++;if(!v)throw new AssertionError(m);} public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle result=new Bundle();try{
 runOnMainSync(()->getTargetContext().bindService(new Intent(getTargetContext(),SessionService.class),c,Context.BIND_AUTO_CREATE));if(!bound.await(30,TimeUnit.SECONDS))throw new AssertionError("service bind timeout");
 runOnMainSync(()->{try{original=s.reports();s.save(new JSONArray());for(String scene:new String[]{"单车","跑步机","器械"}){
 s.begin(scene,true,170,30,1,false,0,0);ok(s.e.running&&s.e.simulated,"explicit simulation");long now=SystemClock.elapsedRealtime();s.e.heart(120,now);s.e.velocity(2,now);s.e.cadence(80,now);s.e.tick(now);ok(s.e.valid==1,"valid samples");s.pause();int seconds=s.e.seconds;s.e.tick(now+1000);ok(s.e.seconds==seconds,"pause freeze");s.pause();if(scene.equals("器械")){s.e.startGroup(false);s.e.tick(now+2000);s.e.finishGroup();s.e.rest=0;s.e.startGroup(true);s.e.tick(now+3000);s.e.finishGroup();ok(s.e.confirmed==1&&s.e.pending()==1,"confirmed and pending separate");s.e.startGroup(false);}
 JSONObject r=s.finish();ok(!s.e.running,"stopped after successful save");ok(r.getBoolean("simulation"),"report simulation label");ok(r.getInt("valid")>0,"report coverage");if(scene.equals("器械"))ok(r.getBoolean("unfinished")&&r.getInt("confirmed")==1,"unfinished excluded");if(scene.equals("跑步机"))ok(r.getLong("metres")>0&&r.getString("distanceLabel").equals("设备速度积分"),"report distance");}
 ok(s.reports().length()==3,"reports persisted");
 }catch(Exception e){throw new RuntimeException(e);}});
 runOnMainSync(()->{try{JSONArray seed=new JSONArray();for(int i=0;i<102;i++)seed.put(new JSONObject());s.save(seed);s.e=new Engine();s.e.running=true;s.finish();}catch(Exception ex){throw new RuntimeException(ex);}});
 runOnMainSync(()->{try{ok(s.reports().length()==100,"100-report limit");JSONArray all=s.reports();all.remove(0);s.save(all);ok(s.reports().length()==99,"delete persisted");s.save(original);}catch(Exception ex){throw new RuntimeException(ex);}});
 Intent intent=new Intent(getTargetContext(),MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);Activity a=startActivitySync(intent);waitForIdleSync();ok(a!=null,"activity launched");runOnMainSync(()->a.finish());result.putString("stream","PASS "+checks+" Android service/report flow assertions\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable t){result.putString("stream","FAIL "+t.toString()+"\n");finish(Activity.RESULT_CANCELED,result);}finally{if(s!=null)getTargetContext().unbindService(c);}}
}
