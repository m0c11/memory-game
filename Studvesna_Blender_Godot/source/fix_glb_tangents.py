"""Repair undefined tangent directions on degenerate-UV corners/poles.

Blender can export a zero tangent at a collapsed UV corner. Preserve valid
tangents; for undefined ones use a unit vector perpendicular to the normal.
This changes no mesh positions, UVs, materials, textures or JSON topology.
"""
import json,struct,math
from pathlib import Path

def fix(path):
    path=Path(path);data=bytearray(path.read_bytes());off=12;doc=None;binary=None
    while off<len(data):
        length,kind=struct.unpack_from('<I4s',data,off);off+=8
        if kind==b'JSON':doc=json.loads(data[off:off+length])
        if kind==b'BIN\0':binary=off
        off+=length
    def offset(accessor,index,components):
        a=doc['accessors'][accessor];v=doc['bufferViews'][a['bufferView']]
        assert a['componentType']==5126
        return binary+v.get('byteOffset',0)+a.get('byteOffset',0)+index*v.get('byteStride',components*4)
    seen=set();changed=0
    for mesh in doc.get('meshes',[]):
        for prim in mesh['primitives']:
            attrs=prim['attributes']
            if 'TANGENT' not in attrs or 'NORMAL' not in attrs:continue
            ta,na=attrs['TANGENT'],attrs['NORMAL']
            if ta in seen:continue
            seen.add(ta)
            for i in range(doc['accessors'][ta]['count']):
                p=offset(ta,i,4);t=struct.unpack_from('<4f',data,p)
                length=math.sqrt(sum(v*v for v in t[:3]))
                if .99999<length<1.00001:continue
                n=struct.unpack_from('<3f',data,offset(na,i,3))
                if length<1e-8 or not math.isfinite(length):
                    axis=min(range(3),key=lambda j:abs(n[j]));ref=[0.,0.,0.];ref[axis]=1.
                    v=[n[1]*ref[2]-n[2]*ref[1],n[2]*ref[0]-n[0]*ref[2],n[0]*ref[1]-n[1]*ref[0]]
                    length=math.sqrt(sum(c*c for c in v))
                else:v=list(t[:3])
                if length<1e-8:raise ValueError('Undefined vertex normal')
                struct.pack_into('<4f',data,p,*(c/length for c in v),1. if t[3]>=0 else -1.)
                changed+=1
    if changed:path.write_bytes(data)
    return changed

if __name__=='__main__':
    root=Path(__file__).resolve().parents[1]
    total=0
    for p in sorted((root/'models').glob('*.glb')):
        n=fix(p);total+=n
        if n:print(p.name,n)
    print('Repaired tangent corners:',total)
