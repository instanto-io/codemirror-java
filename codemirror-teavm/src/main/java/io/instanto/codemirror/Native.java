package io.instanto.codemirror;

import org.teavm.jso.*;
import org.teavm.jso.dom.html.HTMLElement;

final class Native {
  private Native() {}

  @JSFunctor
  interface Update extends JSObject {
    void accept(boolean doc, boolean selection, int revision, int anchor, int head);
  }

  @JSFunctor
  interface Complete extends JSObject {
    JSObject complete(String text, int pos, boolean explicit, int from);
  }

  @JSBody(script = "return typeof CodeMirrorTea !== 'undefined' && CodeMirrorTea.apiVersion === 1;")
  static native boolean available();

  @JSBody(
      params = {"parent", "text", "lang", "theme", "readOnly", "tabSize", "wrap", "listener"},
      script =
          "return CodeMirrorTea.create(parent,text,lang,theme,readOnly,tabSize,wrap,listener);")
  static native JSObject create(
      HTMLElement parent,
      String text,
      String lang,
      String theme,
      boolean readOnly,
      int tabSize,
      boolean wrap,
      Update listener);

  @JSBody(params = "h", script = "CodeMirrorTea.destroy(h);")
  static native void destroy(JSObject h);

  @JSBody(params = "h", script = "return h.view.state.doc.toString();")
  static native String text(JSObject h);

  @JSBody(params = "h", script = "return h.view.state.doc.length;")
  static native int length(JSObject h);

  @JSBody(params = "h", script = "return h.revision;")
  static native int revision(JSObject h);

  @JSBody(
      params = {"h", "key", "value"},
      script = "CodeMirrorTea.configure(h,key,value);")
  static native void configure(JSObject h, String key, JSObject value);

  @JSBody(
      params = {"h", "key", "value"},
      script = "CodeMirrorTea.configure(h,key,value);")
  static native void configure(JSObject h, String key, String value);

  @JSBody(
      params = {"h", "key", "value"},
      script = "CodeMirrorTea.configure(h,key,value);")
  static native void configure(JSObject h, String key, boolean value);

  @JSBody(
      params = {"h", "key", "value"},
      script = "CodeMirrorTea.configure(h,key,value);")
  static native void configure(JSObject h, String key, int value);

  @JSBody(script = "return [];")
  static native JSObject array();

  @JSBody(
      params = {"a", "v"},
      script = "a.push(v);")
  static native void push(JSObject a, JSObject v);

  @JSBody(
      params = {"from", "to", "text"},
      script = "return {from:from,to:to,insert:text};")
  static native JSObject edit(int from, int to, String text);

  @JSBody(
      params = {"h", "edits"},
      script = "CodeMirrorTea.edits(h,edits);")
  static native void edits(JSObject h, JSObject edits);

  @JSBody(
      params = {"h", "anchor", "head"},
      script = "h.view.dispatch({selection:{anchor:anchor,head:head},scrollIntoView:true});")
  static native void select(JSObject h, int anchor, int head);

  @JSBody(params = "h", script = "return h.view.state.selection.main.anchor;")
  static native int anchor(JSObject h);

  @JSBody(params = "h", script = "return h.view.state.selection.main.head;")
  static native int head(JSObject h);

  @JSBody(
      params = {"h", "command"},
      script = "return CodeMirrorTea.command(h,command);")
  static native boolean command(JSObject h, String command);

  @JSBody(
      params = {"h", "name"},
      script = "return CodeMirrorTea.count(h,name);")
  static native int count(JSObject h, String name);

  @JSBody(
      params = {"h", "text", "caseSensitive", "regexp"},
      script = "return CodeMirrorTea.search(h,text,caseSensitive,regexp);")
  static native boolean search(JSObject h, String text, boolean caseSensitive, boolean regexp);

  @JSBody(
      params = {"h", "provider"},
      script = "CodeMirrorTea.completions(h,provider);")
  static native void completions(JSObject h, Complete provider);

  @JSBody(
      params = {"from", "to", "options"},
      script = "return {from:from,to:to,options:options};")
  static native JSObject result(int from, int to, JSObject options);

  @JSBody(
      params = {"label", "apply", "type", "detail"},
      script = "return {label:label,apply:apply,type:type,detail:detail};")
  static native JSObject completion(String label, String apply, String type, String detail);

  @JSBody(
      params = {"from", "to", "severity", "message"},
      script = "return {from:from,to:to,severity:severity,message:message};")
  static native JSObject diagnostic(int from, int to, String severity, String message);

  @JSBody(
      params = {"h", "values"},
      script = "CodeMirrorTea.diagnostics(h,values);")
  static native void diagnostics(JSObject h, JSObject values);

  @JSBody(params = "h", script = "h.view.focus();")
  static native void focus(JSObject h);

  @JSBody(params = "h", script = "return h.view.hasFocus;")
  static native boolean hasFocus(JSObject h);

  @JSBody(params = "h", script = "h.view.requestMeasure();")
  static native void measure(JSObject h);

  @JSBody(params = "h", script = "return h.view;")
  static native JSObject view(JSObject h);
}
