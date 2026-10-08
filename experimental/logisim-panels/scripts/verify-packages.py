"""Inspect release contents and executable permissions without running Linux binaries."""
from pathlib import Path
import hashlib, json, tarfile, zipfile
root=Path(__file__).resolve().parents[1];dist=root/'dist';checks=0
def require(ok,message):
    global checks
    checks+=1
    if not ok:raise AssertionError(message)
    print('OK',message)
for line in (dist/'SHA256SUMS.txt').read_text().splitlines():
    digest,name=line.split('  ',1)
    with (dist/name).open('rb') as f:require(hashlib.file_digest(f,'sha256').hexdigest()==digest,'SHA-256 '+name)
with zipfile.ZipFile(dist/'Logisim-Panels-0.2.0-windows-x64.zip') as archive:
    prefix='Logisim Panels/'
    for name in ['Logisim Panels.exe','runtime/bin/java.exe','app/logisim-panels.jar','sources.zip','licenses/Logisim-GPL-3.md','licenses/Nunito-OFL.txt','examples/Exemplo.circ']:
        require(prefix+name in archive.namelist(),'Windows includes '+name)
    config=archive.read(prefix+'app/Logisim Panels.cfg').decode('utf-8')
    require('logisim-panels.jar' in config and 'interface.jar' not in config,'Windows uses one native-source application JAR')
    require('C:\\' not in config,'Windows launcher has relative application paths')
    jar=archive.read(prefix+'app/logisim-panels.jar')
    import io
    with zipfile.ZipFile(io.BytesIO(jar)) as interface:
        require(not any('Checks.class' in name for name in interface.namelist()),'Test classes are excluded from the release')
        require('local/logisim/panels/en.tsv' in interface.namelist(),'Language resource included')
        require('local/logisim/panels/theme/FlatLaf.properties' in interface.namelist(),'Theme resource has the correct path')
with tarfile.open(dist/'Logisim-Panels-0.2.0-linux-x64.tar.gz','r:gz') as archive:
    prefix='logisim-panels-0.2.0/'
    members={item.name:item for item in archive.getmembers()}
    for name in ['start.sh','example.sh','install-menu.sh','runtime/bin/java']:
        require(bool(members[prefix+name].mode&0o111),'Linux executable permission for '+name)
    require(archive.extractfile(prefix+'runtime/bin/java').read(4)==b'\x7fELF','Linux runtime is an ELF binary')
    for item in archive.getmembers():
        require(not item.name.startswith('/') and '..' not in Path(item.name).parts,'Safe Linux member path') if '..' in Path(item.name).parts or item.name.startswith('/') else None
    start=archive.extractfile(prefix+'start.sh').read()
    require(b'\r' not in start,'Linux launcher has POSIX line endings')
    require(b'-jar' in start and b'logisim-panels.jar' in start,'Linux uses one native-source application JAR')
    require(prefix+'sources.zip' in members,'Linux includes corresponding application source')
    require(prefix+'runtime/legal/java.base/LICENSE' in members,'Linux includes Java legal notices')
with zipfile.ZipFile(dist/'Logisim-Panels-0.2.0-sources.zip') as sources:
    names=sources.namelist()
    require('logisim-panels-source/vendor/logisim-evolution-current-source.zip' in names,'Source download includes original source')
    require('logisim-panels-source/src/main/java/com/cburch/logisim/std/wiring/Probe.java' in names,'Source download includes modified Probe')
    require(not any('/build/' in name or '/.git/' in name for name in names),'Source download excludes generated files and local Git internals')
print('PASS:',checks,'package checks')
