# CodeMirror Java

CodeMirror Java lets you embed a **CodeMirror 6** text editor in a browser
application written in Java. Your application can read and change the document,
respond to edits, offer completions and display validation messages through a
Java API. The editor itself runs in the browser, with builds of the library for
both **GWT** and **TeaVM**.

**Alpha — `0.1.0-SNAPSHOT`.** The API and packaging may change. We target
CodeMirror 6 and maintain one GWT/JsInterop source API for both platforms.

[Showcase](https://instanto-io.github.io/codemirror-java/) ·
[GWT setup](#gwt) · [TeaVM setup](#teavm) · [Shared API](#shared-api) · [Diff and merge](#diff-and-merge) ·
[Upstream research](docs/UPSTREAM.md)

## What the library provides

You can use the editor for a JSON configuration form, a source-code editing pane,
a template editor or a read-only code viewer. CodeMirror handles the text view,
keyboard editing, syntax highlighting, selection and undo history. Your Java
application supplies the document and decides what to do with changes: save them,
validate them, or use them elsewhere in the UI.

The Java API wraps common CodeMirror operations in types such as `Editor`,
`Selection`, `Completion` and `Diagnostic`. For these features, you work with Java
methods and callbacks instead of constructing CodeMirror's JavaScript state and
view objects yourself. `CodeMirrorWidget` adds GWT panel integration, sizing and
value-change events for applications built around widgets. `CodeMirrorMergeWidget`
provides side-by-side and unified comparison, with editing and merge controls.

We maintain one GWT/JsInterop source API for both compiler targets. The GWT build
uses GWT's DOM and widget APIs directly. The TeaVM build adapts those same sources
using `gwt-user-compat` and `elemental2-compat`, so the editor and widget code shown here
can be used with either compiler. Both builds call the same bundled CodeMirror
JavaScript implementation in the browser.

The bundle includes the language support and themes listed below. Your application
still supplies its own persistence and any domain-specific validation or completion
logic. For example, selecting Java syntax highlighting does not supply a Java
compiler or a language server. The [implemented scope](#implemented-scope) describes
which features have Java bindings and which remain outside this alpha.

## Choose your platform

| Platform | Library artifact | Verified compiler |
| --- | --- | --- |
| GWT | `io.instanto:codemirror-gwt:0.1.0-SNAPSHOT` | GWT 2.13.1 |
| TeaVM | `io.instanto:codemirror-teavm:0.1.0-SNAPSHOT` | TeaVM 0.16.0 |

Choose the artifact for the compiler that builds your application. Both bring in
`io.instanto:codemirror-webjar`, a JAR containing the prebuilt browser bundle and
its license notices. You use the same Java packages and editor API in either case.
The `codemirror-teavm` name in existing browser asset paths is retained for
compatibility; those assets are also used by the GWT build.

The examples below use development snapshot coordinates. Build and install the
artifacts from source for local use; a Maven Central release is not yet available.

## GWT

### Dependency and module

In a GWT application, the dependency provides the Java sources that GWT compiles
alongside your own code. Add it to your application's POM:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>codemirror-gwt</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

The GWT module declaration makes those sources available to the compiler. Add
this entry to your application's `.gwt.xml` file:

```xml
<inherits name="io.instanto.CodeMirror"/>
```

`codemirror-gwt` includes Java sources and the GWT module definition. It uses
`org.gwtproject:gwt-user:2.13.1` and `com.google.elemental2:elemental2-dom:1.3.2`.

### Host page and entry point

The host page provides a place for the editor and starts your GWT application.
For a module renamed to `app`, a minimal page contains the following elements.
The container has a height so there is visible space for the editor.

```html
<div id="editor" style="height:320px"></div>
<script src="app/app.nocache.js"></script>
```

In the entry point, ask `CodeMirrorResources` to load the editor bundle. Its first
callback runs when the editor is ready to create; the second lets your application
show a useful message if loading fails. `Editor.mount(...)` then creates the
editor inside the container and returns the Java object used to control it.

The bundle must be available on your website for this to work. The
[browser assets section](#browser-assets) explains how to arrange that during
your build.

```java
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import io.instanto.codemirror.Editor;
import io.instanto.codemirror.Language;
import io.instanto.codemirror.CodeMirrorResources;

public final class App implements EntryPoint {
  private Editor editor;

  @Override public void onModuleLoad() {
    CodeMirrorResources.whenReady(() -> {
      editor = Editor.mount(Document.get().getElementById("editor"));
      editor.setLanguage(Language.JAVA);
      editor.setText("class Hello {}");
    }, error -> Document.get().getElementById("editor")
        .setInnerText("Unable to load editor: " + error.getMessage()));
  }
}
```

Register your fully qualified `App` class as an `<entry-point>` in the application's
GWT module. Close the editor when its owning screen is disposed. For managed
attachment/detachment, use [CodeMirrorWidget](#widget-lifecycle).
A working compiler configuration and browser contract page are in
[codemirror-gwt-tests](codemirror-gwt-tests/pom.xml).

## TeaVM

### Dependency and build modules

In a TeaVM application, use the artifact prepared for the Instanto TeaVM 0.16.0
toolchain. It exposes the same Java API as the GWT artifact, with the compatibility
implementations needed to run it through TeaVM:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>codemirror-teavm</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

The compatibility work is shared with the other Instanto libraries. These modules
provide the GWT and Elemental2 APIs used by the editor, and adapt the library's
sources during its build:

| Module | Purpose |
| --- | --- |
| `io.instanto:gwt-user-compat:0.1.0-SNAPSHOT` | GWT DOM and widget API implementations for TeaVM |
| `io.instanto:elemental2-compat:1.2.3-SNAPSHOT` | Elemental2 API bindings for TeaVM |
| `io.instanto:gwt-resources-compat-maven-plugin:0.1.0-SNAPSHOT` | Generates TeaVM sources from the canonical GWT/JsInterop sources during the library build |
| `io.instanto:instanto-teavm-pom:0.1.0-SNAPSHOT` | Shared TeaVM dependency and compiler configuration |

The runtime modules are transitive dependencies. The source adapter runs when
building this library; consuming its prepared artifact does not require rerunning
that adapter over the library sources. The application still needs its TeaVM
compiler configuration and `org.teavm:teavm-classlib` dependency. See the complete
[showcase POM](codemirror-showcase/pom.xml) for a working example.

### Host page and main method

The host page starts the JavaScript compiled from your application. That
application then loads CodeMirror through the same Java resource loader used by
the GWT example. This keeps resource loading and error handling in your application
code. Give the editor a container with a height:

```html
<div id="editor" style="height:320px"></div>
<script src="app.js"></script>
<script>main();</script>
```

Configure your fully qualified `App` class as TeaVM's `mainClass`, and `app.js`
as its output file. The main method waits for CodeMirror, creates the editor and
sets its initial language and text. As with GWT, the bundle needs to be copied
into the website as explained under [Browser assets](#browser-assets).

```java
import com.google.gwt.dom.client.Document;
import io.instanto.codemirror.Editor;
import io.instanto.codemirror.Language;
import io.instanto.codemirror.CodeMirrorResources;

public final class App {
  private static Editor editor;

  public static void main(String[] args) {
    CodeMirrorResources.whenReady(() -> {
      editor = Editor.mount(Document.get().getElementById("editor"));
      editor.setLanguage(Language.JAVA);
      editor.setText("class Hello {}");
    }, error -> Document.get().getElementById("editor")
        .setInnerText("Unable to load editor: " + error.getMessage()));
  }
}
```

`Document` above comes from `io.instanto:gwt-user-compat`. The same `Editor` and
`CodeMirrorWidget` APIs are available on both platforms. The JavaScript target is
verified; WebAssembly is outside the current tested scope.

## Browser assets

Adding the Maven dependency gives your Java code access to the editor API. The
browser also needs the JavaScript that implements CodeMirror. We ship that code
as a prebuilt bundle inside `codemirror-webjar`, together with its language
support, themes and license notices.

There are two parts to making that bundle available. During the build, its files
are copied from the JAR into the website you will deploy. This is what we mean by
*asset staging*. When someone opens the application, `CodeMirrorResources` loads
the bundle from that website and tells your Java code when it is ready. Staging
prepares the files; loading makes them available in the current browser page.

### Automatic asset staging

For an application that produces a static website folder, the shared
`stage-assets` Maven goal performs the copying. It reads the asset description
included in the WebJar, so your build does not need to name and unpack each file
individually. It works with either the GWT or TeaVM library artifact.

Add the following plugin under `<build><plugins>` in your application POM. This
example uses `target/site` as the folder that will contain the website. Set
`outputDirectory` to the folder where your own build collects its host page and
compiled application JavaScript.

```xml
<plugin>
  <groupId>io.instanto</groupId>
  <artifactId>gwt-resources-compat-maven-plugin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <executions>
    <execution><goals><goal>stage-assets</goal></goals></execution>
  </executions>
  <configuration>
    <outputDirectory>${project.build.directory}/site</outputDirectory>
  </configuration>
</plugin>
```

When you run `mvn package`, this goal copies the editor script, version manifest
and license notices into that folder. If your build also places `index.html`
and its compiled application there, the resulting layout includes:

```text
target/site/
  index.html
  ...compiled application files...
  webjars/codemirror-teavm/0.1.0/
    codemirror-teavm.js
    manifest.json
    THIRD-PARTY-LICENSES.txt
```

Deploy the website folder with these files together. The staging goal supplies
the library assets; your application's build still supplies its HTML, application
JavaScript and page styling. Both compiler examples in this repository use this
arrangement and exercise it in their browser tests.

The WebJar records its source and destination paths in
`META-INF/teavm-assets.properties`. The shared plugin uses that description to
preserve the layout expected by the loader. Its
[documentation](https://github.com/instanto-io/teavm-compat/tree/main/gwt-resources-compat-maven-plugin)
explains how the same mechanism handles other libraries' CSS, fonts and images.
If your web server already serves WebJar resources directly, you can use that
facility instead of copying them into a static output folder.

### Load the editor when your screen needs it

Once the files are served by your website, call
`CodeMirrorResources.whenReady(ready, failed)` before mounting an `Editor` or
attaching a `CodeMirrorWidget`. The loader downloads the bundle and checks that
it provides the supported editor API. It then calls `ready`, where your code can
create and configure the editor, or `failed`, where it can explain the problem
to the user and offer a retry.

The default script URL is
`webjars/codemirror-teavm/0.1.0/codemirror-teavm.js`, relative to the host page.
That is why the staged layout above works with a page at the site's root. If
your page is in a different directory, or your server exposes assets at another
location, call `CodeMirrorResources.setScriptUrl(...)` with the URL it serves
before the first load. For example, a site served from `/my-app/` can use:

```java
CodeMirrorResources.setScriptUrl(
    "/my-app/webjars/codemirror-teavm/0.1.0/codemirror-teavm.js");
```

Several screens can request the resources at once: their requests share one
download. Callbacks run asynchronously, including when the bundle is already
available. A later call can retry a failed load. `Editor.isAvailable()` lets you
check availability without starting a download.

### Give the editor space in your layout

CodeMirror supplies its editor, theme, search, completion and diagnostic styles
when an editor is created. The binding also provides the styles that make the
editor fill its container and scroll within it. The bundled setup therefore
loads through the Java loader without a separate editor stylesheet in your page.

Your application chooses the size and position of the container. Set its height
in your page styling, or use `CodeMirrorWidget.setSize(...)` when working with
widgets. This lets the editor fit into your existing form or panel layout while
CodeMirror handles the contents of the editing area.

## Widgets and direct DOM use

For screens built from GWT widgets, use `CodeMirrorWidget` for editing and
`CodeMirrorMergeWidget` for comparison. These same widgets work with both GWT
and TeaVM through `gwt-user-compat`. Their panel owns attachment, sizing and
removal, and their `HasValue<String>` API provides the editable document and
value-change events.

`Editor` is the existing lower-level API for applications that manage DOM
containers directly. It is not a separate platform implementation or a second
screen component that an application must create. `CodeMirrorWidget` owns one
internally and exposes its advanced editing operations through `getEditor()`
while attached. The merge widget exposes its operations directly, with no
separate public merge-editor class.

Load `CodeMirrorResources` before attaching a widget or directly mounting an
editor. `EditorOptions` supplies the editing widget's initial text, language,
theme, read-only state, tab size and wrapping.

## Diff and merge

`CodeMirrorMergeWidget` compares an original baseline with a modified document.
It uses the official `@codemirror/merge` package, included in the same WebJar and
resource loader as the editor. CodeMirror injects its comparison styles; no extra
script or stylesheet references are needed. Give the widget a width and height.

```java
CodeMirrorResources.whenReady(() -> {
  CodeMirrorMergeWidget comparison = new CodeMirrorMergeWidget(
      "class Hello {}", "class Hello { String name; }");
  comparison.setLanguage(Language.JAVA);
  comparison.setSize("100%", "400px");
  comparison.setMode(CodeMirrorMergeWidget.Mode.UNIFIED);
  comparison.addValueChangeHandler(event -> saveText(event.getValue()));
  RootPanel.get("compare").add(comparison);
}, error -> showLoadError(error));
```

Import `io.instanto.codemirror.*` and `com.google.gwt.user.client.ui.RootPanel`.
The example is identical for GWT and TeaVM; use the platform dependency described
above. `saveText` and `showLoadError` are application callbacks.

| Operation | Behavior |
| --- | --- |
| `setMode(Mode.SPLIT)` | Default: original on the left, modified on the right; arrows revert individual changes into the modified document |
| `setMode(Mode.UNIFIED)` | One editable document with deleted text displayed inline and accept/reject controls |
| `getOriginal()` / `setOriginal(text)` | Read or replace the comparison baseline |
| `getValue()` / `setValue(text)` | Read or replace the modified document; setters are silent unless `setValue(text, true)` is used |
| `getChangeCount()` | Current number of changed blocks; requires attachment |
| `nextChange()` / `previousChange()` | Navigate changed blocks while attached |
| `acceptChange(index)` | In unified mode, copy a current zero-based changed block into the original baseline |
| `rejectChange(index)` | In unified mode, restore a current changed block from the original into the modified document |
| `undo()` / `redo()` | Undo or redo modified-document edits, including rejections |
| `setReadOnly(true)` | Disable user editing and merge controls; application text setters still work |
| `setCollapseUnchanged(true)` | Collapse long unchanged sections with three lines of context |
| `setLanguage(...)` / `setTheme(...)` | Configure syntax highlighting and light/dark presentation |

Accepting a change updates the baseline: it does not change the modified value or
fire a value-change event. Read `getOriginal()` if your application needs the
accepted baseline. Acceptance is not part of the modified document's undo history.
Changed-block indices are transient and must be re-evaluated after edits or merges.
Missing indices and read-only views return `false` from accept/reject. These two
methods require unified mode; split mode provides its own revert controls.

Changing mode, language, theme, read-only state or collapse settings rebuilds the
view, retaining both documents but resetting selection and undo history. Detaching
releases all native views and retains both texts, including accepted changes, for
reattachment. Call `onResize()` when a container changes size or becomes visible.

This is a two-document comparison and merge component. Git operations, three-way
conflict resolution and saving the result belong to the application.

## Shared API

Once an editor is mounted, the following operations work on both compiler targets.
The examples build from ordinary text editing to application-supplied completion
and diagnostics. They use `editor` for an existing `Editor` instance; when using
a widget, obtain that instance with `widget.getEditor()` while it is attached.

### Widget lifecycle

A widget is useful when the editor is part of a form or a screen assembled from
GWT panels. You can give it an initial value and listen for changes in the same
way as another value-bearing widget. The panel manages attachment and removal:
this example creates an editor and passes changed text to an application callback.

```java
CodeMirrorResources.whenReady(() -> {
  CodeMirrorWidget widget = new CodeMirrorWidget();
  widget.setSize("100%", "320px");
  widget.setValue("class Hello {}");
  widget.addValueChangeHandler(event -> saveText(event.getValue()));
  RootPanel.get("editor").add(widget);
}, error -> showLoadError(error));
```

Import `RootPanel` from `com.google.gwt.user.client.ui`; `saveText` and
`showLoadError` are your application callbacks. `setValue`
is silent by default; `setValue(text, true)` fires a value-change event when the
value changes. User edits also fire events. `getEditor()` provides the editor API
while attached. Detaching disposes the native editor; reattaching creates a new
editor with the retained text. History is not retained across detachments.
`onResize()` asks CodeMirror to measure its container again.

The lower-level `Editor.mount` also accepts an Elemental2 `HTMLElement`.

### Read and change the document

Use `getText()` when your application needs the current document, for example
when a user presses Save. You can also change a range of text and select it from
Java, which is useful for inserting a template or navigating to a reported problem.
Here we start with a greeting, replace its second word and select the replacement:

```java
editor.setText("Hello there");
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

### Respond to changes

An update listener lets your application react while the user works. For example,
it can mark a form as modified or arrange for validation of the new text. Check
`documentChanged()` when you only care about content, because moving the selection
also produces updates.

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
or disposal after the callback returns, using GWT `Scheduler.get().scheduleDeferred(...)` or TeaVM
`org.teavm.jso.browser.Window.setTimeout(...)`.

### Configure editing and search

Choose a language to give the editor the syntax support appropriate to the
content being edited. The bundled configurations include Java, JavaScript,
TypeScript, JSON, HTML, CSS, Python and plain text. Light and One Dark themes are
bundled. Changing language, theme, tab size or wrapping preserves the document,
selection and history.

```java
editor.setTabSize(4);
editor.setLineWrapping(true);
editor.setReadOnly(true);
```

Read-only disables user edits; application code can still update the document.

Your application can drive search from its own controls, or open CodeMirror's
built-in search/replace panel. For example, this code sets a case-sensitive literal
query and moves to its next match:

```java
if (editor.setSearch("World", true, false)) {
  editor.findNext();
}
```

The third argument enables regular expressions. Invalid or empty queries return
`false` and leave the previous query intact. `findPrevious()` searches backwards.
`openSearchPanel()` opens the search/replace UI; `closeSearchPanel()` closes it.

### Supply completion results

A Java completion provider lets you offer suggestions from your application's
own vocabulary, such as template variables or configuration keys. CodeMirror
handles showing the suggestions and inserting the chosen text. Your provider
supplies the proposals and the document range they replace.

This small example offers a single Java statement at the current word:

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

### Supply diagnostics

Diagnostics let your application show validation results beside the text they
refer to. Your own validator or service decides what is wrong and supplies a
message, severity and range; CodeMirror displays the result in the editor.

Validation can finish after the user has typed more text. Record the document
revision before starting analysis, then pass it back with the results so that
warnings for an older document are not applied to a newer one:

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

### Dispose of the editor

When your screen owns an `Editor` directly, it also owns its lifetime. Close any
listener you no longer need, and close the editor before removing its container.
A `CodeMirrorWidget` performs editor disposal when the widget is detached.

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
Java completion. Shared merge-widget checks cover split/unified comparison,
accept/reject, empty documents, Unicode, final-line changes, read-only commands,
value events and detach/reattach. The showcase also exercises the comparison
controls in both browser engines. TeaVM tests run in both standard and optimized builds.

The typed Java API does not yet cover creating multiple selections, asynchronous
completion, snippet placeholders, diagnostic fixes or custom syntax parsers. There
is also no built-in connection to a language server through LSP. JVM editing and
WebAssembly are outside the validated scope.

If your application needs a CodeMirror extension beyond the typed API, it can
supply native extension objects through `setExtensions(Object)`. That method
replaces the extensions in a dedicated part of the editor's configuration;
`nativeView()` exposes the underlying CodeMirror `EditorView` for direct access.
This requires working with CodeMirror's JavaScript API and bypasses the Java
wrapper's validation.

The bundle exports `CodeMirrorTea.state` and `CodeMirrorTea.view` so such
extensions can use the same CodeMirror module instances as the editor. Avoid
loading a second copy of those modules, and let the Java wrapper continue to
manage editor disposal and updates.

## Build

To build the library from source, use JDK 21 and Maven 3.9+, with the matching
Instanto TeaVM parent and compiler artifacts installed. Verification compiles the
Java API for both targets and exercises it in real browsers. With the project's
Playwright browsers available, run the standard build and then the optimized
TeaVM build:

```sh
mvn clean verify
mvn -Pproduction clean verify
```

`mvn install` also installs the alpha artifacts into your local Maven repository.
The TeaVM site is written to `codemirror-showcase/target/site`; the GWT browser
contract page is in `codemirror-gwt-tests/target/site`. Both compilers and both
browser engines are exercised by the build.
[The build workflow](.github/workflows/build.yml) records browser installation
and verification for both compiler targets.

The prebuilt editor bundle is committed to this repository and included in the
WebJar. If you change the browser integration or its bundled dependencies, use
Node 22+ and run `npm ci && npm run build` to regenerate it.
`npm run check:assets` checks that the committed bundle can be reproduced.

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
