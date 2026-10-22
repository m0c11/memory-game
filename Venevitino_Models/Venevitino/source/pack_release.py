from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import json
BASE=Path(__file__).resolve().parents[1]
manifest=json.loads((BASE/'docs/asset_manifest.json').read_text())
lines=['# Каталог моделей','', 'Габариты указаны по осям Blender X × Y × Z, в метрах. В Godot вертикальная ось — Y.','', '| Модель | Файл в models | Габариты, м | Треугольники |','| --- | --- | --- | --- |']
for item in manifest:
    dims=' × '.join(str(v) for v in item['size_m'])
    lines.append(f"| {item['name_ru']} | `{Path(item['file']).name}` | {dims} | {item['triangles']:,} |")
(BASE/'docs/CATALOG_RU.md').write_text('\n'.join(lines)+'\n')
target=BASE.parent/'Venevitino_Models.zip'
with ZipFile(target,'w',ZIP_DEFLATED,compresslevel=6) as z:
    for p in sorted(BASE.rglob('*')):
        if p.is_file() and '__pycache__' not in p.parts and p.suffix not in ('.tmp','.blend1','.pyc'):
            z.write(p,p.relative_to(BASE.parent))
with ZipFile(target) as z:
    assert z.testzip() is None
    assert len([n for n in z.namelist() if n.endswith('.glb')])==59
    assert 'Venevitino/renders/Venevitino_All_Models.png' in z.namelist()
print('RELEASE',str(target),'bytes',target.stat().st_size,flush=True)
