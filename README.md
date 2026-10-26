# CodeMirror TeaVM

CodeMirror TeaVM provides Java bindings for **CodeMirror 6** in browser
applications compiled with modern GWT or TeaVM. It includes a WebJar with the editor and its
language modules, plus Java APIs for editing, selection, search, completion and
diagnostics.

**Alpha — `0.1.0-SNAPSHOT`.** The API and packaging may change. This is a new
binding for CodeMirror 6, with one GWT/JsInterop source API. The TeaVM build uses
Instanto's GWT and Elemental2 compatibility layers and source adapter. Historical
CodeMirror 3/5 wrappers are API references; their packages and CM5 behavior are
not preserved.

[Showcase](https://instanto-io.github.io/codemirror-teavm/) ·
[Java API sources](codemirror-teavm/src/main/java/io/instanto/codemirror) ·
[Upstream research](docs/UPSTREAM.md)

## Add the library

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>codemirror-teavm</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

For GWT 2.13.1, use `codemirror-gwt` instead of `codemirror-teavm` and inherit
`io.instanto.CodeMirror` in your GWT module. Both artifacts provide the same Java
API; choose the artifact for your compiler rather than including both.

The TeaVM artifact uses the Instanto TeaVM toolchain. The coordinates use the current
snapshot, which can be installed from source. Maven Central publication is planned.

The dependency includes `codemirror-webjar`. Serve its `META-INF/resources/`
contents from your application, or copy them into your static output as shown
in [the showcase POM](codemirror-showcase/pom.xml).

## Load and create an editor

Load the bundled script once, before starting your application. For TeaVM:

```html
<div id="editor" style="height:320px"></div>
<style>
  .cm-editor { height:100%; }
  .cm-scroller { overflow:auto; }
</style>
<script src="webjars/codemirror-teavm/0.1.0/codemirror-teavm.js"></script>
<script src="app.js"></script>
<script>main();</script>
```

Mount the editor after its container is attached to the page:

```java
import io.instanto.codemirror.*;
import com.google.gwt.dom.client.Document;

Editor editor = Editor.mount(Document.get().getElementById("editor"));
editor.setLanguage(Language.JAVA);
editor.setTheme(Theme.DARK);
editor.setText("class Hello {}");
```

Keep this reference in the component that owns the editor. `EditorOptions` is
also available when supplying the text, language, theme, read-only setting, tab
size and wrapping at creation. `Editor.isAvailable()` checks that the bundle
has loaded.

## Use it as a GWT widget

`CodeMirrorWidget` works with GWT and with the TeaVM compatibility layer:

```java
CodeMirrorWidget widget = new CodeMirrorWidget();
widget.setSize("100%", "320px");
widget.setValue("class Hello {}");
widget.addValueChangeHandler(event -> saveText(event.getValue()));
RootPanel.get("editor").add(widget);
```

Import `RootPanel` from `com.google.gwt.user.client.ui`; `saveText` is your
application callback. Load the bundle before attaching the widget. `setValue`
is silent by default; `setValue(text, true)` fires a value-change event when the
value changes. User edits also fire events. `getEditor()` provides the editor API
while attached. Detaching disposes the native editor; reattaching creates a new
editor with the retained text. History is not retained across detachments.
`onResize()` asks CodeMirror to measure its container again.

The lower-level `Editor.mount` also accepts an Elemental2 `HTMLElement`.

## Read and change the document

```java
String text = editor.getText();
editor.replace(6, 11, "World");
editor.setSelection(new Selection(6, 11));
String selected = editor.getSelectedText();
```

Offsets count UTF-16 code units, like Java `String` indexes. End positions are
exclusive. A `Selection` stores its anchor and head, preserving its direction;
the typed API exposes the main selection.

`applyEdits(List<TextEdit>)` applies a batch as one transaction and one undo step.
Ranges refer to the original document and must be ordered and non-overlapping.
`setText(...)` is an undoable replacement. Use `undo()`, `redo()`, `undoDepth()`
and `redoDepth()` to work with the history.

## Respond to changes

```java
Subscription subscription = editor.onUpdate(update -> {
  if (update.documentChanged()) {
    String currentText = editor.getText();
    // Update application state for update.revision().
  }
});
```

Updates identify document and selection changes, the current selection and the
document revision. Close the subscription to remove that listener. Callbacks run
during a native view update: read state there, and schedule any editor mutation
or disposal after the callback returns, for example with `Window.setTimeout`.

## Configure editing and search

Language configurations include Java, JavaScript, TypeScript, JSON, HTML, CSS,
Python and plain text. Light and One Dark themes are bundled. Changing language,
theme, tab size or wrapping preserves the document, selection and history.

```java
editor.setTabSize(4);
editor.setLineWrapping(true);
editor.setReadOnly(true);
```

Read-only disables user edits; application code can still update the document.

For a literal search with case-sensitive matching:

```java
if (editor.setSearch("World", true, false)) {
  editor.findNext();
}
```

The third argument enables regular expressions. Invalid or empty queries return
`false` and leave the previous query intact. `findPrevious()` searches backwards.
`openSearchPanel()` opens the search/replace UI; `closeSearchPanel()` closes it.

## Supply completion results

```java
editor.setCompletionSource(context -> new CompletionResult(
    context.wordFrom(), context.position(),
    java.util.List.of(new Completion("println", "System.out.println()",
        Completion.Kind.FUNCTION, "Print to stdout"))));
editor.startCompletion();
```

A provider receives the document, cursor position, whether completion was
explicitly requested, and a suggested word boundary. It returns the replacement
range and plain-text proposals, or `null` for no result. Providers run
synchronously and should return promptly. The suggested boundary uses ASCII
word characters and `$`; supply your own range for other identifier rules.
`setCompletionSource(null)` restores the language's default completion source.

## Supply diagnostics

Record the document revision when starting analysis, then publish results for
that revision:

```java
int revision = editor.revision();
String textToAnalyze = editor.getText();
// Analyse this snapshot, then supply the results:
boolean applied = editor.setDiagnostics(revision, java.util.List.of(
    new Diagnostic(0, 5, Diagnostic.Severity.WARNING, "Example warning")));
```

The call returns `false` if the document has changed since analysis began.
Diagnostics contain a range, severity and plain-text message. Existing ranges
move with edits, but the application should recompute or clear results when
their meaning becomes stale. `clearDiagnostics()` removes them and
`diagnosticCount()` reports their number.

## Dispose of the editor

```java
subscription.close();
editor.close();
```

The editor owns the DOM it creates; the application owns the container. Close
the editor before removing that container. `close()` is terminal and safe to
call repeatedly; it discards history and registrations. Mount a new editor to
recreate the view, restoring application-held text and configuration as needed.
Call `requestMeasure()` after showing an editor that was hidden.

## Implemented scope

The showcase and Chromium/WebKit tests cover editing, atomic transactions,
Unicode offsets, selections, configuration changes, history, search/replace,
Java completion, diagnostics, listeners and disposal. Shared widget checks run
on GWT 2.13.1 and TeaVM, covering sizing, events, handler removal and reattachment.
The GWT build also checks editing, selection, history, search, diagnostics and
Java completion. TeaVM tests run in both standard and optimized builds.

Multi-selection creation, asynchronous completion, LSP transport, snippet
placeholders, diagnostic fixes, custom syntax parsers are not implemented. JVM editing and WebAssembly are outside the validated scope.

For native extensions, `setExtensions(Object)` replaces a dedicated extension
compartment and `nativeView()` exposes the underlying `EditorView`. The bundle
exports `CodeMirrorTea.state` and `CodeMirrorTea.view` for extensions that need
the same module instances. These APIs bypass Java validation. Avoid loading a
second copy of those CodeMirror modules; keep disposal and dispatch owned by the
Java wrapper.

## Build

Use JDK 21 and Maven 3.9+, with the matching Instanto TeaVM parent and compiler
artifacts installed. With the project's Playwright browsers available:

```sh
mvn clean verify
mvn -Pproduction clean verify
```

`mvn install` also installs the alpha artifacts into your local Maven repository.
The TeaVM site is written to `codemirror-showcase/target/site`; the GWT browser
contract page is in `codemirror-gwt-tests/target/site`. Both compilers and both
browser engines are exercised by the build.
[The build workflow](.github/workflows/build.yml) records browser installation
and verification. Node 22+ is needed only to regenerate the committed assets:
`npm ci && npm run build`; `npm run check:assets` checks their reproducibility.

## Assets and licenses

The WebJar includes the script, a version/hash manifest and full third-party
license texts. `codemirror@6.0.2` supplies the basic setup; its component packages
have independent versions, including `@codemirror/view@6.43.15` and
`@codemirror/state@6.7.6`. The complete inventory is in the bundled manifest.

Original Java and bridge code: **MIT**. All 29 bundled browser packages declare
MIT; their original notices and full licenses are retained in the WebJar.
The showcase also distributes the TeaVM Apache-2.0 license. Preserve `LICENSE`,
`NOTICE`, and the bundled third-party texts when redistributing. Historical GWT
wrapper licensing is recorded for research only; none of those wrappers is a
runtime/build dependency. This is an independent integration, not an official
CodeMirror project. See [NOTICE](NOTICE).
