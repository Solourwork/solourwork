"""Original procedural four-bar synth compositions. No external recordings or samples."""
import math,wave,struct,pathlib
out=pathlib.Path(__file__).resolve().parents[1]/'app/src/main/assets';out.mkdir(parents=True,exist_ok=True)
for index,bpm in enumerate([88,116,136]):
 rate=22050;n=round(16*60/bpm*rate);notes=[48,55,52,59,53,60,55,62,48,55,52,60,53,60,55,59];data=bytearray()
 for i in range(n):
  t=i/rate;beat=t*bpm/60;k=int(beat);phase=beat%1;f=440*2**((notes[k%16]-69)/12);env=math.exp(-phase*5)*min(phase*100,1);kick=math.sin(2*math.pi*(48*t+3*(1-math.exp(-phase*15))))*math.exp(-phase*18)*min(phase*100,1)
  sample=.20*math.sin(2*math.pi*f*t)*env+.12*math.sin(2*math.pi*f*.5*t)*env+.23*kick
  # Tail/head taper prevents a loop boundary click, with a deliberate light breath at the bar.
  edge=min(1,i/(rate*.01),(n-1-i)/(rate*.01));data+=struct.pack('<h',int(sample*edge*25000))
 with wave.open(str(out/f'pulse{index}.wav'),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes(data)
