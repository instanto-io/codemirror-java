# Upstream research and API decisions

Verified 25 October 2026 from upstream source trees and published npm package
manifests/license files. Wrapper release numbers are not CodeMirror versions.

| Reference | Verified version evidence | License evidence | Decision |
| --- | --- | --- | --- |
| [Protégé codemirror-gwt](https://github.com/protegeproject/codemirror-gwt/tree/20c82f2c3fc4b7867b2506a21042a781c7e6fbb3) | [POM](https://github.com/protegeproject/codemirror-gwt/blob/20c82f2c3fc4b7867b2506a21042a781c7e6fbb3/pom.xml) says `3.0.1-SNAPSHOT`; README example says `2.0.0`. [Bundled JavaScript](https://github.com/protegeproject/codemirror-gwt/blob/20c82f2c3fc4b7867b2506a21042a781c7e6fbb3/src/main/java/edu/stanford/bmir/gwtcodemirror/client/js/codemirror/lib/codemirror.js) assigns `CodeMirror.version = "5.25.0"`. | POM lists both Apache-2.0 and LGPL-3.0; the bundled CodeMirror header specifies MIT. The missing root `LICENSE` and multiple declarations are not treated as a clear single-license grant for all wrapper files. | Useful ideas: widget ownership, typed value changes, handler removal, completion callbacks. No code copied or translated. |
| [Geomajas wrapper](https://github.com/geomajas/geomajas-project-codemirror-gwt/tree/8c6db1705084e73428d0ed22bee5bd7c656bd355) | [POM](https://github.com/geomajas/geomajas-project-codemirror-gwt/blob/8c6db1705084e73428d0ed22bee5bd7c656bd355/pom.xml) says wrapper `3.13.0-SNAPSHOT`, describes CodeMirror 3.1; [bundled JavaScript](https://github.com/geomajas/geomajas-project-codemirror-gwt/blob/8c6db1705084e73428d0ed22bee5bd7c656bd355/codemirror/src/main/resources/org/geomajas/codemirror/public/lib/codemirror.js) confirms `3.1`. | [Root LICENSE.txt](https://github.com/geomajas/geomajas-project-codemirror-gwt/blob/8c6db1705084e73428d0ed22bee5bd7c656bd355/LICENSE.txt) is Apache-2.0; upstream browser asset licensing is separate. | Too old as a behavioral baseline for CM6. No code copied. |
| [Gerrit GWT glue](https://gerrit.googlesource.com/gerrit.git/+/ed3d537e3a03e320c285faed9dd61465065e15e6/gerrit-gwtui/src/main/java/net/codemirror/lib/CodeMirror.java) | Historical `JavaScriptObject`/JSNI API uses `CodeMirror(parent,cfg)` and named options. This inspected file alone does not establish an exact bundled CM version. | File header is Apache-2.0. | An additional historical reference, not a dependency or source baseline. |

No related TeaVM CodeMirror repository was found in the organization or the
GitHub repository search. Local matching assets belonged to older applications.
The Java API is shared between modern GWT and TeaVM. The GWT artifact packages
the canonical Java/JsInterop sources. The TeaVM artifact uses teavm-compat's
source adapter, GWT widgets and Elemental2 implementations. The editor lifecycle,
value events and handler removal follow GWT conventions; the editor operations
follow CodeMirror 6. No historical wrapper source was copied.

## CodeMirror 6 architecture

The [official migration guide](https://codemirror.net/docs/migration/),
[system guide](https://codemirror.net/docs/guide/) and
[reference manual](https://codemirror.net/docs/ref/) are the behavioral baseline:

* `EditorView` owns the DOM, `EditorState` holds immutable state, and dispatched
  transactions carry changes. Ordered edits use offsets in the original document.
* Positions are zero-based UTF-16 offsets. Line numbers in upstream CM6 are
  one-based. The Java API does not pretend CM5 `{line,ch}` positions still apply.
* Language support is an extension, not a global mode string or a separately
  injected `mode-*.js` file. Configuration compartments preserve history.
* View update listeners replace CM5's named event emitter. Notifications are
  synchronous and editor mutation must be scheduled after the listener returns.
* Themes, completion, lint and search are extensions. CM5 CSS themes and add-ons
  cannot be loaded into this bundle. No `fromTextArea` or implicit form syncing.
* Destroying a view releases its DOM observers/listeners. Container removal alone
  does not establish lifecycle correctness; owners explicitly call `close()`.

CM6 packages version independently. `codemirror@6.0.2` is the basic-setup package,
not the version of the entire editor. This build pins `@codemirror/view@6.43.15`,
`@codemirror/state@6.7.6`, and every direct dependency in `package.json`; the lock
file pins transitive dependencies and integrity hashes. Package manifests now
point much of upstream development to [CodeMirror's Forgejo](https://code.haverbeke.berlin/codemirror).
The committed WebJar manifest records all 29 browser packages, their versions,
repository URLs, declared licenses, and the final bundle SHA-256. The generation
script verifies a license file exists for every bundled package and includes its
full text. All packages in this initial browser bundle declare MIT.

These findings establish the pinned baseline, not a promise that it will always
be the newest release. Updates must regenerate assets, review notices, and rerun
both browser engines before publishing.
