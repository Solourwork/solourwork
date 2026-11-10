package work.solour.pulse;
import java.util.*;
/** Pure session logic. Every tick represents one active second; stale samples never enter aggregates. */
public class Engine {
 public String scene="单车"; public boolean paused=false, simulated=false, running=false, warning=false; public int alert=170, restDefault=60, rest=0, seconds=0, valid=0, max=0, hr=0, cadence=0, cadenceCount=0; public double sum=0, distance=0, manualKmh=0, cadenceSum=0; public long hrAt=-100000,speedAt=-100000,cadenceAt=-100000; public double speed=0, smooth=0, baseline=0, baseSum=0, peak=0; public int baseN=0,rise=0,fall=0,groupStart=-1; public boolean inferred=false,restPaused=false,restDone=false; public int confirmed=0; public final List<Group> groups=new ArrayList<>(); public final List<Integer> curve=new ArrayList<>(); public int band=0,candidate=-1,candidateSeconds=0,lastSwitch=-10000,lastEncourage=0; public boolean estimatedUsed=false,deviceDistanceUsed=false; public final List<Integer> countdown=new ArrayList<>();
 public static class Group { public int start,duration; public boolean guessed,confirmed; Group(int s,int d,boolean g){start=s;duration=d;guessed=g;confirmed=!g;} }
 public static class Measurement {public int hr; public Measurement(int h){hr=h;}}
 public static int parseHr(byte[] b){if(b==null||b.length<2)throw new IllegalArgumentException("短心率包");if((b[0]&1)==0)return b[1]&255;if(b.length<3)throw new IllegalArgumentException("短16位心率包");return (b[1]&255)|((b[2]&255)<<8);}
 public void heart(int v,long now){if(v>=25&&v<=250){hr=v;hrAt=now;}}
 public boolean freshHr(long now){return now>=hrAt&&now-hrAt<=10000;}
 public void velocity(double metresPerSecond,long now){if(metresPerSecond>=0&&metresPerSecond<30){speed=metresPerSecond;speedAt=now;}}
 public void cadence(int value,long now){if(value>=0&&value<300){cadence=value;cadenceAt=now;}}
 public void disconnect(){hrAt=speedAt=cadenceAt=-100000;rise=fall=0;candidate=-1;candidateSeconds=0;}
 public void pause(){paused=!paused;rise=fall=0;candidate=-1;candidateSeconds=0;}
 public void startGroup(boolean guess){if(groupStart<0){groupStart=seconds;inferred=guess;peak=smooth;rise=fall=0;}}
 public void finishGroup(){if(groupStart>=0){Group g=new Group(groupStart,seconds-groupStart,inferred);groups.add(g);if(g.confirmed)confirmed++;groupStart=-1;rise=fall=0;startRest();}}
 public void confirm(int i){Group g=groups.get(i);if(!g.confirmed){g.confirmed=true;confirmed++;}}
 public void delete(int i){Group g=groups.remove(i);if(g.confirmed)confirmed--;}
 public int pending(){int n=0;for(Group g:groups)if(!g.confirmed)n++;return n;}
 public void startRest(){rest=restDefault;restPaused=false;restDone=false;rise=fall=0;}
 public void tick(long now){countdown.clear();restDone=false;if(!running||paused)return;seconds++;boolean fresh=freshHr(now);warning=fresh&&hr>=alert;
 if(fresh){valid++;sum+=hr;max=Math.max(max,hr);smooth=smooth==0?hr:.25*hr+.75*smooth;curve.add(hr);}else{curve.add(0);rise=fall=0;}
 if(now>=cadenceAt&&now-cadenceAt<=5000){cadenceCount++;cadenceSum+=cadence;}
 if(scene.equals("跑步机")){if(now>=speedAt&&now-speedAt<=5000){distance+=speed;deviceDistanceUsed=true;}else if(manualKmh>0){distance+=manualKmh/3.6;estimatedUsed=true;}}
 if(rest>0){if(!restPaused){rest--;if(rest==10||rest==5||rest==3||rest==2||rest==1)countdown.add(rest);if(rest==0)restDone=true;}rise=fall=0;}
 else if(scene.equals("器械")&&fresh&&!warning){if(baseN<20){baseSum+=smooth;baseN++;if(baseN==20)baseline=baseSum/20;}else if(groupStart<0){rise=smooth>=baseline+12?rise+1:0;if(rise>=8)startGroup(true);}else if(inferred){peak=Math.max(peak,smooth);fall=peak-smooth>=10?fall+1:0;if(seconds-groupStart>=20&&fall>=10)finishGroup();}}
 else {rise=fall=0;}
 int desired=warning?0:(scene.equals("器械")?(rest>0?0:1):(fresh?(hr<110?0:hr<145?1:2):0));
 if(warning||!fresh||scene.equals("器械")){band=desired;candidateSeconds=0;candidate=-1;}else if(desired!=band){if(candidate!=desired){candidate=desired;candidateSeconds=1;}else candidateSeconds++;int buffer=scene.equals("跑步机")?12:8,spacing=scene.equals("跑步机")?30:20;if(candidateSeconds>=buffer&&seconds-lastSwitch>=spacing){band=desired;lastSwitch=seconds;candidate=-1;candidateSeconds=0;}}else{candidate=-1;candidateSeconds=0;}
 }
 public boolean encourage(){int interval=scene.equals("跑步机")?300:180;if(!scene.equals("器械")&&!warning&&seconds-lastEncourage>=interval){lastEncourage=seconds;return true;}return false;}
 public String distanceLabel(){return estimatedUsed?(deviceDistanceUsed?"设备与手动估算混合":"手动速度估算"):"设备速度积分";}
}
