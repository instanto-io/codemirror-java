package io.instanto.codemirror.showcase;

import io.instanto.codemirror.*;
import java.util.List;
import org.teavm.jso.dom.html.HTMLDocument;

/** Browser-executed contracts call the real compiled Java API, never a mock. */
final class Contracts {
  private Contracts() {}

  static void run() {
    io.instanto.codemirror.testing.client.WidgetChecks.run();
    var parent = HTMLDocument.current().createElement("div");
    HTMLDocument.current().getBody().appendChild(parent);
    Editor editor =
        Editor.mount(
            jsinterop.base.Js.<elemental2.dom.HTMLElement>uncheckedCast(parent),
            new EditorOptions("a😀b\nsecond", Language.JAVA, Theme.LIGHT, false, 4, false));
    try {
      check(editor.length() == 11, "UTF-16 length");
      editor.setSelection(new Selection(3, 1));
      check(editor.getSelectedText().equals("😀"), "directional Unicode selection");
      int[] calls = {0};
      var sub =
          editor.onUpdate(
              u -> {
                if (u.documentChanged()) calls[0]++;
              });
      int revision = editor.revision();
      editor.applyEdits(List.of(new TextEdit(0, 1, "A"), new TextEdit(5, 11, "last")));
      check(editor.getText().equals("A😀b\nlast"), "atomic original-offset edits");
      check(calls[0] == 1, "one update per transaction");
      check(editor.undoDepth() == 1, "one history step");
      check(editor.undo(), "undo");
      check(editor.getText().equals("a😀b\nsecond"), "undo text");
      check(editor.redo(), "redo");
      String text = editor.getText();
      editor.setLanguage(Language.JSON);
      editor.setTheme(Theme.DARK);
      editor.setTabSize(2);
      editor.setLineWrapping(true);
      check(
          editor.getText().equals(text) && editor.undoDepth() == 1,
          "configuration preserves document and history");
      check(
          !editor.setDiagnostics(
              revision, List.of(new Diagnostic(0, 1, Diagnostic.Severity.ERROR, "stale"))),
          "stale diagnostics rejected");
      check(
          editor.setDiagnostics(
              editor.revision(),
              List.of(new Diagnostic(0, 1, Diagnostic.Severity.WARNING, "test"))),
          "fresh diagnostics accepted");
      check(editor.diagnosticCount() == 1, "diagnostic count");
      editor.clearDiagnostics();
      check(editor.diagnosticCount() == 0, "clear diagnostics");
      sub.close();
      sub.close();
      int count = calls[0];
      editor.setText("alpha beta alpha");
      check(calls[0] == count, "listener removed");
      editor.setSelection(new Selection(0, 0));
      check(editor.setSearch("alpha", true, false), "valid search");
      check(editor.findNext(), "find next");
      check(editor.getSelectedText().equals("alpha"), "search selection");
      check(editor.findNext() && editor.getSelection().from() == 11, "second search result");
      check(editor.findPrevious() && editor.getSelection().from() == 0, "previous search result");
      check(!editor.setSearch("[", false, true), "invalid regexp rejected");
      expectInvalid(() -> editor.replace(0, 99, "bad"));
      expectInvalid(
          () -> editor.applyEdits(List.of(new TextEdit(3, 5, "x"), new TextEdit(4, 6, "y"))));
      expectInvalid(() -> editor.setSelection(new Selection(0, 99)));
      expectInvalid(() -> editor.setTabSize(0));
      check(editor.getText().equals("alpha beta alpha"), "invalid changes are atomic");
      int[] guarded = {0};
      var guard =
          editor.onUpdate(
              u -> {
                try {
                  editor.setText("reentrant");
                } catch (IllegalStateException e) {
                  guarded[0]++;
                }
              });
      editor.setText("guarded");
      guard.close();
      check(guarded[0] == 1, "reentrant mutation rejected clearly");
      int[] duplicateCalls = {0};
      java.util.function.Consumer<EditorUpdate> duplicate = update -> duplicateCalls[0]++;
      var firstRegistration = editor.onUpdate(duplicate);
      var secondRegistration = editor.onUpdate(duplicate);
      firstRegistration.close();
      firstRegistration.close();
      editor.setText("registered once");
      check(duplicateCalls[0] == 1, "independent duplicate listener registrations");
      secondRegistration.close();
      editor.setText("a\\nb");
      editor.setSelection(new Selection(0, 0));
      check(editor.setSearch("\\n", true, false) && editor.findNext(), "literal backslash search");
      check(editor.getSelectedText().equals("\\n"), "literal does not interpret escapes");
      editor.setExtensions(testExtension());
      check(
          hasTestAttribute(jsinterop.base.Js.uncheckedCast(editor.nativeView())),
          "custom extension applied");
      editor.setExtensions(emptyExtensions());
      check(
          !hasTestAttribute(jsinterop.base.Js.uncheckedCast(editor.nativeView())),
          "custom extension removed");
      editor.setCompletionSource(null);
      editor.focus();
      editor.requestMeasure();
      check(editor.hasFocus(), "focus ownership");
      for (var language : Language.values()) editor.setLanguage(language);
      editor.setReadOnly(true);
      editor.setText("API write");
      check(editor.getText().equals("API write"), "read-only permits programmatic updates");
      editor.close();
      editor.close();
      check(editor.isClosed(), "idempotent close");
      try {
        editor.getText();
        throw new AssertionError("closed access accepted");
      } catch (IllegalStateException expected) {
      }
      check(parent.getChildNodes().getLength() == 0, "view DOM removed");
      for (int i = 0; i < 3; i++) {
        var fresh =
            Editor.mount(jsinterop.base.Js.<elemental2.dom.HTMLElement>uncheckedCast(parent));
        fresh.setText("fresh");
        check(fresh.undoDepth() == 1, "fresh history");
        fresh.close();
      }
    } finally {
      editor.close();
      parent.getParentNode().removeChild(parent);
    }
  }

  @org.teavm.jso.JSBody(
      script = "return CodeMirrorTea.view.EditorView.editorAttributes.of({'data-contract':'yes'});")
  private static native org.teavm.jso.JSObject testExtension();

  @org.teavm.jso.JSBody(script = "return [];")
  private static native org.teavm.jso.JSObject emptyExtensions();

  @org.teavm.jso.JSBody(
      params = "view",
      script = "return view.dom.getAttribute('data-contract') === 'yes';")
  private static native boolean hasTestAttribute(org.teavm.jso.JSObject view);

  private static void expectInvalid(Runnable operation) {
    try {
      operation.run();
      throw new AssertionError("invalid input accepted");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
