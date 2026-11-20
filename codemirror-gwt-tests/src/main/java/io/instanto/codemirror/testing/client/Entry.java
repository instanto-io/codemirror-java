package io.instanto.codemirror.testing.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import elemental2.dom.DomGlobal;
import io.instanto.codemirror.*;
import java.util.List;

public class Entry implements EntryPoint {
  private static final String EDITOR_ELEMENT_ID = "editor";
  private static final String WORLD_TEXT = "World";

  public void onModuleLoad() {
    CodeMirrorResources.setScriptUrl(CodeMirrorResources.ASSET_PATH + "?gwt-contract");
    CodeMirrorResources.whenReady(
        this::start, error -> DomGlobal.document.body.setAttribute("data-error", error.toString()));
    CodeMirrorResources.whenReady(
        () -> DomGlobal.document.body.setAttribute("data-shared-load", "true"), error -> {});
  }

  private void start() {
    try {
      WidgetChecks.run();
      FeatureChecks.installLauncher();
      CodeMirrorWidget editor = new CodeMirrorWidget();
      editor.setSize("100%", "100%");
      editor.setLanguage(Language.JAVA);
      editor.setTheme(Theme.DARK);
      editor.setText("class Hello {}");
      RootPanel.get(EDITOR_ELEMENT_ID).add(editor);
      int[] changes = {0};
      Subscription listener =
          editor.onUpdate(
              e -> {
                if (e.documentChanged()) changes[0]++;
              });
      editor.replace(6, 11, WORLD_TEXT);
      require(editor.getText().equals("class World {}"), "replace");
      require(changes[0] == 1, "Java update callback");
      editor.setSelection(new Selection(6, 11));
      require(editor.getSelectedText().equals(WORLD_TEXT), "selection");
      require(editor.undo() && editor.getText().equals("class Hello {}"), "undo");
      require(editor.redo(), "redo");
      require(editor.setSearch(WORLD_TEXT, true, false) && editor.findNext(), "search");
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
      editor.removeFromParent();
      RootPanel.get(EDITOR_ELEMENT_ID).add(editor);
      editor.setSelection(new Selection(3, 3));
      editor.focus();
      editor.startCompletion();
      DomGlobal.document
          .getElementById("dispose")
          .addEventListener(
              "click",
              e -> {
                listener.close();
                editor.removeFromParent();
                editor.removeFromParent();
                require(
                    DomGlobal.document.getElementById(EDITOR_ELEMENT_ID).childNodes.length == 0,
                    "dispose");
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
