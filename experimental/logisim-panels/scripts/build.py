"""Build Panels with the full fork's current Gradle sources. Requires JDK 21+."""
import argparse, os, shutil, subprocess, sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--jdk',type=Path)
parser.add_argument('--java',type=Path,help='Optional Java executable for the Panels integration checks')
parser.add_argument('--upstream',type=Path,help='Full fork checkout, including build.gradle.kts')
parser.add_argument('--tests',action='store_true')
parser.add_argument('--check',action='append')
args=parser.parse_args()
native=args.upstream
if native is None:
    candidate=ROOT.parents[1] if ROOT.parent.name=='experimental' else ROOT/'build/upstream'
    native=candidate if (candidate/'build.gradle.kts').is_file() else None
if native is None:
    sys.exit('Use a full checkout of toybile/logisim-evolution (logisim-panels branch), or pass --upstream pointing to it. The build no longer downloads the 5.0.0 JAR.')
env=dict(os.environ)
if args.jdk:env['JAVA_HOME']=str(args.jdk.resolve())
if os.name=='nt':
    xxd=Path(env.get('ProgramFiles','C:/Program Files'))/'Git/usr/bin'
    if (xxd/'xxd.exe').is_file():env['PATH']=str(xxd)+os.pathsep+env.get('PATH','')
wrapper=native/('gradlew.bat' if os.name=='nt' else 'gradlew')
tasks=['shadowJar']
if args.tests:tasks+=['test','verifyPanels']
elif args.check:tasks+=['verifyPanels'+name for name in args.check]
command=([str(wrapper)] if os.name=='nt' else ['sh',str(wrapper)])+['--no-daemon','--no-configuration-cache',*tasks]
if args.java:command.append('-PpanelsJava='+str(args.java.resolve()))
subprocess.run(command,cwd=native,env=env,check=True)
artifacts=list((native/'build/libs').glob('*-all.jar'))
if len(artifacts)!=1:sys.exit('Expected one current native application JAR.')
(ROOT/'build').mkdir(exist_ok=True)
shutil.copy2(artifacts[0],ROOT/'build/logisim-panels.jar')
print('Built single native application:',ROOT/'build/logisim-panels.jar')
