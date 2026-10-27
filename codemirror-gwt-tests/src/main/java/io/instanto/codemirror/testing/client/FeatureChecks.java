package io.instanto.codemirror.testing.client;

import com.google.gwt.user.client.ui.*;
import io.instanto.codemirror.*;
import java.util.ArrayList;
import java.util.List;

/** Identical feature contracts and interactive fixtures for both compilers. */
public final class FeatureChecks {
  private FeatureChecks() {}

  public static void run() {
    CodeMirrorWidget w = new CodeMirrorWidget();
    w.setSize("600px", "240px");
    w.setLanguage(Language.JAVA);
    w.setValue("one two\nthree");
    RootPanel.get().add(w);
    try {
      w.setSelections(List.of(new Selection(0, 3), new Selection(7, 4)), 1);
      check(w.getSelections().size() == 2 && w.getMainSelectionIndex() == 1, "multiple selections");
      check(
          w.getSelectedText().equals("two") && w.getSelection().anchor() == 7,
          "primary directional selection");
      try {
        w.setSelections(List.of(new Selection(0, 100)), 0);
        throw new AssertionError("invalid ranges accepted");
      } catch (IllegalArgumentException expected) {
      }
      check(w.getSelections().size() == 2, "invalid selection is atomic");
      w.execute(EditorCommand.DOCUMENT_START);
      check(w.getSelection().head() == 0, "document start");
      w.execute(EditorCommand.DOCUMENT_END);
      check(w.getSelection().head() == w.length(), "document end");
      w.setText("class Hello {\n  void run() {\n    call();\n  }\n}\n");
      check(w.execute(EditorCommand.FOLD_ALL), "fold all");
      check(w.getElement().getInnerHTML().contains("cm-foldPlaceholder"), "fold placeholder");
      check(w.execute(EditorCommand.UNFOLD_ALL), "unfold all");
      w.setSelection(new Selection(0, 0));
      check(
          w.execute(EditorCommand.TOGGLE_COMMENT) && w.getText().startsWith("//"),
          "comment command");
      w.undo();
      w.insertSnippet("call(${1:name}, ${2:value})${0}", 0, w.length());
      check(w.getSelectedText().equals("name"), "first snippet field");
      check(
          w.execute(EditorCommand.NEXT_SNIPPET_FIELD) && w.getSelectedText().equals("value"),
          "next snippet field");
      check(
          w.execute(EditorCommand.PREVIOUS_SNIPPET_FIELD) && w.getSelectedText().equals("name"),
          "previous snippet field");
      w.setText("bad");
      int[] fixed = {0};
      w.setDiagnostics(
          w.revision(),
          List.of(
              new Diagnostic(
                  0,
                  3,
                  Diagnostic.Severity.ERROR,
                  "Bad word",
                  List.of(
                      new DiagnosticFix(
                          "Fix",
                          (from, to) -> {
                            fixed[0]++;
                            w.replace(from, to, "good");
                          })))));
      w.replace(0, 0, "x ");
      w.setReadOnly(true);
      check(!w.applyDiagnosticFix(0, 0) && fixed[0] == 0, "read-only fix blocked");
      w.setReadOnly(false);
      check(
          w.applyDiagnosticFix(0, 0) && w.getText().equals("x good"),
          "fix uses mapped diagnostic range");
      check(!w.applyDiagnosticFix(99, 0) && !w.applyDiagnosticFix(0, 99), "invalid fix indices");
      w.setText("hello\nworld");
      int revision = w.revision();
      check(
          w.setHighlights(revision, List.of(new TextHighlight(0, 5, "#123456", "#ffff00", true))),
          "publish highlights");
      check(
          w.setGutterMarks(revision, List.of(new GutterMark(6, "MARK", "Plain <b>title</b>"))),
          "publish markers");
      check(w.getElement().getInnerHTML().contains("cm-javaHighlight"), "highlight rendered");
      check(w.getElement().getInnerHTML().contains("MARK"), "gutter mark rendered");
      w.replace(0, 0, "prefix\n");
      check(
          !w.setHighlights(revision, List.of()) && !w.setGutterMarks(revision, List.of()),
          "stale annotations rejected");
      check(
          w.getElement().getInnerHTML().contains("cm-javaHighlight")
              && w.getElement().getInnerHTML().contains("MARK"),
          "annotations map through edits");
      w.clearHighlights();
      w.clearGutterMarks();
      check(!w.getElement().getInnerHTML().contains("cm-javaHighlight"), "clear highlights");
    } finally {
      w.removeFromParent();
    }
    CodeMirrorMergeWidget merge = new CodeMirrorMergeWidget("old", "new");
    merge.setSize("600px", "240px");
    RootPanel.get().add(merge);
    try {
      merge.setValue("changed");
      merge.setSelection(new Selection(6, 2));
      merge.setTheme(Theme.DARK);
      merge.setLanguage(Language.JAVA);
      merge.setCollapseUnchanged(true);
      merge.setReadOnly(true);
      merge.setReadOnly(false);
      merge.setMode(CodeMirrorMergeWidget.Mode.UNIFIED);
      check(
          merge.getSelection().equals(new Selection(6, 2)),
          "merge selection survives settings and layout");
      check(
          merge.undo() && merge.getValue().equals("new"),
          "merge undo survives settings and layout");
      merge.setMode(CodeMirrorMergeWidget.Mode.SPLIT);
      check(merge.redo() && merge.getValue().equals("changed"), "merge redo survives layout");
      merge.setMode(CodeMirrorMergeWidget.Mode.UNIFIED);
      merge.acceptChange(0);
      merge.setTheme(Theme.LIGHT);
      check(merge.getOriginal().equals("changed"), "accepted baseline survives configuration");
    } finally {
      merge.removeFromParent();
    }
  }

  public static void installLauncher() {
    Button launch = new Button("Feature contracts");
    launch.getElement().setId("feature-launcher");
    launch.setVisible(false);
    launch.addClickHandler(
        event -> {
          launch.removeFromParent();
          fixture();
        });
    RootPanel.get().add(launch);
  }

  private static void fixture() {
    FlowPanel panel = new FlowPanel();
    panel.getElement().setId("feature-fixture");
    CodeMirrorWidget w = new CodeMirrorWidget();
    w.getElement().setId("feature-editor");
    w.setSize("600px", "240px");
    w.setLanguage(Language.JAVA);
    w.setValue("pri");
    int[] requests = {0}, cancelled = {0}, finished = {0}, keys = {0};
    List<Runnable> responses = new ArrayList<>();
    w.setAsyncCompletionSource(
        (context, result) -> {
          panel.getElement().setAttribute("data-requests", String.valueOf(++requests[0]));
          responses.add(
              () -> {
                // Explicit delivery also tests an uncancellable service's late response.
                result.accept(
                    new CompletionResult(
                        context.wordFrom(),
                        context.position(),
                        List.of(
                            Completion.snippet(
                                "println",
                                "System.out.println(${1:value});${0}",
                                "Async Java source",
                                "Print a value <b>literally</b>"))));
                panel.getElement().setAttribute("data-finished", String.valueOf(++finished[0]));
              });
          return () ->
              panel.getElement().setAttribute("data-cancelled", String.valueOf(++cancelled[0]));
        });
    w.setKeyBindings(
        List.of(
            new KeyBinding("F8", EditorCommand.SELECT_ALL),
            new KeyBinding(
                "F9",
                () -> panel.getElement().setAttribute("data-keys", String.valueOf(++keys[0])))));
    w.onUpdate(update -> panel.getElement().setAttribute("data-selection", w.getSelectedText()));
    panel.add(w);
    RootPanel.get().add(panel);
    button(
        panel,
        "feature-start",
        () -> {
          w.setText("pri");
          w.setSelection(new Selection(3, 3));
          w.focus();
          w.startCompletion();
        });
    button(
        panel,
        "feature-deliver",
        () -> {
          List<Runnable> ready = new ArrayList<>(responses);
          responses.clear();
          for (Runnable response : ready) response.run();
        });
    button(panel, "feature-edit", () -> w.setText("changed"));
    button(panel, "feature-replace-source", () -> w.setCompletionSource(c -> null));
    button(panel, "feature-detach", w::removeFromParent);
    button(panel, "feature-attach", () -> panel.insert(w, 0));
    button(
        panel,
        "feature-diagnostic",
        () -> {
          w.setText("bad");
          w.setDiagnostics(
              w.revision(),
              List.of(
                  new Diagnostic(
                      0,
                      3,
                      Diagnostic.Severity.ERROR,
                      "Replace bad word",
                      List.of(
                          new DiagnosticFix(
                              "Use good",
                              (from, to) -> {
                                w.replace(from, to, "good");
                                w.clearDiagnostics();
                              })))));
        });
    button(
        panel,
        "feature-presentation",
        () -> {
          w.setText("hello\nworld");
          w.setHoverSource(
              (text, pos) -> new HoverInfo(0, text.length(), "Helpful <b>plain text</b>"));
          w.setHighlights(
              w.revision(), List.of(new TextHighlight(0, 5, "#123456", "#ffff00", true)));
          w.setGutterMarks(w.revision(), List.of(new GutterMark(6, "●", "Breakpoint")));
          w.setCustomTheme(
              new EditorTheme(
                  "#123456", "#f0f4ff", "#123456", "#ddeeff", "#123456", "#eeeeff", false));
        });
  }

  private static void button(FlowPanel panel, String id, Runnable action) {
    Button button = new Button(id);
    button.getElement().setId(id);
    button.addClickHandler(event -> action.run());
    panel.add(button);
  }

  private static void check(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
  }
}
