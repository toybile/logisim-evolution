"""Create self-contained Windows x64 and Linux x64 evaluation downloads."""
import argparse, hashlib, json, os, shutil, subprocess, tarfile, tempfile, zipfile
from pathlib import Path, PurePosixPath

ROOT=Path(__file__).resolve().parents[1];VERSION='0.2.0'
parser=argparse.ArgumentParser();parser.add_argument('--jdk',type=Path,required=True);args=parser.parse_args()
vendor=ROOT/'vendor';build=ROOT/'build';dist=ROOT/'dist';dist.mkdir(exist_ok=True)
def sha(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def metadata(platform):
    value=json.loads((vendor/('temurin-windows.json' if platform=='windows' else 'temurin-linux.json')).read_text(encoding='utf-8-sig'))
    return value[0] if isinstance(value,list) else value
def verify(platform,archive):
    meta=metadata(platform)
    if sha(archive)!=meta['binary']['package']['checksum']:raise ValueError('Runtime checksum mismatch: '+platform)
    return meta
linux=vendor/'temurin-linux-x64.tar.gz';windows=vendor/'temurin-windows-x64.zip'
linux_meta=verify('linux',linux);windows_meta=verify('windows',windows)
source=dist/f'Logisim-Panels-{VERSION}-sources.zip'
with zipfile.ZipFile(source,'w') as out:
    files=[ROOT/'README.md',ROOT/'LICENSE.md',vendor/'README.md',vendor/'logisim-evolution-current-source.zip',ROOT/'.gitignore',ROOT/'.gitattributes',ROOT/'README.en.md',ROOT/'UPSTREAM.json',ROOT/'panels.gradle']
    for folder in ['src','scripts','assets','docs','.github']:files.extend(p for p in (ROOT/folder).rglob('*') if p.is_file())
    for file in sorted(set(files)):
        compression=zipfile.ZIP_STORED if file.suffix=='.zip' else zipfile.ZIP_DEFLATED
        out.write(file,'logisim-panels-source/'+file.relative_to(ROOT).as_posix(),compress_type=compression)

def common(folder,platform):
    (folder/'app').mkdir(parents=True,exist_ok=True)
    shutil.copy2(build/'logisim-panels.jar',folder/'app/logisim-panels.jar')
    shutil.copytree(ROOT/'assets',folder/'examples')
    shutil.copytree(ROOT/'docs',folder/'docs')
    (folder/'licenses').mkdir();shutil.copy2(ROOT/'LICENSE.md',folder/'licenses/Logisim-GPL-3.md')
    shutil.copy2(ROOT/'src/main/resources/fonts/OFL.txt',folder/'licenses/Nunito-OFL.txt')
    shutil.copy2(ROOT/'README.md',folder/'README.md');shutil.copy2(ROOT/'README.en.md',folder/'README.en.md');shutil.copy2(source,folder/'sources.zip')
    dependencies={'logisim':json.loads((ROOT/'UPSTREAM.json').read_text(encoding='utf-8')),'java':metadata(platform),'application':{'version':VERSION,'sha256':sha(build/'logisim-panels.jar')}}
    (folder/'DEPENDENCIES.json').write_text(json.dumps(dependencies,indent=2)+'\n',encoding='utf-8')

stage=Path(tempfile.mkdtemp(prefix='package-',dir=build))
# All extracted paths are verified to remain inside this newly created build directory.
runtime=stage/'windows-runtime';runtime.mkdir()
with zipfile.ZipFile(windows) as archive:
    for item in archive.infolist():
        parts=PurePosixPath(item.filename).parts[1:]
        if not parts:continue
        target=runtime.joinpath(*parts)
        if not target.resolve().is_relative_to(runtime.resolve()):raise ValueError('Unsafe runtime archive path')
        if item.is_dir():target.mkdir(parents=True,exist_ok=True)
        else:
            target.parent.mkdir(parents=True,exist_ok=True)
            with archive.open(item) as src,target.open('wb') as dst:shutil.copyfileobj(src,dst)

inputs=stage/'inputs';inputs.mkdir()
shutil.copy2(build/'logisim-panels.jar',inputs/'logisim-panels.jar')
output=stage/'windows'
subprocess.run([str(args.jdk/'bin/jpackage.exe'),'--type','app-image','--name','Logisim Panels','--app-version',VERSION,
  '--input',str(inputs),'--main-jar','logisim-panels.jar','--main-class','local.logisim.panels.Launcher',
  '--runtime-image',str(runtime),'--dest',str(output),'--icon',str(ROOT/'assets/icon.ico'),
  '--java-options','--enable-native-access=ALL-UNNAMED'],check=True)
winfolder=output/'Logisim Panels';common(winfolder,'windows')
winzip=dist/f'Logisim-Panels-{VERSION}-windows-x64.zip'
with zipfile.ZipFile(winzip,'w') as archive:
    for file in sorted(winfolder.rglob('*')):
        if file.is_file():archive.write(file,'Logisim Panels/'+file.relative_to(winfolder).as_posix(),compress_type=zipfile.ZIP_STORED if file.suffix in ['.zip','.jar'] else zipfile.ZIP_DEFLATED)

linfolder=stage/'linux';linfolder.mkdir();common(linfolder,'linux')
for name in ['start.sh','example.sh','install-menu.sh']:shutil.copy2(ROOT/'scripts'/name,linfolder/name)
shutil.copy2(ROOT/'assets/icon.png',linfolder/'icon.png')
linux_tar=dist/f'Logisim-Panels-{VERSION}-linux-x64.tar.gz'
prefix='logisim-panels-'+VERSION
with tarfile.open(linux_tar,'w:gz',compresslevel=6) as output_tar:
    for file in sorted(linfolder.rglob('*')):
        info=output_tar.gettarinfo(str(file),prefix+'/'+file.relative_to(linfolder).as_posix())
        info.uid=info.gid=0;info.uname=info.gname='';info.mode=0o755 if file.is_dir() or file.suffix=='.sh' else 0o644
        if file.is_file():
            with file.open('rb') as f:output_tar.addfile(info,f)
        else:output_tar.addfile(info)
    # Rebase the official POSIX archive without extracting on Windows, preserving symlinks/modes.
    with tarfile.open(linux,'r:gz') as official:
        root=PurePosixPath(official.getmembers()[0].name).parts[0]
        for item in official.getmembers():
            path=PurePosixPath(item.name)
            if path.is_absolute() or '..' in path.parts or path.parts[0]!=root:raise ValueError('Unsafe Linux runtime path')
            if item.issym() and PurePosixPath(item.linkname).is_absolute():raise ValueError('Absolute runtime symlink')
            item.name=prefix+'/runtime'+('/'+PurePosixPath(*path.parts[1:]).as_posix() if len(path.parts)>1 else '')
            if item.islnk():
                link=PurePosixPath(item.linkname)
                if link.parts[0]!=root or '..' in link.parts:raise ValueError('Unsafe runtime hard link')
                item.linkname=prefix+'/runtime/'+PurePosixPath(*link.parts[1:]).as_posix()
            item.uid=item.gid=0;item.uname=item.gname=''
            stream=official.extractfile(item) if item.isfile() else None
            output_tar.addfile(item,stream)
            if stream:stream.close()

(dist/'SHA256SUMS.txt').write_text(''.join(f'{sha(file)}  {file.name}\n' for file in [winzip,linux_tar,source]),encoding='ascii')
(build/'package-paths.json').write_text(json.dumps({'windows':str(winfolder),'linux_stage':str(linfolder),'windows_runtime':str(runtime)},indent=2),encoding='utf-8')
for file in [winzip,linux_tar,source]:print(f'{file.name}: {file.stat().st_size/1024/1024:.1f} MiB')
print('Windows application:',winfolder/'Logisim Panels.exe')
