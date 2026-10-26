package io.instanto.codemirror;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.RequiresResize;

/** GWT widget ownership for the shared editor API. Load the browser bundle before attachment. */
public final class CodeMirrorWidget extends Composite implements HasValue<String>, RequiresResize {
  private final EditorOptions options;
  private Editor editor;
  private String value;
  private boolean settingValue;

  public CodeMirrorWidget() {
    this(EditorOptions.defaults());
  }

  public CodeMirrorWidget(EditorOptions options) {
    this.options = java.util.Objects.requireNonNull(options);
    value = options.text();
    initWidget(new FlowPanel());
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    editor =
        Editor.mount(
            getElement(),
            new EditorOptions(
                value,
                options.language(),
                options.theme(),
                options.readOnly(),
                options.tabSize(),
                options.lineWrapping()));
    editor.onUpdate(
        update -> {
          if (!update.documentChanged()) return;
          String previous = value;
          value = editor.getText();
          if (!settingValue) ValueChangeEvent.fireIfNotEqual(this, previous, value);
        });
  }

  @Override
  protected void onUnload() {
    if (editor != null) {
      value = editor.getText();
      editor.close();
      editor = null;
    }
    super.onUnload();
  }

  /** Available while the widget is attached. A later attachment creates a new native editor. */
  public Editor getEditor() {
    if (editor == null) throw new IllegalStateException("Widget is not attached");
    return editor;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String value) {
    setValue(value, false);
  }

  @Override
  public void setValue(String value, boolean fireEvents) {
    String previous = this.value;
    String next = value == null ? "" : value;
    if (previous.equals(next)) return;
    if (editor != null) {
      settingValue = true;
      try {
        editor.setText(next);
      } finally {
        settingValue = false;
      }
    }
    this.value = next;
    if (fireEvents) ValueChangeEvent.fireIfNotEqual(this, previous, next);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public void onResize() {
    if (editor != null) editor.requestMeasure();
  }
}
