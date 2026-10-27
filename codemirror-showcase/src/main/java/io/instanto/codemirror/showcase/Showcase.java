package io.instanto.codemirror.showcase;

import io.instanto.codemirror.*;
import java.util.List;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.*;

/** This entire showcase, including completion and diagnostics, is driven by Java. */
public final class Showcase {
  private static final HTMLDocument DOC = HTMLDocument.current();
  private static final String SAMPLE =
      "public class Hello {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from TeaVM\");\n    }\n}\n";
  private static Editor editor;
  private static CodeMirrorMergeWidget comparison;
  private static boolean compareDark = true;
  private static final String MODIFIED =
      SAMPLE.replace("Hello from TeaVM", "Hello from CodeMirror Java")
          + "\n// Shared GWT and TeaVM widget\n";
  private static Language language = Language.JAVA;
  private static boolean dark = true, readOnly;
  private static int changes;

  private Showcase() {}

  public static void main(String[] args) {
    on("retry-load", Showcase::loadResources);
    loadResources();
  }

  private static void loadResources() {
    el("retry-load").setAttribute("hidden", "");
    status("Loading editor…");
    CodeMirrorResources.whenReady(
        Showcase::start,
        error -> {
          status("Unable to load editor assets. Check the served asset path, then retry.");
          el("retry-load").removeAttribute("hidden");
        });
    CodeMirrorResources.whenReady(
        () -> DOC.getBody().setAttribute("data-shared-load", "true"), error -> {});
  }

  private static void start() {
    mount(SAMPLE);
    comparison = new CodeMirrorMergeWidget(SAMPLE, MODIFIED);
    comparison.setLanguage(Language.JAVA);
    comparison.setTheme(Theme.DARK);
    comparison.setSize("100%", "100%");
    on("compare-mode", () -> showCompare(true));
    on("editor-mode", () -> showCompare(false));
    on("split", () -> comparisonMode(CodeMirrorMergeWidget.Mode.SPLIT));
    on("unified", () -> comparisonMode(CodeMirrorMergeWidget.Mode.UNIFIED));
    on("previous-change", () -> comparison.previousChange());
    on("next-change", () -> comparison.nextChange());
    on(
        "compare-readonly",
        () -> {
          comparison.setReadOnly(!comparison.isReadOnly());
          el("compare-readonly")
              .setAttribute("aria-pressed", String.valueOf(comparison.isReadOnly()));
        });
    on(
        "collapse",
        () -> {
          comparison.setCollapseUnchanged(!comparison.isCollapseUnchanged());
          el("collapse")
              .setAttribute("aria-pressed", String.valueOf(comparison.isCollapseUnchanged()));
        });
    on(
        "compare-theme",
        () -> {
          compareDark = !compareDark;
          comparison.setTheme(compareDark ? Theme.DARK : Theme.LIGHT);
        });
    on(
        "reset-compare",
        () -> {
          comparison.setOriginal(SAMPLE);
          comparison.setValue(MODIFIED);
        });
    on(
        "java",
        () -> {
          language = Language.JAVA;
          el("java").setAttribute("aria-pressed", "true");
          el("json").setAttribute("aria-pressed", "false");
          editor.setLanguage(language);
          editor.setText(SAMPLE);
          el("filename").setTextContent("Hello.java");
        });
    on(
        "json",
        () -> {
          language = Language.JSON;
          el("java").setAttribute("aria-pressed", "false");
          el("json").setAttribute("aria-pressed", "true");
          editor.setLanguage(language);
          editor.setText(
              "{\n  \"editor\": \"CodeMirror 6\",\n  \"language\": \"Java + TeaVM\"\n}\n");
          el("filename").setTextContent("project.json");
        });
    on(
        "theme",
        () -> {
          dark = !dark;
          editor.setTheme(dark ? Theme.DARK : Theme.LIGHT);
          DOC.getBody().setAttribute("data-theme", dark ? "dark" : "light");
        });
    on(
        "readonly",
        () -> {
          readOnly = !readOnly;
          editor.setReadOnly(readOnly);
          el("readonly").setAttribute("aria-pressed", String.valueOf(readOnly));
          status(readOnly ? "Read-only: keyboard edits are disabled" : "Editing enabled");
        });
    on("undo", () -> editor.undo());
    on("redo", () -> editor.redo());
    on("search", () -> editor.openSearchPanel());
    on("select", () -> editor.setSelection(new Selection(0, Math.min(12, editor.length()))));
    on(
        "complete",
        () -> {
          editor.focus();
          editor.startCompletion();
        });
    on(
        "diagnostics",
        () -> {
          editor.setDiagnostics(
              editor.revision(),
              List.of(
                  new Diagnostic(
                      0,
                      Math.min(6, editor.length()),
                      Diagnostic.Severity.WARNING,
                      "Example diagnostic supplied by Java")));
          status("One Java diagnostic published");
        });
    on(
        "clear",
        () -> {
          editor.clearDiagnostics();
          status("Diagnostics cleared");
        });
    on(
        "recreate",
        () -> {
          String text = editor.getText();
          editor.close();
          mount(text);
          status("Disposed the previous view and mounted a fresh editor");
        });
    on("save", () -> status("Read " + editor.length() + " UTF-16 code units from Java"));
    on(
        "fixture",
        () -> {
          editor.setReadOnly(false);
          readOnly = false;
          editor.setText("pri");
          editor.setSelection(new Selection(3, 3));
          editor.focus();
        });
    on(
        "contracts",
        () -> {
          try {
            Contracts.run();
            el("contract-result").setTextContent("passed");
          } catch (Throwable e) {
            el("contract-result").setTextContent("FAILED: " + e);
            throw e;
          }
        });
    status("Ready. Type pri, then use Complete or Ctrl+Space.");
    DOC.getBody().setAttribute("data-ready", "true");
  }

  private static void showCompare(boolean show) {
    el("editor-mode").setAttribute("aria-pressed", String.valueOf(!show));
    el("compare-mode").setAttribute("aria-pressed", String.valueOf(show));
    if (show) {
      el("editor-screen").setAttribute("hidden", "");
      el("compare-screen").removeAttribute("hidden");
      if (!comparison.isAttached())
        com.google.gwt.user.client.ui.RootPanel.get("compare").add(comparison);
      comparison.onResize();
      status(
          "Compare the original and modified documents. Switch to Unified to accept or reject changes.");
    } else {
      if (comparison.isAttached()) comparison.removeFromParent();
      el("compare-screen").setAttribute("hidden", "");
      el("editor-screen").removeAttribute("hidden");
      editor.requestMeasure();
      status("Ready. Type pri, then use Complete or Ctrl+Space.");
    }
  }

  private static void comparisonMode(CodeMirrorMergeWidget.Mode mode) {
    comparison.setMode(mode);
    boolean split = mode == CodeMirrorMergeWidget.Mode.SPLIT;
    if (split) el("comparison-original").removeAttribute("hidden");
    else el("comparison-original").setAttribute("hidden", "");
    el("comparison-modified")
        .setTextContent(split ? "Modified · Hello.java" : "Hello.java · Unified changes");
    el("split").setAttribute("aria-pressed", String.valueOf(split));
    el("unified").setAttribute("aria-pressed", String.valueOf(!split));
    el("compare-status")
        .setTextContent(
            split
                ? "Original is read-only. Use the arrows between documents to revert a change."
                : "Accept updates the original baseline; reject restores the original text.");
  }

  private static void mount(String text) {
    editor =
        Editor.mount(
            jsinterop.base.Js.<elemental2.dom.HTMLElement>uncheckedCast(el("editor")),
            new EditorOptions(text, language, dark ? Theme.DARK : Theme.LIGHT, readOnly, 4, false));
    editor.setCompletionSource(
        context -> {
          if (!context.explicit() && context.wordFrom() == context.position()) return null;
          return new CompletionResult(
              context.wordFrom(),
              context.position(),
              List.of(
                  new Completion(
                      "println",
                      "System.out.println()",
                      Completion.Kind.FUNCTION,
                      "Java completion provider"),
                  new Completion(
                      "private", "private", Completion.Kind.KEYWORD, "Visibility modifier")));
        });
    editor.onUpdate(
        update -> {
          if (update.documentChanged()) {
            changes++;
            el("changes").setTextContent(String.valueOf(changes));
          }
          el("position")
              .setTextContent(
                  "Offset " + update.selection().head() + " · revision " + update.revision());
        });
    el("position").setTextContent("Offset 0 · revision 0");
    expose(jsinterop.base.Js.uncheckedCast(editor.nativeView()));
  }

  static HTMLElement el(String id) {
    return DOC.getElementById(id);
  }

  private static void on(String id, Runnable action) {
    el(id).addEventListener("click", e -> action.run());
  }

  private static void status(String value) {
    el("status").setTextContent(value);
  }

  @JSBody(params = "view", script = "window.showcaseView=view;")
  private static native void expose(org.teavm.jso.JSObject view);
}
