"""Offline sound/cue authoring. Input is mono PCM16 /tmp/studvesna-analysis.wav."""
from pathlib import Path
import numpy as np
from scipy.io import wavfile
from scipy.signal import stft,find_peaks
import json
R=Path(__file__).resolve().parents[2];sr=22050;rng=np.random.default_rng(261028)
a=np.zeros(sr*9)
for t in rng.uniform(0,8.4,260):
 n=int(sr*.12);clap=rng.normal(0,1,n)*np.exp(-np.arange(n)/(sr*.018))*rng.uniform(.03,.12);start=int(t*sr);a[start:start+n]+=clap
a*=np.minimum(1,np.arange(len(a))/(sr*.6))*np.minimum(1,(len(a)-np.arange(len(a)))/(sr*1.4));a/=max(1,np.max(np.abs(a))*1.2)
wavfile.write(R/'assets/studvesna/audio/applause.wav',sr,(a*32767).astype(np.int16))
rate,y=wavfile.read('/tmp/studvesna-analysis.wav');f,t,z=stft(y.astype(float)/32768,rate,nperseg=1024,noverlap=768)
flux=np.maximum(0,np.diff(np.abs(z[(f>90)&(f<6000)]),axis=1)).mean(axis=0)
peaks,_=find_peaks(flux,distance=int(2.4*rate/256),prominence=np.quantile(flux,.72))
cues=[round(float(t[i+1]),3) for i in peaks if 5<t[i+1]<126]
(R/'resources/studvesna/rhythm.json').write_text(json.dumps({'source':'tanec_vesna.MP4','method':'Sparse positive spectral-flux peaks; optional prompts, not choreography transcription.','duration':131.743333,'cues':cues},indent=2)+'\n')
