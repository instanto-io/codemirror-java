package io.instanto.codemirror.testing.client;

import com.google.gwt.user.client.ui.RootPanel;
import io.instanto.codemirror.*;

/** The same GWT widget assertions are compiled and run by both backends. */
public final class WidgetChecks {
  private WidgetChecks() {}

  public static void run() {
    CodeMirrorWidget widget = new CodeMirrorWidget();
    widget.setSize("400px", "200px");
    check(widget.getElement().getStyle().getProperty("width").equals("400px"), "widget CSS width");
    check(
        widget.getElement().getStyle().getProperty("height").equals("200px"), "widget CSS height");
    widget.setValue("before attachment");
    int[] events = {0};
    var registration = widget.addValueChangeHandler(e -> events[0]++);
    RootPanel.get().add(widget);
    try {
      check(widget.getEditor().getText().equals("before attachment"), "initial value");
      widget.setValue("silent");
      check(events[0] == 0, "silent setValue");
      widget.setValue("notified", true);
      check(events[0] == 1, "requested value event");
      widget.getEditor().replace(0, 8, "edited");
      check(events[0] == 2 && widget.getValue().equals("edited"), "native edit event");
      registration.removeHandler();
      widget.getEditor().setText("retained");
      check(events[0] == 2, "handler removal");
      Editor prior = widget.getEditor();
      RootPanel.get().remove(widget);
      check(prior.isClosed(), "detach releases native editor");
      RootPanel.get().add(widget);
      check(
          widget.getEditor() != prior && widget.getValue().equals("retained"),
          "reattach restores value");
      widget.onResize();
    } finally {
      RootPanel.get().remove(widget);
    }
  }

  private static void check(boolean ok, String what) {
    if (!ok) throw new AssertionError("GWT widget: " + what);
  }
}
