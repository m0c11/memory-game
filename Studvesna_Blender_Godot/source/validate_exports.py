"""Validate embedded GLB textures, UVs, geometry and package completeness."""
from pathlib import Path
from io import BytesIO
from PIL import Image
import struct,json,math
ROOT=Path(__file__).resolve().parents[1]

def read_glb(p):
    data=p.read_bytes();magic,version,total=struct.unpack_from('<4sII',data)
    assert magic==b'glTF' and version==2 and total==len(data),(p,'invalid header')
    off=12;doc=None;binary=None
    while off<len(data):
        size,kind=struct.unpack_from('<I4s',data,off);off+=8
        block=data[off:off+size];off+=size
        if kind==b'JSON':doc=json.loads(block)
        elif kind==b'BIN\0':binary=block
    return doc,binary

def validate():
    results=[];errors=[]
    for p in sorted((ROOT/'models').glob('*.glb')):
        d,b=read_glb(p);tri=0;uv_missing=0;bad_images=[]
        for mesh in d.get('meshes',[]):
            for prim in mesh['primitives']:
                if prim.get('mode',4)==4:tri+=d['accessors'][prim['indices']]['count']//3
                if 'TEXCOORD_0' not in prim['attributes']:uv_missing+=1
        for im in d.get('images',[]):
            try:
                assert 'bufferView' in im,'external image'
                view=d['bufferViews'][im['bufferView']];a=view.get('byteOffset',0)
                with Image.open(BytesIO(b[a:a+view['byteLength']])) as img:img.load()
            except Exception as e:bad_images.append(str(e))
        no_texture=[m.get('name','?') for m in d.get('materials',[]) if 'baseColorTexture' not in m.get('pbrMetallicRoughness',{})]
        result={'file':p.name,'meshes':len(d.get('meshes',[])),'triangles':tri,'materials':len(d.get('materials',[])),'embedded_images':len(d.get('images',[])),'uv_missing':uv_missing,'untextured_materials':no_texture,'invalid_images':bad_images,'bytes':p.stat().st_size}
        results.append(result)
        if uv_missing or no_texture or bad_images:errors.append(result)
    report={'model_count':len(results),'all_checks_passed':not errors and len(results)==33,'total_triangles':sum(r['triangles'] for r in results),'models':results,'errors':errors}
    (ROOT/'validation_report.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps({'count':len(results),'all_checks_passed':report['all_checks_passed'],'triangles':report['total_triangles'],'errors':errors},indent=2))
    return report

if __name__=='__main__':validate()
