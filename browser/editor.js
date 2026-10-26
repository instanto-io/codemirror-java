import {basicSetup} from 'codemirror';
import * as state from '@codemirror/state';
import * as view from '@codemirror/view';
import {undo, redo, undoDepth, redoDepth, isolateHistory} from '@codemirror/commands';
import {SearchQuery, search as searchExtension, setSearchQuery, findNext, findPrevious, openSearchPanel, closeSearchPanel} from '@codemirror/search';
import {autocompletion, startCompletion, closeCompletion} from '@codemirror/autocomplete';
import {setDiagnostics, lintGutter, diagnosticCount} from '@codemirror/lint';
import {java} from '@codemirror/lang-java';
import {javascript} from '@codemirror/lang-javascript';
import {json} from '@codemirror/lang-json';
import {html} from '@codemirror/lang-html';
import {css} from '@codemirror/lang-css';
import {python} from '@codemirror/lang-python';
import {oneDark} from '@codemirror/theme-one-dark';
export {state, view};
export const apiVersion = 1;
const languages = {PLAIN: () => [], JAVA: java, JAVASCRIPT: javascript,
  TYPESCRIPT: () => javascript({typescript:true}), JSON: json, HTML: html, CSS: css, PYTHON: python};
const themes = {LIGHT: [], DARK: oneDark};
export function create(parent, text, language, theme, readOnly, tabSize, wrap, listener) {
  const compartments = Object.fromEntries(['language','theme','readOnly','tabSize','wrap','completion','custom'].map(k => [k,new state.Compartment()]));
  const handle = {view:null, compartments, revision:0, listener, destroyed:false};
  handle.view = new view.EditorView({parent, doc:text, extensions:[basicSetup, searchExtension(), lintGutter(),
    compartments.language.of(languages[language]()), compartments.theme.of(themes[theme]),
    compartments.readOnly.of([state.EditorState.readOnly.of(readOnly),view.EditorView.editable.of(!readOnly)]),
    compartments.tabSize.of(state.EditorState.tabSize.of(tabSize)),
    compartments.wrap.of(wrap ? view.EditorView.lineWrapping : []),
    compartments.completion.of([]), compartments.custom.of([]),
    view.EditorView.updateListener.of(update => {
      if (update.docChanged) handle.revision++;
      const selectionChanged = !update.startState.selection.eq(update.state.selection);
      if (handle.listener && (update.docChanged || selectionChanged)) {
        const s = update.state.selection.main;
        handle.listener(update.docChanged, selectionChanged, handle.revision, s.anchor, s.head);
      }
    })]});
  return handle;
}
export function destroy(h) {
  if (h.destroyed) return;
  h.listener = null;
  h.completionProvider = null;
  h.view.destroy();
  h.destroyed = true;
}
export function configure(h, key, value) {
  const ext = key === 'language' ? languages[value]() : key === 'theme' ? themes[value] :
    key === 'readOnly' ? [state.EditorState.readOnly.of(value),view.EditorView.editable.of(!value)] :
    key === 'tabSize' ? state.EditorState.tabSize.of(value) :
    key === 'wrap' ? (value ? view.EditorView.lineWrapping : []) : value;
  h.view.dispatch({effects:h.compartments[key].reconfigure(ext)});
}
export function edits(h, edits) {
  h.view.dispatch({changes:edits, annotations:isolateHistory.of('full')});
}
export function search(h, query, caseSensitive, regexp) {
  const q = new SearchQuery({search:query,caseSensitive,regexp,literal:!regexp});
  if (!q.valid) return false;
  h.view.dispatch({effects:setSearchQuery.of(q)});
  return true;
}
export function completions(h, provider) {
  closeCompletion(h.view);
  h.completionProvider = provider;
  configure(h, 'completion', provider ? autocompletion({override:[context => {
    const match = context.matchBefore(/[\w$]*/);
    return h.completionProvider ? h.completionProvider(context.state.doc.toString(),context.pos,context.explicit,match ? match.from : context.pos) : null;
  }]}) : []);
}
export function diagnostics(h, values) { h.view.dispatch(setDiagnostics(h.view.state,values)); }
export function command(h, name) {
  const commands = {undo,redo,findNext,findPrevious,openSearchPanel,closeSearchPanel,startCompletion,closeCompletion};
  return commands[name](h.view);
}
export function count(h, name) { return ({undo:undoDepth,redo:redoDepth,diagnostics:diagnosticCount})[name](h.view.state); }

// Shared native boundary for the GWT and TeaVM Java builds.
export function available() { return true; }
export function text(h) { return h.view.state.doc.toString(); }
export function length(h) { return h.view.state.doc.length; }
export function revision(h) { return h.revision; }
export function array() { return []; }
export function push(a,v) { a.push(v); }
export function edit(from,to,text) { return {from,to,insert:text}; }
export function select(h,anchor,head) { h.view.dispatch({selection:{anchor,head},scrollIntoView:true}); }
export function anchor(h) { return h.view.state.selection.main.anchor; }
export function head(h) { return h.view.state.selection.main.head; }
export function result(from,to,options) { return {from,to,options}; }
export function completion(label,apply,type,detail) { return {label,apply,type,detail}; }
export function diagnostic(from,to,severity,message) { return {from,to,severity,message}; }
export function focus(h) { h.view.focus(); }
export function hasFocus(h) { return h.view.hasFocus; }
export function measure(h) { h.view.requestMeasure(); }
export function nativeView(h) { return h.view; }
