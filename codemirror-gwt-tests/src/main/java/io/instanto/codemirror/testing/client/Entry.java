package io.instanto.codemirror.testing.client;

import com.google.gwt.core.client.EntryPoint;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import io.instanto.codemirror.*;
import java.util.List;

public class Entry implements EntryPoint {
  public void onModuleLoad() {
    try {
      WidgetChecks.run();
      Editor editor = Editor.mount((HTMLElement) DomGlobal.document.getElementById("editor"));
      editor.setLanguage(Language.JAVA);
      editor.setTheme(Theme.DARK);
      editor.setText("class Hello {}");
      int[] changes = {0};
      Subscription listener =
          editor.onUpdate(
              e -> {
                if (e.documentChanged()) changes[0]++;
              });
      editor.replace(6, 11, "World");
      require(editor.getText().equals("class World {}"), "replace");
      require(changes[0] == 1, "Java update callback");
      editor.setSelection(new Selection(6, 11));
      require(editor.getSelectedText().equals("World"), "selection");
      require(editor.undo() && editor.getText().equals("class Hello {}"), "undo");
      require(editor.redo(), "redo");
      require(editor.setSearch("World", true, false) && editor.findNext(), "search");
      int revision = editor.revision();
      require(
          editor.setDiagnostics(
              revision, List.of(new Diagnostic(6, 11, Diagnostic.Severity.WARNING, "Example"))),
          "diagnostic");
      require(editor.diagnosticCount() == 1, "diagnostic count");
      editor.setText("pri");
      require(!editor.setDiagnostics(revision, List.of()), "stale diagnostic");
      editor.setSelection(new Selection(3, 3));
      editor.setCompletionSource(
          c ->
              new CompletionResult(
                  c.wordFrom(),
                  c.position(),
                  List.of(
                      new Completion(
                          "println",
                          "System.out.println()",
                          Completion.Kind.FUNCTION,
                          "Java callback"))));
      editor.focus();
      editor.startCompletion();
      DomGlobal.document
          .getElementById("dispose")
          .addEventListener(
              "click",
              e -> {
                listener.close();
                editor.close();
                editor.close();
                require(
                    DomGlobal.document.getElementById("editor").childNodes.length == 0, "dispose");
                DomGlobal.document.body.setAttribute("data-disposed", "true");
              });
      DomGlobal.document.body.setAttribute("data-ready", "true");
    } catch (Throwable error) {
      DomGlobal.document.body.setAttribute("data-error", error.toString());
      throw error;
    }
  }

  private static void require(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }
}
