package io.instanto.codemirror;

import java.util.*;
import java.util.function.Consumer;

/** Internal native-view owner; applications use CodeMirrorWidget. */
final class Editor implements AutoCloseable {
  private Object handle;
  private final List<Consumer<EditorUpdate>> listeners = new ArrayList<>();
  private boolean notifying;

  private Editor(Object parent, EditorOptions options) {
    Objects.requireNonNull(parent);
    Objects.requireNonNull(options);
    if (!isAvailable())
      throw new IllegalStateException(
          "Call CodeMirrorResources.whenReady before mounting an editor");
    handle =
        Native.create(
            parent,
            options.text(),
            options.language().name(),
            options.theme().name(),
            options.readOnly(),
            options.tabSize(),
            options.lineWrapping(),
            this::updated);
  }

  public static boolean isAvailable() {
    return Native.available();
  }

  /** Mount in a GWT DOM element, using the same native editor lifecycle. */
  public static Editor mount(com.google.gwt.dom.client.Element parent, EditorOptions options) {
    return new Editor(parent, options);
  }

  public static Editor mount(com.google.gwt.dom.client.Element parent) {
    return mount(parent, EditorOptions.defaults());
  }

  private Object live() {
    if (handle == null) throw new IllegalStateException("Editor is closed");
    return handle;
  }

  private Object mutable() {
    Object h = live();
    if (notifying)
      throw new IllegalStateException(
          "Schedule editor mutations after the update listener returns");
    return h;
  }

  private void updated(boolean doc, boolean selection, int revision, int anchor, int head) {
    notifying = true;
    try {
      var update = new EditorUpdate(doc, selection, revision, new Selection(anchor, head));
      for (var l : new ArrayList<>(listeners)) l.accept(update);
    } finally {
      notifying = false;
    }
  }

  /** Listen to document or selection changes; closing the subscription unregisters it. */
  public Subscription onUpdate(Consumer<EditorUpdate> listener) {
    live();
    Objects.requireNonNull(listener);
    Consumer<EditorUpdate> registration = update -> listener.accept(update);
    listeners.add(registration);
    return () -> listeners.remove(registration);
  }

  public String getText() {
    return Native.text(live());
  }

  public int length() {
    return Native.length(live());
  }

  public int revision() {
    return Native.revision(live());
  }

  /** Replace the document as one undoable transaction; selection is mapped by CodeMirror. */
  public void setText(String text) {
    replace(0, length(), text);
  }

  public void replace(int from, int to, String text) {
    applyEdits(List.of(new TextEdit(from, to, text)));
  }

  /** Ordered, non-overlapping ranges all refer to the original document; one undo step. */
  public void applyEdits(List<TextEdit> edits) {
    Object h = mutable();
    Objects.requireNonNull(edits);
    int length = length(), end = -1, previousFrom = -1;
    Object values = Native.array();
    for (var edit : edits) {
      Objects.requireNonNull(edit);
      if (edit.to() > length || edit.from() < end || edit.from() == previousFrom)
        throw new IllegalArgumentException("Edits must be ordered, non-overlapping and in bounds");
      end = edit.to();
      previousFrom = edit.from();
      Native.push(values, Native.edit(edit.from(), edit.to(), edit.text()));
    }
    if (!edits.isEmpty()) Native.edits(h, values);
  }

  public Selection getSelection() {
    Object h = live();
    return new Selection(Native.anchor(h), Native.head(h));
  }

  public String getSelectedText() {
    var s = getSelection();
    return getText().substring(s.from(), s.to());
  }

  public void setSelection(Selection selection) {
    Objects.requireNonNull(selection);
    Object h = mutable();
    if (selection.to() > length()) throw new IllegalArgumentException("Selection exceeds document");
    Native.select(h, selection.anchor(), selection.head());
  }

  public void setLanguage(Language language) {
    Native.configure(mutable(), "language", Objects.requireNonNull(language).name());
  }

  public void setTheme(Theme theme) {
    Native.configure(mutable(), "theme", Objects.requireNonNull(theme).name());
  }

  /** Prevents user editing; programmatic edits remain possible. */
  public void setReadOnly(boolean readOnly) {
    Native.configure(mutable(), "readOnly", readOnly);
  }

  public void setLineWrapping(boolean enabled) {
    Native.configure(mutable(), "wrap", enabled);
  }

  public void setTabSize(int size) {
    if (size < 1 || size > 32) throw new IllegalArgumentException("tabSize must be 1..32");
    Native.configure(mutable(), "tabSize", size);
  }

  public boolean undo() {
    return Native.command(mutable(), "undo");
  }

  public boolean redo() {
    return Native.command(mutable(), "redo");
  }

  public int undoDepth() {
    return Native.count(live(), "undo");
  }

  public int redoDepth() {
    return Native.count(live(), "redo");
  }

  /** Invalid/empty queries return false and leave the previous query intact. */
  public boolean setSearch(String query, boolean caseSensitive, boolean regexp) {
    return Native.search(mutable(), Objects.requireNonNull(query), caseSensitive, regexp);
  }

  public boolean findNext() {
    return Native.command(mutable(), "findNext");
  }

  public boolean findPrevious() {
    return Native.command(mutable(), "findPrevious");
  }

  public boolean openSearchPanel() {
    return Native.command(mutable(), "openSearchPanel");
  }

  public boolean closeSearchPanel() {
    return Native.command(mutable(), "closeSearchPanel");
  }

  public void setCompletionSource(CompletionSource source) {
    Native.completions(
        mutable(),
        source == null
            ? null
            : (text, pos, explicit, from) ->
                completionResult(
                    source.complete(new CompletionContext(text, pos, explicit, from)), text, pos));
  }

  private Object completionResult(CompletionResult result, String text, int pos) {
    if (result == null) return null;
    if (result.to() > text.length() || result.from() > pos || result.to() < pos)
      throw new IllegalArgumentException("Completion range must contain cursor and fit document");
    Object values = Native.array();
    for (Completion option : result.options())
      Native.push(
          values,
          Native.completion(
              option.label(),
              option.insertText(),
              option.kind().name().toLowerCase(java.util.Locale.ROOT),
              option.detail(),
              option.documentation(),
              option.snippet()));
    return Native.result(result.from(), result.to(), values);
  }

  public void setAsyncCompletionSource(AsyncCompletionSource source) {
    Native.asyncCompletions(
        mutable(),
        source == null
            ? null
            : (text, pos, explicit, from, request) -> {
              Subscription cancellation =
                  source.complete(
                      new CompletionContext(text, pos, explicit, from),
                      result -> {
                        try {
                          Native.completeResult(request, completionResult(result, text, pos));
                        } catch (RuntimeException error) {
                          Native.completeFailure(request, error.toString());
                        }
                      });
              Native.completionCancellation(
                  request, cancellation == null ? null : () -> cancellation.close());
            });
  }

  public boolean startCompletion() {
    return Native.command(mutable(), "startCompletion");
  }

  /** Publish diagnostics only for the document revision they were computed from. */
  public boolean setDiagnostics(int expectedRevision, List<Diagnostic> diagnostics) {
    Object h = mutable();
    Objects.requireNonNull(diagnostics);
    if (expectedRevision != revision()) return false;
    Object values = Native.array();
    int length = length();
    for (var diagnostic : diagnostics) {
      Objects.requireNonNull(diagnostic);
      if (diagnostic.to() > length)
        throw new IllegalArgumentException("Diagnostic exceeds document");
      Object fixes = Native.array();
      for (DiagnosticFix fix : diagnostic.fixes())
        Native.push(
            fixes, Native.diagnosticFix(fix.label(), (from, to) -> fix.action().apply(from, to)));
      Native.push(
          values,
          Native.diagnostic(
              diagnostic.from(),
              diagnostic.to(),
              diagnostic.severity().name().toLowerCase(java.util.Locale.ROOT),
              diagnostic.message(),
              fixes));
    }
    Native.diagnostics(h, values);
    return true;
  }

  public int diagnosticCount() {
    return Native.count(live(), "diagnostics");
  }

  public void clearDiagnostics() {
    setDiagnostics(revision(), List.of());
  }

  /**
   * Replace the custom extension compartment; use extensions built with this bundle's state/view
   * exports.
   */
  public void setExtensions(Object extensions) {
    Native.configure(mutable(), "custom", Objects.requireNonNull(extensions));
  }

  /**
   * Low-level escape hatch. Direct transactions bypass Java validation; do not destroy the view
   * directly.
   */
  public Object nativeView() {
    return Native.view(live());
  }

  public void focus() {
    Native.focus(live());
  }

  public boolean hasFocus() {
    return Native.hasFocus(live());
  }

  public void requestMeasure() {
    Native.measure(live());
  }

  public boolean isClosed() {
    return handle == null;
  }

  /** Terminal and idempotent. Mount a new Editor to recreate a disposed view. */
  @Override
  public void close() {
    if (handle == null) return;
    Object h = mutable();
    Native.destroy(h);
    handle = null;
    listeners.clear();
  }

  public List<Selection> getSelections() {
    Object h = live();
    List<Selection> ranges = new ArrayList<>();
    for (int i = 0; i < Native.selectionCount(h); i++)
      ranges.add(new Selection(Native.selectionAnchor(h, i), Native.selectionHead(h, i)));
    return List.copyOf(ranges);
  }

  public int getMainSelectionIndex() {
    return Native.mainSelectionIndex(live());
  }

  public void setSelections(List<Selection> ranges, int mainIndex) {
    Object h = mutable();
    Objects.requireNonNull(ranges);
    if (ranges.isEmpty() || mainIndex < 0 || mainIndex >= ranges.size())
      throw new IllegalArgumentException("Invalid main selection index");
    Object values = Native.array();
    for (Selection range : ranges) {
      Objects.requireNonNull(range);
      if (range.to() > length()) throw new IllegalArgumentException("Selection exceeds document");
      Native.push(values, Native.selectionRange(range.anchor(), range.head()));
    }
    Native.selections(h, values, mainIndex);
  }

  public boolean execute(EditorCommand command) {
    return Native.command(mutable(), Objects.requireNonNull(command).name());
  }

  public void setKeyBindings(List<KeyBinding> bindings) {
    Object h = mutable();
    Object values = Native.array();
    for (KeyBinding binding : List.copyOf(bindings))
      Native.push(
          values,
          Native.binding(
              h,
              binding.key(),
              binding.command() == null ? null : binding.command().name(),
              binding.action() == null ? null : () -> binding.action().run()));
    Native.keyBindings(h, values);
  }

  public void insertSnippet(String template, int from, int to) {
    Object h = mutable();
    Objects.requireNonNull(template);
    if (from < 0 || to < from || to > length())
      throw new IllegalArgumentException("Invalid snippet range");
    Native.insertSnippet(h, template, from, to);
  }

  public boolean applyDiagnosticFix(int diagnosticIndex, int actionIndex) {
    return Native.applyDiagnosticFix(mutable(), diagnosticIndex, actionIndex);
  }

  public void setHoverSource(HoverSource source) {
    Native.hoverSource(
        mutable(),
        source == null
            ? null
            : (text, pos) -> {
              HoverInfo info = source.hover(text, pos);
              if (info == null) return null;
              if (info.to() > text.length() || info.from() > pos || info.to() < pos)
                throw new IllegalArgumentException(
                    "Hover range must contain position and fit document");
              return Native.hoverInfo(info.from(), info.to(), info.text());
            });
  }

  public boolean setHighlights(int expectedRevision, List<TextHighlight> ranges) {
    Object h = mutable();
    if (expectedRevision != revision()) return false;
    Object values = Native.array();
    for (TextHighlight range : List.copyOf(ranges)) {
      if (range.to() > length()) throw new IllegalArgumentException("Highlight exceeds document");
      Native.push(
          values,
          Native.highlight(
              range.from(), range.to(), range.color(), range.backgroundColor(), range.underline()));
    }
    Native.highlights(h, values);
    return true;
  }

  public boolean setGutterMarks(int expectedRevision, List<GutterMark> marks) {
    Object h = mutable();
    if (expectedRevision != revision()) return false;
    Object values = Native.array();
    for (GutterMark mark : List.copyOf(marks)) {
      if (mark.position() > length()) throw new IllegalArgumentException("Marker exceeds document");
      Native.push(values, Native.gutterMark(mark.position(), mark.label(), mark.title()));
    }
    Native.gutterMarks(h, values);
    return true;
  }

  public void setCustomTheme(EditorTheme theme) {
    Object h = mutable();
    if (theme == null) Native.clearPalette(h);
    else
      Native.palette(
          h,
          theme.foreground(),
          theme.background(),
          theme.caret(),
          theme.selection(),
          theme.gutterForeground(),
          theme.gutterBackground(),
          theme.dark());
  }
}
