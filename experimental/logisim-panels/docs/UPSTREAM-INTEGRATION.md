# Upstream integration status

Panels 0.2.0 is now compiled with the current native Logisim Evolution sources through Gradle. Upstream base: `4c02b9885faf3b523d6ff98f19b1d3c0e42d677c`, 5.1.0-dev. No upstream pull request has been opened.

## Completed in this fork

- Native source build and one application JAR; no released-JAR dependency or classpath override.
- Explicit Frame workspace-component and GridPainter palette APIs replace reflection into native fields.
- Native Probe retains the optional centered single-bit presentation; propagation and bus rendering remain native.
- Windows/Ubuntu workflow runs upstream JUnit tests and the Panels real-editor suites.
- New upstream TTL components remain available and survive circuit save/reopen.
- English/pt-BR, movable/resizable panels and toolbar, native properties/undo, simulation, truth equations, themes, opacity and fades are preserved.

## Before requesting upstream adoption

1. Present the interface and agree on scope with the maintainers. The original editor entry point remains selectable with `-Ppanels=false` and the running UI can return to the original arrangement.
2. Review preferences, localization, icon conventions, accessibility, keyboard focus and native API design with the project.
3. Test on real Linux and macOS desktops, different display scaling and larger/sequential/FPGA/HDL projects.
4. Bring new source formatting and checks into full alignment with upstream review conventions, and divide any proposal into reviewable pull requests against `main`.

The fork preserves upstream history on `main`; development is on `logisim-panels`. The metadata in UPSTREAM.json pins the base used by the portable downloads. Publishing the fork does not imply upstream acceptance.
