package io.instanto.codemirror;

import jsinterop.annotations.*;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "CodeMirrorTea")
final class Native {
  private Native() {}

  @JsFunction
  interface Update {
    void accept(boolean doc, boolean selection, int revision, int anchor, int head);
  }

  @JsFunction
  interface Complete {
    Object complete(String text, int pos, boolean explicit, int from);
  }

  public static int apiVersion;

  @JsOverlay
  static boolean available() {
    return jsinterop.base.Js.global().get("CodeMirrorTea") != null && apiVersion == 3;
  }

  public static native Object create(
      Object parent,
      String text,
      String lang,
      String theme,
      boolean readOnly,
      int tabSize,
      boolean wrap,
      Update listener);

  public static native void destroy(Object h);

  public static native String text(Object h);

  public static native int length(Object h);

  public static native int revision(Object h);

  public static native void configure(Object h, String key, Object value);

  public static native void configure(Object h, String key, String value);

  public static native void configure(Object h, String key, boolean value);

  public static native void configure(Object h, String key, int value);

  public static native Object array();

  public static native void push(Object a, Object v);

  public static native Object edit(int from, int to, String text);

  public static native void edits(Object h, Object edits);

  public static native void select(Object h, int anchor, int head);

  public static native int anchor(Object h);

  public static native int head(Object h);

  public static native boolean command(Object h, String command);

  public static native int count(Object h, String name);

  public static native boolean search(Object h, String text, boolean caseSensitive, boolean regexp);

  public static native void completions(Object h, Complete provider);

  public static native Object result(int from, int to, Object options);

  public static native Object completion(
      String label, String apply, String type, String detail, String info, boolean snippet);

  public static native Object diagnostic(
      int from, int to, String severity, String message, Object fixes);

  public static native void diagnostics(Object h, Object values);

  public static native void focus(Object h);

  public static native boolean hasFocus(Object h);

  public static native void measure(Object h);

  @JsMethod(name = "nativeView")
  public static native Object view(Object h);

  @JsFunction
  interface MergeUpdate {
    void accept(String value);
  }

  public static native Object mergeCreate(
      Object parent,
      String original,
      String modified,
      String mode,
      String language,
      String theme,
      boolean readOnly,
      boolean collapse,
      MergeUpdate listener,
      Object saved);

  public static native void mergeDestroy(Object handle);

  public static native String mergeText(Object handle, boolean original);

  public static native void mergeSetText(Object handle, String value, boolean original);

  public static native int mergeCount(Object handle);

  public static native boolean mergeResolve(Object handle, int index, boolean accept);

  public static native boolean mergeCommand(Object handle, String command);

  public static native void mergeMeasure(Object handle);

  @JsFunction
  interface Action {
    void run();
  }

  @JsFunction
  interface AsyncComplete {
    void complete(String text, int pos, boolean explicit, int from, Object request);
  }

  public static native void completeResult(Object request, Object result);

  public static native void completeFailure(Object request, String message);

  public static native void completionCancellation(Object request, Action cancellation);

  @JsFunction
  interface Fix {
    void apply(int from, int to);
  }

  @JsFunction
  interface Hover {
    Object hover(String text, int position);
  }

  public static native void asyncCompletions(Object h, AsyncComplete source);

  public static native Object selectionRange(int anchor, int head);

  public static native void selections(Object h, Object ranges, int mainIndex);

  public static native int selectionCount(Object h);

  public static native int selectionAnchor(Object h, int index);

  public static native int selectionHead(Object h, int index);

  public static native int mainSelectionIndex(Object h);

  public static native Object binding(Object h, String key, String command, Action action);

  public static native void keyBindings(Object h, Object bindings);

  public static native void insertSnippet(Object h, String template, int from, int to);

  public static native Object diagnosticFix(String label, Fix action);

  public static native boolean applyDiagnosticFix(Object h, int diagnosticIndex, int actionIndex);

  public static native void hoverSource(Object h, Hover source);

  public static native Object hoverInfo(int from, int to, String text);

  public static native Object highlight(
      int from, int to, String color, String background, boolean underline);

  public static native void highlights(Object h, Object ranges);

  public static native Object gutterMark(int position, String label, String title);

  public static native void gutterMarks(Object h, Object marks);

  public static native void palette(
      Object h,
      String foreground,
      String background,
      String caret,
      String selection,
      String gutterForeground,
      String gutterBackground,
      boolean dark);

  public static native void clearPalette(Object h);

  public static native Object mergeCapture(Object h);

  public static native int mergeAnchor(Object h);

  public static native int mergeHead(Object h);

  public static native void mergeSelect(Object h, int anchor, int head);
}
