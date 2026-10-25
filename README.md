# CodeMirror TeaVM

A Java-first integration of **CodeMirror 6** for TeaVM. An initial, working library
with a local browser bundle, typed APIs, and a Java-driven interactive showcase.

[Showcase](https://instanto-io.github.io/codemirror-teavm/) ·
[Upstream research](docs/UPSTREAM.md) · [MIT license](LICENSE)

## What is implemented

* Explicit mount/close lifecycle; idempotent disposal; guarded access after close.
* Text retrieval, replacements and atomic batches; UTF-16 directional selections.
* Removable typed update listeners, document revisions, and clear reentrancy errors.
* Java, JavaScript, TypeScript, JSON, HTML, CSS, Python and plain text configuration.
* Light/One Dark themes, read-only input, wrapping and tab size. Reconfiguration
  preserves document, selection and undo history.
* Undo/redo, history depth, search/replace UI, literal/regex query navigation.
* Synchronous Java completion providers with typed plain-text items.
* Java-supplied diagnostics with severity, range, and stale-revision rejection.
* A custom extension compartment and native `EditorView` escape hatch for advanced
  CodeMirror integrations.

This is **not source-compatible with GWT CodeMirror wrappers or CodeMirror 5**.
It uses TeaVM JSO directly, without a GWT runtime dependency. See the verified
[wrapper/version/license comparison](docs/UPSTREAM.md). There is no copied wrapper
source and no CM5 mode, add-on, theme, or `fromTextArea` compatibility layer.

## Build and run

Use JDK 21 and Maven 3.9+. Java library bytecode targets Java 17. The organization
parent currently pins TeaVM **0.16.0**, including the shared compiler corrections.
Node 22+ is needed only when regenerating or verifying the committed browser
assets, not for a normal Maven build or a consuming application.

The build inherits `io.instanto:instanto-teavm-pom:0.1.0-SNAPSHOT` from
[instanto-poms](https://github.com/instanto-io/instanto-poms). Configure the Instanto
snapshot registry in Maven settings (see `.github/maven-settings.xml` and its
`FORGEJO_PACKAGE_USER` / `FORGEJO_PACKAGE_TOKEN` environment placeholders), or
install that parent and its managed TeaVM compiler corrections locally first.
The repository does not contain credentials. These initial library snapshots
are built locally; inclusion here is not a claim of a Maven Central release.

```sh
mvn clean install -DskipTests
mvn -f codemirror-showcase/pom.xml org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.classpathScope=test -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install chromium webkit"
mvn verify
python3 -m http.server 8080 --directory codemirror-showcase/target/site
```

Open `http://localhost:8080/`. Linux browser installations may need Playwright's
`install --with-deps chromium webkit`. `mvn -Pproduction clean verify` exercises
TeaVM ADVANCED optimization. Tests run against the actual compiled Java API in
Chromium and WebKit, including keyboard editing, undo, search/replace, completion,
diagnostics, lifecycle, Unicode offsets, invalid inputs, mobile layout, missing
assets and local license links. Reports/screenshots are under the showcase's
`target/` directory. No browser execution is needed on the JVM consumer side.

## Add to an application

After installing the reactor, add:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>codemirror-teavm</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

The transitive `codemirror-webjar` contains:

```
META-INF/resources/webjars/codemirror-teavm/0.1.0/
  codemirror-teavm.js
  manifest.json
  THIRD-PARTY-LICENSES.txt
```

Serve these resources with your application, or unpack `META-INF/resources` as in
`codemirror-showcase/pom.xml`. Load the bundle **once, before your TeaVM app**:

```html
<div id="editor" style="height: 320px"></div>
<script src="webjars/codemirror-teavm/0.1.0/codemirror-teavm.js"></script>
<script src="app.js"></script>
<script>main();</script>
```

```java
import io.instanto.codemirror.*;
import org.teavm.jso.dom.html.HTMLDocument;

var editor = Editor.mount(HTMLDocument.current().getElementById("editor"),
    new EditorOptions("class Hello {}", Language.JAVA, Theme.DARK, false, 4, false));
var subscription = editor.onUpdate(update -> {
    if (update.documentChanged()) {
        System.out.println("Revision " + update.revision());
    }
});
editor.replace(6, 11, "World");
editor.setSelection(new Selection(6, 11));
editor.setLanguage(Language.JAVA); // history survives reconfiguration

// When the owning widget is detached or the screen is disposed:
subscription.close();
editor.close();
```

The editor owns its generated DOM; the application owns the mount container.
Give `.cm-editor` a height of `100%` and `.cm-scroller` `overflow:auto` to fill a
fixed-height container. Call `requestMeasure()` after revealing a hidden editor.
Mount once after attachment and close before removal. Reattachment creates a new
`Editor`; persist text/configuration in your owning component. Closing discards
history and registrations. `close()` is terminal and safe to call twice.

## Java completion and diagnostics

```java
editor.setCompletionSource(context -> new CompletionResult(
    context.wordFrom(), context.position(),
    java.util.List.of(new Completion("println", "System.out.println()",
        Completion.Kind.FUNCTION, "Print to stdout"))));
editor.startCompletion();

int revision = editor.revision();
String textToAnalyze = editor.getText();
// Analyze this snapshot, then publish only if it is still current:
boolean applied = editor.setDiagnostics(revision, java.util.List.of(
    new Diagnostic(0, 5, Diagnostic.Severity.WARNING, "Example warning")));
```

`setCompletionSource(null)` restores language defaults. Returning null from a
provider yields no custom result for that request. Results contain plain text;
no completion or diagnostic HTML is interpreted. Providers are synchronous and
must be fast. The suggested word boundary is ASCII (`[\\w$]`); providers may return
their own range for other identifiers. Diagnostics are mapped as text changes;
applications should recompute or clear semantically obsolete results.

Update notifications run during the native view update. Read state freely, but
schedule mutations/close after the listener returns (e.g. `Window.setTimeout`).
Read-only prevents **user** edits, not application transactions. `setText` is one
undoable replacement, not a history reset. Edit ranges are exclusive and use the
original document; batches must be sorted and non-overlapping. Java `String`
offsets and CM6 offsets both count UTF-16 code units, so emoji often occupy two.

## Extensibility and boundaries

The typed selection API covers the main selection. Multi-selection creation,
async completion/cancellation, LSP transport, snippets with placeholders,
diagnostic quick-fix actions, custom syntax parsers, and a GWT `Widget` adapter
are not part of this first slice. The editor is a browser API, not a JVM editor.
Only the JavaScript TeaVM target is verified; no WebAssembly compatibility claim.

For custom native extensions, `setExtensions(JSObject)` replaces a dedicated
compartment, and `nativeView()` exposes the view. The bundle exports
`CodeMirrorTea.state` and `CodeMirrorTea.view` so extensions can share the same
module instances. Do not introduce a second copy of `@codemirror/state` or
`@codemirror/view`, directly destroy the native view, or change its dispatch
handler. These escape hatches bypass Java validation and require upstream CM6
knowledge. Built-in languages and themes are deliberately bundled together;
there is no lazy loader or runtime CDN dependency.

## Assets and licensing

`codemirror@6.0.2` supplies basic setup; individual CM6 packages have their own
versions. This distribution includes `@codemirror/view@6.43.15` and
`@codemirror/state@6.7.6`. The complete manifest lives alongside the browser script.

```sh
npm ci
npm run build         # reproduce bundle, license inventory and SHA-256 manifest
npm run check:assets  # fail if regeneration differs from committed assets
```

Original Java and bridge code: **MIT**. All 29 bundled browser packages declare
MIT; their original notices and full licenses are retained in the WebJar.
The showcase also distributes the TeaVM Apache-2.0 license. Preserve `LICENSE`,
`NOTICE`, and the bundled third-party texts when redistributing. Historical GWT
wrapper licensing is recorded for research only; none of those wrappers is a
runtime/build dependency. This is an independent integration, not an official
CodeMirror project. See [NOTICE](NOTICE).
