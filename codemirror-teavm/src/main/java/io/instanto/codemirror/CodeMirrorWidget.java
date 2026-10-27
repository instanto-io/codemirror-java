package io.instanto.codemirror;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.RequiresResize;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * CodeMirror editor widget shared by GWT and TeaVM. Load CodeMirrorResources before attachment.
 * Text, configuration, completion sources, extensions and update subscriptions survive
 * reattachment. Selection, history, diagnostics, search state and revision belong to each mounted
 * view.
 */
public final class CodeMirrorWidget extends Composite
    implements HasValue<String>, HasText, RequiresResize {
  private Language language;
  private Theme theme;
  private boolean readOnly, lineWrapping;
  private int tabSize;
  private CompletionSource completionSource;
  private Object extensions;
  private final List<Consumer<EditorUpdate>> listeners = new ArrayList<>();
  private Editor editor;
  private String value;
  private boolean settingValue;

  public CodeMirrorWidget() {
    this(EditorOptions.defaults());
  }

  public CodeMirrorWidget(EditorOptions options) {
    Objects.requireNonNull(options);
    language = options.language();
    theme = options.theme();
    readOnly = options.readOnly();
    lineWrapping = options.lineWrapping();
    tabSize = options.tabSize();
    value = options.text();
    initWidget(new FlowPanel());
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    editor =
        Editor.mount(
            getElement(),
            new EditorOptions(value, language, theme, readOnly, tabSize, lineWrapping));
    value = editor.getText();
    if (completionSource != null) editor.setCompletionSource(completionSource);
    if (extensions != null) editor.setExtensions(extensions);
    editor.onUpdate(
        update -> {
          if (update.documentChanged()) {
            String previous = value;
            value = editor.getText();
            if (!settingValue) ValueChangeEvent.fireIfNotEqual(this, previous, value);
          }
          for (Consumer<EditorUpdate> listener : new ArrayList<>(listeners))
            listener.accept(update);
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

  private Editor attached() {
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
    this.value = editor == null ? next : editor.getText();
    if (fireEvents) ValueChangeEvent.fireIfNotEqual(this, previous, this.value);
  }

  @Override
  public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  @Override
  public String getText() {
    return getValue();
  }

  /**
   * Replaces the text; value-change events fire when it changes. May be called before attachment.
   */
  @Override
  public void setText(String text) {
    Objects.requireNonNull(text);
    if (editor == null) setValue(text, true);
    else editor.setText(text);
  }

  public int length() {
    return getText().length();
  }

  /** Register before or after attachment. Close the subscription to remove it permanently. */
  public Subscription onUpdate(Consumer<EditorUpdate> listener) {
    Objects.requireNonNull(listener);
    Consumer<EditorUpdate> registration = update -> listener.accept(update);
    listeners.add(registration);
    return () -> listeners.remove(registration);
  }

  public void setLanguage(Language next) {
    Objects.requireNonNull(next);
    if (editor != null) editor.setLanguage(next);
    language = next;
  }

  public void setTheme(Theme next) {
    Objects.requireNonNull(next);
    if (editor != null) editor.setTheme(next);
    theme = next;
  }

  /** Prevents user editing; programmatic changes remain possible. */
  public void setReadOnly(boolean next) {
    if (editor != null) editor.setReadOnly(next);
    readOnly = next;
  }

  public void setLineWrapping(boolean next) {
    if (editor != null) editor.setLineWrapping(next);
    lineWrapping = next;
  }

  public void setTabSize(int next) {
    if (next < 1 || next > 32) throw new IllegalArgumentException("tabSize must be 1..32");
    if (editor != null) editor.setTabSize(next);
    tabSize = next;
  }

  /** A synchronous provider retained across attachments. Null restores language defaults. */
  public void setCompletionSource(CompletionSource source) {
    if (editor != null) editor.setCompletionSource(source);
    completionSource = source;
  }

  /** Retains native extensions built from this bundle's state/view exports across attachments. */
  public void setExtensions(Object next) {
    Objects.requireNonNull(next);
    if (editor != null) editor.setExtensions(next);
    extensions = next;
  }

  /** False while detached. */
  public boolean hasFocus() {
    return editor != null && editor.hasFocus();
  }

  /** Remeasures an attached view; harmless while detached. */
  public void requestMeasure() {
    onResize();
  }

  /** Current view revision; resets on reattachment. Requires attachment. */
  public int revision() {
    return attached().revision();
  }

  /** Replace a UTF-16 range as one undoable transaction. Requires attachment. */
  public void replace(int from, int to, String text) {
    attached().replace(from, to, text);
  }

  /**
   * Apply ordered, non-overlapping original-document ranges in one undo step. Requires attachment.
   */
  public void applyEdits(List<TextEdit> edits) {
    attached().applyEdits(edits);
  }

  /** Read the main selection. Requires attachment. */
  public Selection getSelection() {
    return attached().getSelection();
  }

  /** Read the selected text. Requires attachment. */
  public String getSelectedText() {
    return attached().getSelectedText();
  }

  /** Set the main selection using UTF-16 offsets. Requires attachment. */
  public void setSelection(Selection selection) {
    attached().setSelection(selection);
  }

  /** Undo a document transaction. Requires attachment. */
  public boolean undo() {
    return attached().undo();
  }

  /** Redo a document transaction. Requires attachment. */
  public boolean redo() {
    return attached().redo();
  }

  /** Number of available undo steps. Requires attachment. */
  public int undoDepth() {
    return attached().undoDepth();
  }

  /** Number of available redo steps. Requires attachment. */
  public int redoDepth() {
    return attached().redoDepth();
  }

  /** Configure search; invalid or empty queries return false. Requires attachment. */
  public boolean setSearch(String query, boolean caseSensitive, boolean regexp) {
    return attached().setSearch(query, caseSensitive, regexp);
  }

  /** Find the next search match. Requires attachment. */
  public boolean findNext() {
    return attached().findNext();
  }

  /** Find the previous search match. Requires attachment. */
  public boolean findPrevious() {
    return attached().findPrevious();
  }

  /** Open the built-in search/replace panel. Requires attachment. */
  public boolean openSearchPanel() {
    return attached().openSearchPanel();
  }

  /** Close the search panel. Requires attachment. */
  public boolean closeSearchPanel() {
    return attached().closeSearchPanel();
  }

  /** Show completion suggestions. Requires attachment. */
  public boolean startCompletion() {
    return attached().startCompletion();
  }

  /**
   * Publish diagnostics for this view revision; stale results return false. Requires attachment.
   */
  public boolean setDiagnostics(int expectedRevision, List<Diagnostic> diagnostics) {
    return attached().setDiagnostics(expectedRevision, diagnostics);
  }

  /** Number of diagnostics in the current view. Requires attachment. */
  public int diagnosticCount() {
    return attached().diagnosticCount();
  }

  /** Remove the current diagnostics. Requires attachment. */
  public void clearDiagnostics() {
    attached().clearDiagnostics();
  }

  /**
   * Native escape hatch; bypasses Java validation. Never destroy the view directly. Requires
   * attachment.
   */
  public Object nativeView() {
    return attached().nativeView();
  }

  /** Focus the editor. Requires attachment. */
  public void focus() {
    attached().focus();
  }

  @Override
  public void onResize() {
    if (editor != null) editor.requestMeasure();
  }
}
