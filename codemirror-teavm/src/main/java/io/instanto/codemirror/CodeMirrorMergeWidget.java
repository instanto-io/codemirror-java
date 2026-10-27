package io.instanto.codemirror;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.RequiresResize;
import java.util.Objects;

/**
 * A two-document comparison and merge widget for both GWT and TeaVM. Load CodeMirrorResources
 * before attachment. The original is never keyboard-editable. Value-change events describe the
 * modified document, not changes to the baseline.
 */
public final class CodeMirrorMergeWidget extends Composite
    implements HasValue<String>, RequiresResize {
  public enum Mode {
    SPLIT,
    UNIFIED
  }

  private Object handle;
  private String original = "", value = "";
  private Mode mode = Mode.SPLIT;
  private Language language = Language.PLAIN;
  private Theme theme = Theme.LIGHT;
  private boolean readOnly, collapseUnchanged, settingValue;

  public CodeMirrorMergeWidget() {
    initWidget(new FlowPanel());
  }

  public CodeMirrorMergeWidget(String original, String modified) {
    this();
    setOriginal(original);
    setValue(modified);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    mount(null);
  }

  private void mount(Object saved) {
    if (!Native.available())
      throw new IllegalStateException(
          "Load CodeMirrorResources.whenReady before attaching the widget");
    handle =
        Native.mergeCreate(
            getElement(),
            original,
            value,
            mode.name(),
            language.name(),
            theme.name(),
            readOnly,
            collapseUnchanged,
            next -> {
              String previous = value;
              value = next;
              if (!settingValue) ValueChangeEvent.fireIfNotEqual(this, previous, next);
            },
            saved);
  }

  private void release() {
    if (handle == null) return;
    original = Native.mergeText(handle, true);
    value = Native.mergeText(handle, false);
    Native.mergeDestroy(handle);
    handle = null;
  }

  @Override
  protected void onUnload() {
    release();
    super.onUnload();
  }

  private void rebuild() {
    if (handle == null) return;
    Object saved = Native.mergeCapture(handle);
    release();
    mount(saved);
  }

  public String getOriginal() {
    return handle == null ? original : Native.mergeText(handle, true);
  }

  /** Replaces the comparison baseline. Null means an empty document. */
  public void setOriginal(String text) {
    original = text == null ? "" : text;
    if (handle != null) Native.mergeSetText(handle, original, true);
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String text) {
    setValue(text, false);
  }

  @Override
  public void setValue(String text, boolean fireEvents) {
    String next = text == null ? "" : text;
    String previous = value;
    if (previous.equals(next)) return;
    if (handle != null) {
      settingValue = true;
      try {
        Native.mergeSetText(handle, next, false);
      } finally {
        settingValue = false;
      }
    }
    value = next;
    if (fireEvents) ValueChangeEvent.fireIfNotEqual(this, previous, next);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public Mode getMode() {
    return mode;
  }

  /** Changing presentation preserves both documents, selection and undo history. */
  public void setMode(Mode next) {
    Objects.requireNonNull(next);
    if (mode != next) {
      mode = next;
      rebuild();
    }
  }

  /** Reconfigures presentation while preserving documents, selection and history. */
  public void setLanguage(Language next) {
    Objects.requireNonNull(next);
    if (language != next) {
      language = next;
      rebuild();
    }
  }

  /** Reconfigures presentation while preserving documents, selection and history. */
  public void setTheme(Theme next) {
    Objects.requireNonNull(next);
    if (theme != next) {
      theme = next;
      rebuild();
    }
  }

  /** Disables user edits and merge controls; programmatic text setters remain available. */
  public void setReadOnly(boolean next) {
    if (readOnly != next) {
      readOnly = next;
      rebuild();
    }
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  /** Collapses long unchanged regions, with three lines of context around changes. */
  public void setCollapseUnchanged(boolean next) {
    if (collapseUnchanged != next) {
      collapseUnchanged = next;
      rebuild();
    }
  }

  public boolean isCollapseUnchanged() {
    return collapseUnchanged;
  }

  private Object attached() {
    if (handle == null) throw new IllegalStateException("Widget is not attached");
    return handle;
  }

  /** Number of changed blocks, recomputed after every edit. Requires attachment. */
  public int getChangeCount() {
    return Native.mergeCount(attached());
  }

  public boolean nextChange() {
    return Native.mergeCommand(attached(), "next");
  }

  public boolean previousChange() {
    return Native.mergeCommand(attached(), "previous");
  }

  public boolean undo() {
    return Native.mergeCommand(attached(), "undo");
  }

  public boolean redo() {
    return Native.mergeCommand(attached(), "redo");
  }

  /**
   * Accepts a current zero-based changed block into the original baseline in unified mode. Does not
   * change the modified value or fire a value-change event. Returns false for a missing block or a
   * read-only view.
   */
  public boolean acceptChange(int index) {
    return resolve(index, true);
  }

  /** Reverts a current changed block to its original text in unified mode. */
  public boolean rejectChange(int index) {
    return resolve(index, false);
  }

  private boolean resolve(int index, boolean accept) {
    Object current = attached();
    if (mode != Mode.UNIFIED)
      throw new IllegalStateException(
          "Use unified mode for accept/reject; split mode has revert controls");
    return Native.mergeResolve(current, index, accept);
  }

  @Override
  public void onResize() {
    if (handle != null) Native.mergeMeasure(handle);
  }

  /** Main selection in the modified document. Requires attachment. */
  public Selection getSelection() {
    Object h = attached();
    return new Selection(Native.mergeAnchor(h), Native.mergeHead(h));
  }

  public void setSelection(Selection selection) {
    Objects.requireNonNull(selection);
    Object h = attached();
    if (selection.to() > getValue().length())
      throw new IllegalArgumentException("Selection exceeds modified document");
    Native.mergeSelect(h, selection.anchor(), selection.head());
  }
}
