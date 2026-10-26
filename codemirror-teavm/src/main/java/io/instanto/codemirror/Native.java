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
    return jsinterop.base.Js.global().get("CodeMirrorTea") != null && apiVersion == 1;
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

  public static native Object completion(String label, String apply, String type, String detail);

  public static native Object diagnostic(int from, int to, String severity, String message);

  public static native void diagnostics(Object h, Object values);

  public static native void focus(Object h);

  public static native boolean hasFocus(Object h);

  public static native void measure(Object h);

  @JsMethod(name = "nativeView")
  public static native Object view(Object h);
}
