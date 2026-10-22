import json, struct, math
from pathlib import Path
BASE=Path(__file__).resolve().parents[1]
results=[]
for path in sorted((BASE/'models').glob('*.glb')):
    raw=path.read_bytes(); errors=[]
    magic,version,total=struct.unpack_from('<4sII',raw)
    if (magic,version,total)!=(b'glTF',2,len(raw)): errors.append('Invalid GLB header')
    length,kind=struct.unpack_from('<II',raw,12)
    doc=json.loads(raw[20:20+length])
    for i,im in enumerate(doc.get('images',[])):
        if 'bufferView' not in im: errors.append(f'Image {i} is not embedded')
    for i,m in enumerate(doc.get('materials',[])):
        pbr=m.get('pbrMetallicRoughness',{})
        for field in ('baseColorTexture','metallicRoughnessTexture'):
            if field not in pbr: errors.append(f'Material {i}: missing {field}')
        if 'normalTexture' not in m: errors.append(f'Material {i}: missing normal map')
    for i,mesh in enumerate(doc.get('meshes',[])):
        for prim in mesh['primitives']:
            for field in ('POSITION','NORMAL','TEXCOORD_0'):
                if field not in prim['attributes']: errors.append(f'Mesh {i}: missing {field}')
    if 'KHR_lights_punctual' in doc.get('extensionsUsed',[]):errors.append('Unexpected exported light')
    if path.stem=='door_hinged_090x210':
        if not any(n.get('name','').startswith('Door_Hinge') for n in doc['nodes']):errors.append('Missing hinge node')
        leaf_meshes=[doc['meshes'][n['mesh']] for n in doc['nodes'] if n.get('name','').startswith('Door_Leaf') and 'mesh' in n]
        acc=[doc['accessors'][pr['attributes']['POSITION']] for m in leaf_meshes for pr in m['primitives']]
        xmin=min(a['min'][0] for a in acc);xmax=max(a['max'][0] for a in acc)
        if not -.05 < xmin < .05 or not .8 < xmax < 1.:errors.append('Door origin is not at hinge edge')
    results.append({'file':path.name,'bytes':len(raw),'meshes':len(doc.get('meshes',[])),'materials':len(doc.get('materials',[])),'embedded_images':len(doc.get('images',[])),'errors':errors})
report={'files':len(results),'passed':sum(not r['errors'] for r in results),'errors':sum(len(r['errors']) for r in results),'results':results}
(BASE/'docs/glb_validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
print(json.dumps({k:v for k,v in report.items() if k!='results'}))
for r in results:
    if r['errors']:print(r)
if report['files']!=59 or report['errors']:raise SystemExit(1)
