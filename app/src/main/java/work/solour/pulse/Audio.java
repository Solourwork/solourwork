package work.solour.pulse;

import android.content.Context;
import android.media.*;
import android.speech.tts.*;
import java.util.*;

/** Audio preparation never blocks service binding or the first screen. */
public class Audio {
    final SessionService s;
    final AudioManager manager;
    final MediaPlayer[] players=new MediaPlayer[3];
    final float[] volumes={0,0,0};
    TextToSpeech tts;
    boolean ready=false,focus=false,duck=false,talking=false,music=true;
    volatile boolean closed=false;
    int selected=0;
    public String ttsStatus="正在检查离线中文语音", musicStatus="离线音乐加载中";
    AudioFocusRequest req;

    Audio(SessionService service){
        s=service;manager=(AudioManager)s.getSystemService(Context.AUDIO_SERVICE);
        tts=new TextToSpeech(s,result->{
            if(closed)return;
            if(result==TextToSpeech.SUCCESS){
                boolean found=false;Set<Voice> voices=tts.getVoices();
                if(voices!=null)for(Voice v:voices)if(v.getLocale().getLanguage().equals("zh")&&!v.isNetworkConnectionRequired()){tts.setVoice(v);found=true;break;}
                ready=found;ttsStatus=found?"离线中文语音可用":"缺少离线中文语音包，请在系统语音设置安装";
            }else ttsStatus="系统语音初始化失败，可打开系统设置检查";
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
            public void onStart(String id){s.h.post(()->talking=true);}
            public void onDone(String id){s.h.post(()->talking=false);}
            public void onError(String id){s.h.post(()->talking=false);}
        });
        Thread loader=new Thread(()->{
            int loaded=0;
            for(int i=0;i<3&&!closed;i++){
                final int index=i;MediaPlayer p=null;
                try{
                    p=new MediaPlayer();p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                    try(android.content.res.AssetFileDescriptor fd=s.getAssets().openFd("pulse"+i+".wav")){p.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}
                    p.setLooping(true);p.prepare();p.setVolume(0,0);
                    final MediaPlayer prepared=p;s.h.post(()->{if(closed)prepared.release();else players[index]=prepared;});loaded++;
                }catch(Exception e){if(p!=null)p.release();}
            }
            final int count=loaded;s.h.post(()->{if(!closed)musicStatus=count==3?"三档离线音乐已就绪":count==0?"音乐暂不可用，训练可继续":"部分音乐加载失败，训练可继续";});
        },"pulse-music-loader");loader.setDaemon(true);loader.start();
        req=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()).setOnAudioFocusChangeListener(change->s.h.post(()->{
            if(change==AudioManager.AUDIOFOCUS_GAIN){focus=true;duck=false;}
            else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)duck=true;
            else{focus=false;tts.stop();talking=false;if(s.e.running&&!s.e.paused)s.pause();}
            update();
        })).build();
        s.h.post(fader);
    }
    final Runnable fader=new Runnable(){public void run(){
        if(closed)return;float scale=talking||duck?.17f:.65f;
        for(int i=0;i<3;i++){
            float target=focus&&music&&!s.e.paused&&s.e.running&&selected==i?scale:0;
            volumes[i]+=Math.max(-.065f,Math.min(.065f,target-volumes[i]));MediaPlayer p=players[i];
            if(p!=null)try{p.setVolume(volumes[i],volumes[i]);if(focus&&s.e.running&&!s.e.paused&&!p.isPlaying())p.start();if((!focus||s.e.paused||!s.e.running)&&p.isPlaying())p.pause();}
            catch(IllegalStateException ex){players[i]=null;p.release();musicStatus="音乐播放中断，训练可继续";}
        }
        s.h.postDelayed(this,100);
    }};
    void request(){focus=manager.requestAudioFocus(req)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}
    void update(){selected=s.e.band;}
    public void say(String text,boolean urgent){if(!ready||!focus||closed)return;tts.setSpeechRate(s.style==1?1.08f:s.style==2?.98f:.88f);tts.speak(text,urgent?TextToSpeech.QUEUE_FLUSH:TextToSpeech.QUEUE_ADD,null,UUID.randomUUID().toString());}
    public String encouragement(){return s.style==1?"节奏很棒，稳住，我们一起完成！":s.style==2?"保持可控的强度，关注呼吸与动作。":"你已经做得很好，按自己的节奏来。";}
    void close(){closed=true;s.h.removeCallbacks(fader);tts.stop();tts.shutdown();for(MediaPlayer p:players)if(p!=null)p.release();manager.abandonAudioFocusRequest(req);}
}
