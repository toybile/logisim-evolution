# Verification - Panels 0.2.0

Base: upstream Logisim Evolution 5.1.0-dev, revision `4c02b9885faf3b523d6ff98f19b1d3c0e42d677c` (8 October 2026).

## Native-source build

The interface and the simulator/editor are compiled together by the fork's Gradle build. The application contains one JAR, with no 5.0.0 binary dependency or classpath override. The UI uses explicit native workspace and grid-palette APIs. Grid colors are generated directly by GridPainter rather than recoloring a cached image through reflection.

## Local results

- 734 original upstream JUnit tests passed: zero failures, errors or skips. Windows memory tests use Git's bundled xxd utility.
- 350 existing interface checks passed on the current native base: 35 editor integration, 8 asynchronous simulation, 71 interactions/resize/undo, 24 appearance/equation, 192 alignment and 20 language checks.
- 13 new integration checks cover TTL 74148, 744060, 74123 and 74390: availability, component creation and .circ save/reopen.
- Idle simulation still generates zero unnecessary repaint requests in the unchanged-signal scenario.
- The Panels suites also run against the bundled Temurin 21 runtime. The original JUnit suite was run with the local JDK 26, compiling Java-21-compatible classes.

## Downloads and platform limits

The Windows and Linux x64 packages bundle Temurin 21, the single current application JAR, exact corresponding application sources and licenses. Package verification checks resources, launcher paths, checksums and Linux execution permissions.

The current GitHub workflow builds and tests the actual native fork on Windows and Ubuntu/Xvfb. Its result is recorded after execution; the earlier [successful run](https://github.com/toybile/logisim-evolution/actions/runs/37790235710) tested Panels 0.1.0 against the release JAR and does not verify this migration.

A real Linux desktop test remains pending. These are targeted checks, not exhaustive validation of FPGA, HDL or all external libraries. The development base is not a new official stable Logisim release.

[Previous 0.1.0 report](VERIFICACOES-0.1.0.md)
