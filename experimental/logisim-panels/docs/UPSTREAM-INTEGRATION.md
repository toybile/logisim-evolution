# Upstream integration plan

Status: experimental extension for Logisim Evolution 5.0.0. No upstream pull request has been opened.

## Proposed behavior

Offer movable, resizable Library, Properties, Simulation and Truth table panels around the native circuit editor. Preserve access to existing tools and menus, support a resizable icon toolbar, indicate which panels are open, and allow users to save their layout. Appearance settings include themes, separate selection/signal colors, opacity, transitions and English/pt-BR.

## Before requesting integration

1. Present screenshots or a short demonstration to the upstream maintainers and agree on scope. Consider whether the layout should remain optional.
2. Port the experiment to the current upstream `main` source and Gradle build. Replace the classpath override and launcher/reflection hooks with reviewed extension points or direct source changes.
3. Use upstream localization, preferences, icon and accessibility conventions. Review focus, keyboard navigation, small screens, display scaling and theme contrast.
4. Preserve native simulation and editing behavior, undo/redo, library/FPGA/HDL access and saved `.circ` compatibility.
5. Run relevant upstream and panel checks; test on actual Windows, Linux and macOS desktops where available. Document untested combinations accurately.
6. Divide the integration into reviewable changes with before/after examples, tests and documentation. Open pull requests against upstream `main` after agreeing on the approach.

## Repository layout

The fork preserves upstream history. Its `main` branch follows the original project; `logisim-panels` adds the current experiment at `experimental/logisim-panels/`. The added root workflow verifies the standalone experiment; it does not prove compatibility with the fork's current native build.

The experimental build still uses the official 5.0.0 JAR. Releases marked `panels-*` are independent evaluation packages, with corresponding sources and license notices. They are not upstream releases.

Original contribution instructions: https://github.com/logisim-evolution/logisim-evolution/blob/main/docs/developers.md#how-to-contribute
