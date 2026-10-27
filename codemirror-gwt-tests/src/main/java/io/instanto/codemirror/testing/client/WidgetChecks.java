package io.instanto.codemirror.testing.client;

import com.google.gwt.user.client.ui.RootPanel;
import io.instanto.codemirror.*;

/** The same GWT widget assertions are compiled and run by both backends. */
public final class WidgetChecks {
  private WidgetChecks() {}

  public static void run() {
    FeatureChecks.run();
    mergeChecks();
    CodeMirrorWidget widget = new CodeMirrorWidget();
    widget.setSize("400px", "200px");
    check(widget.getElement().getStyle().getProperty("width").equals("400px"), "widget CSS width");
    check(
        widget.getElement().getStyle().getProperty("height").equals("200px"), "widget CSS height");
    widget.setValue("before attachment");
    widget.setLanguage(Language.JAVA);
    widget.setTheme(Theme.DARK);
    widget.setTabSize(2);
    widget.setLineWrapping(true);
    int[] updates = {0};
    Subscription subscription =
        widget.onUpdate(
            u -> {
              if (u.documentChanged()) updates[0]++;
            });
    int[] events = {0};
    var registration = widget.addValueChangeHandler(e -> events[0]++);
    RootPanel.get().add(widget);
    try {
      check(widget.getText().equals("before attachment"), "initial value");
      widget.setValue("silent");
      check(events[0] == 0, "silent setValue");
      widget.setValue("notified", true);
      check(events[0] == 1, "requested value event");
      widget.replace(0, 8, "edited");
      check(events[0] == 2 && widget.getValue().equals("edited"), "native edit event");
      registration.removeHandler();
      widget.setText("retained");
      check(events[0] == 2, "handler removal");
      Object prior = widget.nativeView();
      RootPanel.get().remove(widget);
      check(widget.getElement().getChildCount() == 0, "detach releases native editor");
      RootPanel.get().add(widget);
      check(
          widget.nativeView() != prior && widget.getValue().equals("retained"),
          "reattach restores value");
      int previousUpdates = updates[0];
      widget.replace(0, widget.length(), "reattached");
      check(updates[0] == previousUpdates + 1, "one update subscription across reattach");
      subscription.close();
      subscription.close();
      widget.setText("unsubscribed");
      check(updates[0] == previousUpdates + 1, "remove retained update subscription");
      widget.setReadOnly(true);
      RootPanel.get().remove(widget);
      check(!widget.hasFocus(), "detached focus");
      widget.requestMeasure();
      widget.setLanguage(Language.JSON);
      widget.setText("{\"retained\": true}");
      RootPanel.get().add(widget);
      check(
          widget.getElement().getInnerHTML().contains("contenteditable=\"false\""),
          "read-only retained");
      check(widget.getElement().getInnerHTML().contains("cm-lineWrapping"), "wrapping retained");
      check(widget.getText().equals("{\"retained\": true}"), "detached text updates");
      widget.setReadOnly(false);
      widget.setValue("silent again");
      check(
          updates[0] == previousUpdates + 1,
          "removed listener stays removed after another attachment");
      widget.onResize();
    } finally {
      RootPanel.get().remove(widget);
    }
  }

  private static void mergeChecks() {
    CodeMirrorMergeWidget widget =
        new CodeMirrorMergeWidget("one\ntwo\nthree\n", "one\nTWO\nthree\n");
    widget.setSize("480px", "240px");
    int[] events = {0};
    var registration = widget.addValueChangeHandler(e -> events[0]++);
    RootPanel.get().add(widget);
    try {
      check(widget.getChangeCount() == 1, "split diff");
      check(widget.nextChange(), "navigate split diff");
      widget.setValue("one\nchanged\nthree\n");
      check(events[0] == 0, "merge silent setValue");
      widget.setValue("one\nnew\nthree\n", true);
      check(events[0] == 1, "merge value event");
      widget.setMode(CodeMirrorMergeWidget.Mode.UNIFIED);
      check(widget.getChangeCount() == 1, "unified diff");
      check(widget.acceptChange(0), "accept change");
      check(widget.getOriginal().equals(widget.getValue()), "accept updates original");
      check(events[0] == 1 && widget.getChangeCount() == 0, "accept is not a value change");
      check(!widget.acceptChange(0) && !widget.rejectChange(-1), "missing block");
      // Cover insertions, deletions, empty documents, UTF-16 and the final line without a newline.
      String[][] pairs = {
        {"", "insert"},
        {"delete", ""},
        {"a\n", "a\nb\n"},
        {"a\nb", "a"},
        {"a😀z", "a😀new"},
        {"a\nb\n", "a\n"}
      };
      for (String[] pair : pairs) {
        widget.setOriginal(pair[0]);
        widget.setValue(pair[1]);
        check(widget.getChangeCount() > 0, "edge-case diff");
        check(widget.rejectChange(0), "reject edge case");
        check(widget.getValue().equals(pair[0]), "reject preserves exact original");
        widget.setValue(pair[1]);
        check(widget.acceptChange(0), "accept edge case");
        check(widget.getOriginal().equals(pair[1]), "accept preserves exact modified");
      }
      String context = "";
      for (int i = 0; i < 20; i++) context += "unchanged line " + i + "\n";
      widget.setOriginal(context + "old\n" + context);
      widget.setValue(context + "new\n" + context);
      widget.setCollapseUnchanged(true);
      check(
          widget.getElement().getInnerHTML().contains("cm-collapsedLines"),
          "collapse unchanged lines");
      widget.setCollapseUnchanged(false);
      check(
          !widget.getElement().getInnerHTML().contains("cm-collapsedLines"),
          "expand unchanged lines");
      widget.setOriginal("old");
      widget.setValue("new");
      widget.setReadOnly(true);
      check(!widget.rejectChange(0) && !widget.acceptChange(0), "read-only merge commands");
      check(!widget.undo(), "read-only undo");
      widget.setReadOnly(false);
      widget.setTheme(Theme.DARK);
      widget.setLanguage(Language.JAVA);
      widget.setCollapseUnchanged(true);
      check(
          widget.getOriginal().equals("old") && widget.getValue().equals("new"),
          "configuration retains documents");
      check(widget.rejectChange(0), "reject after reconfigure");
      check(widget.undo() && widget.getValue().equals("new"), "undo reject");
      check(widget.redo() && widget.getValue().equals("old"), "redo reject");
      widget.setValue("retained");
      widget.acceptChange(0);
      registration.removeHandler();
      int previousEvents = events[0];
      widget.setValue("modified", true);
      check(events[0] == previousEvents, "merge handler removal");
      RootPanel.get().remove(widget);
      check(widget.getElement().getChildCount() == 0, "merge detach removes views");
      RootPanel.get().add(widget);
      check(
          widget.getOriginal().equals("retained") && widget.getValue().equals("modified"),
          "merge reattach retains accepted baseline");
      widget.setMode(CodeMirrorMergeWidget.Mode.SPLIT);
      check(widget.getChangeCount() == 1, "return to split");
      widget.onResize();
    } finally {
      RootPanel.get().remove(widget);
    }
  }

  private static void check(boolean ok, String what) {
    if (!ok) throw new AssertionError("GWT widget: " + what);
  }
}
