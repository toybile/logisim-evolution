# Packaging dependencies

The application is built from the full native fork with Gradle; no Logisim release JAR is used. Run scripts/build.py from a full checkout of branch logisim-panels. Build output: build/logisim-panels.jar in this experimental directory.

Portable packaging needs Python 3.11+, a Windows JDK 21+ with jpackage, and these vendor files (excluded from Git):

- temurin-windows-x64.zip and temurin-windows.json
- temurin-linux-x64.tar.gz and temurin-linux.json
- logisim-evolution-current-source.zip: a complete snapshot of the fork's tracked/new source files at build time, including the native changes and experimental interface; exclude .git, generated build output, vendor binaries and local credentials.

Runtime archives must match the SHA-256 in their official Adoptium metadata. The build script records native base identity in UPSTREAM.json; package.py adds application/runtime metadata and exact corresponding sources to both platforms.

```sh
python scripts/package.py --jdk /path/to/windows-jdk
python scripts/verify-packages.py
```

The retained 5.0.0 JAR/source files are historical inputs and are not included or used by Panels 0.2.0.
