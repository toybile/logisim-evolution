"""Reproducible Java build. Requires JDK 21+; no third-party Python packages."""
import argparse, os, shutil, subprocess, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--jdk',type=Path);parser.add_argument('--java',type=Path);parser.add_argument('--tests',action='store_true');parser.add_argument('--check',action='append')
args=parser.parse_args()
jdk=args.jdk or (Path(os.environ['JAVA_HOME']) if os.environ.get('JAVA_HOME') else None)
if jdk is None:
    compiler=shutil.which('javac');jdk=Path(compiler).resolve().parent.parent if compiler else None
if jdk is None:sys.exit('Set JAVA_HOME or pass --jdk pointing to JDK 21 or newer.')
suffix='.exe' if os.name=='nt' else ''
def run(tool,*params):
    executable=args.java if tool=='java' and args.java else jdk/'bin'/(tool+suffix)
    subprocess.run([str(executable),*map(str,params)],cwd=ROOT,check=True)

build=ROOT/'build';build.mkdir(exist_ok=True)
# Each compilation starts in a fresh directory, so stale classes cannot enter releases.
import tempfile
with tempfile.TemporaryDirectory(prefix='classes-',dir=build) as temp:
    classes=Path(temp)
    native=ROOT/'vendor/logisim-evolution-5.0.0-all.jar'
    if not native.is_file():sys.exit('Missing vendor/logisim-evolution-5.0.0-all.jar. See README.md.')
    run('javac','--release','21','-encoding','UTF-8','-cp',native,'-d',classes,*sorted((ROOT/'src/main/java').rglob('*.java')))
    shutil.copytree(ROOT/'src/main/resources',classes,dirs_exist_ok=True)
    interface=build/'interface.jar'
    run('jar','-J-Djava.io.tmpdir='+str(build),'--create','--file',interface,'--main-class','local.logisim.panels.Launcher','-C',classes,'.')
    if args.tests or args.check:
        tests=classes/'test';tests.mkdir()
        run('javac','--release','21','-encoding','UTF-8','-cp',str(interface)+os.pathsep+str(native),'-d',tests,*sorted((ROOT/'src/test/java').rglob('*.java')))
        checks=build/'checks';checks.mkdir(exist_ok=True)
        for name in args.check or ['Checks','AsyncChecks','UxChecks','AppearanceChecks','PinAlignmentChecks','LanguageChecks','IdleChecks']:
            run('java','--enable-native-access=ALL-UNNAMED','-Dlogisim.panels.home='+str(checks),'-cp',os.pathsep.join(map(str,[tests,interface,native])),'local.logisim.panels.'+name,checks)
print('Built',build/'interface.jar')
