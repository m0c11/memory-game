"""Original offline synthesized ambience and foley; no borrowed recordings."""
from pathlib import Path
import numpy as np,wave
out=Path(__file__).resolve().parents[2]/'assets/polet/audio';rate=22050;rng=np.random.default_rng(271026)
def save(name,v):
 v=np.clip(v,-.95,.95)
 with wave.open(str(out/(name+'.wav')),'wb') as w:w.setparams((1,2,rate,0,'NONE','not compressed'));w.writeframes((v*32767).astype('<i2').tobytes())
def tone(freq,dur=.2):
 t=np.arange(int(rate*dur))/rate;return np.sin(2*np.pi*freq*t)*np.minimum(t/.01,1)*np.exp(-t*16)
def noise(dur):return rng.normal(0,1,int(rate*dur))
for name,dur,amp,decay in [('step',.13,.17,35),('paper',.45,.12,8),('cloth',.5,.1,6),('sweep',.65,.07,3),('door',.24,.12,18),('bump',.22,.2,18),('click',.08,.1,65)]:
 t=np.arange(int(dur*rate))/rate;n=np.convolve(noise(dur),np.ones(7)/7,'same');v=n*np.exp(-t*decay)*amp
 if name in ['step','bump','door']:v+=np.sin(2*np.pi*90*t)*np.exp(-t*35)*.12
 save(name,v)
for name,f in [('success',660),('tick',440),('radio',1200),('bell',1046),('choice',520)]:
 v=tone(f,.35)*.22+tone(f*1.5,.35)*.10;save(name,v)
# Seamless quiet air bed with occasional birds; fade the ends so the restart never clicks.
dur=24;t=np.arange(rate*dur)/rate;n=noise(dur);v=np.convolve(n,np.ones(160)/160,'same')*.10
for start in [2.2,5.5,12.3,19.8]:
 length=int(.5*rate);u=np.arange(length)/rate;chirp=np.sin(2*np.pi*(1800*u+900*u*u))*np.sin(np.pi*u/.5)**2*.025;idx=int(start*rate);v[idx:idx+length]+=chirp
v*=np.minimum(1,np.minimum(t/.3,(dur-t)/.3));save('camp_air',v)
# Gentle original harmonic loop, 32 seconds, no voices.
dur=32;t=np.arange(rate*dur)/rate;v=np.zeros_like(t)
chords=[[48,52,55],[45,48,52],[41,45,48],[43,47,50]]
for beat in range(64):
 chord=chords[(beat//16)%4];note=chord[beat%3]+12;freq=440*2**((note-69)/12)
 u=np.arange(rate*2)/rate;env=np.minimum(u/.015,1)*np.exp(-u*3)
 a=(np.sin(2*np.pi*freq*u)+.18*np.sin(2*np.pi*freq*2*u))*env*.09
 st=int(beat*.5*rate);end=min(len(v),st+len(a));v[st:end]+=a[:end-st]
for bar in range(8):
 u=np.arange(rate*4)/rate;env=np.sin(np.pi*u/4)**1.2
 for note in chords[bar//2]:v[bar*rate*4:(bar+1)*rate*4]+=np.sin(2*np.pi*440*2**((note-69)/12)*u)*env*.018
v*=np.minimum(1,np.minimum(t/.2,(dur-t)/.6));save('last_evening',v)
