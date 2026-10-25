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
  private static Language language = Language.JAVA;
  private static boolean dark = true, readOnly;
  private static int changes;

  private Showcase() {}

  public static void main(String[] args) {
    if (!Editor.isAvailable()) {
      el("status").setTextContent("Unable to load editor assets. Check the bundled script path.");
      return;
    }
    mount(SAMPLE);
    on(
        "java",
        () -> {
          language = Language.JAVA;
          editor.setLanguage(language);
          editor.setText(SAMPLE);
          el("filename").setTextContent("Hello.java");
        });
    on(
        "json",
        () -> {
          language = Language.JSON;
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
    DOC.getBody().setAttribute("data-ready", "true");
  }

  private static void mount(String text) {
    editor =
        Editor.mount(
            el("editor"),
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
    expose(editor.nativeView());
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
