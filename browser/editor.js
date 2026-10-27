import {MergeView, unifiedMergeView, getChunks, getOriginalDoc, originalDocChangeEffect, acceptChunk, rejectChunk, goToNextChunk, goToPreviousChunk} from '@codemirror/merge';
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
export const apiVersion = 2;
const languages = {PLAIN: () => [], JAVA: java, JAVASCRIPT: javascript,
  TYPESCRIPT: () => javascript({typescript:true}), JSON: json, HTML: html, CSS: css, PYTHON: python};
const themes = {LIGHT: [], DARK: oneDark};
const layoutTheme = view.EditorView.theme({"&":{height:"100%"},".cm-scroller":{overflow:"auto"}});
export function create(parent, text, language, theme, readOnly, tabSize, wrap, listener) {
  const compartments = Object.fromEntries(['language','theme','readOnly','tabSize','wrap','completion','custom'].map(k => [k,new state.Compartment()]));
  const handle = {view:null, compartments, revision:0, listener, destroyed:false};
  handle.view = new view.EditorView({parent, doc:text, extensions:[basicSetup, searchExtension(), lintGutter(),
    layoutTheme,
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

// A widget owns this private handle; there is no second public merge component.
const mergeLight = view.EditorView.theme({"&":{backgroundColor:"#fff",color:"#202937"}});
const mergeLayout = view.EditorView.baseTheme({
  "&": {minHeight:"100%"},
  "&.cm-editor": {height:"auto"},
  ".cm-scroller": {overflowX:"auto"}
});
export function mergeCreate(parent, original, modified, mode, language, theme, readOnly, collapse, listener) {
  const h = {split:null, editor:null, listener, readOnly};
  const extensions = editable => [basicSetup, languages[language](), themes[theme], theme === "LIGHT" ? mergeLight : [], mergeLayout,
    state.EditorState.readOnly.of(!editable), view.EditorView.editable.of(editable)];
  const updates = view.EditorView.updateListener.of(u => {
    if (u.docChanged && h.listener) h.listener(u.state.doc.toString());
  });
  const collapseUnchanged = collapse ? {margin:3,minSize:4} : undefined;
  if (mode === 'SPLIT') {
    h.split = new MergeView({parent,
      a:{doc:original,extensions:extensions(false)},
      b:{doc:modified,extensions:[extensions(!readOnly),updates]},
      revertControls:readOnly ? undefined : 'a-to-b', collapseUnchanged,
      renderRevertControl:() => {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = '→';
        button.title = 'Restore original text';
        button.setAttribute('aria-label', button.title);
        button.style.color = 'inherit';
        // MergeView handles pointer mousedown; forward keyboard button activation too.
        button.addEventListener('click', event => {
          if (event.detail === 0) button.dispatchEvent(new MouseEvent('mousedown', {bubbles:true,cancelable:true}));
        });
        return button;
      }});
    h.editor = h.split.b;
    // One scroll container keeps the two aligned documents together.
    h.split.dom.style.height = '100%';
    h.split.dom.style.overflow = 'auto';
    h.split.dom.style.backgroundColor = theme === 'LIGHT' ? '#fff' : '#282c34';
    h.split.dom.style.color = theme === 'LIGHT' ? '#202937' : '#abb2bf';
  } else {
    h.editor = new view.EditorView({parent,doc:modified,extensions:[extensions(!readOnly),layoutTheme,updates,
      unifiedMergeView({original,mergeControls:!readOnly,collapseUnchanged})]});
  }
  return h;
}
export function mergeDestroy(h) {
  h.listener = null;
  if (h.split) h.split.destroy(); else h.editor.destroy();
}
export function mergeText(h, original) {
  return original ? (h.split ? h.split.a.state.doc : getOriginalDoc(h.editor.state)).toString()
    : h.editor.state.doc.toString();
}
export function mergeSetText(h, text, original) {
  const editor = original && h.split ? h.split.a : h.editor;
  if (original && !h.split) {
    const doc = getOriginalDoc(editor.state);
    const changes = state.ChangeSet.of({from:0,to:doc.length,insert:text},doc.length);
    editor.dispatch({effects:originalDocChangeEffect(editor.state,changes)});
  } else {
    editor.dispatch({changes:{from:0,to:editor.state.doc.length,insert:text},annotations:isolateHistory.of('full')});
  }
}
export function mergeCount(h) { return getChunks(h.editor.state).chunks.length; }
export function mergeResolve(h, index, accept) {
  const chunks = getChunks(h.editor.state).chunks;
  if (h.readOnly || h.split || index < 0 || index >= chunks.length) return false;
  return (accept ? acceptChunk : rejectChunk)(h.editor,chunks[index].fromB);
}
export function mergeCommand(h, name) {
  if (name === 'next') return goToNextChunk(h.editor);
  if (name === 'previous') return goToPreviousChunk(h.editor);
  if (h.readOnly) return false;
  return (name === 'undo' ? undo : redo)(h.editor);
}
export function mergeMeasure(h) {
  h.editor.requestMeasure();
  if (h.split) h.split.a.requestMeasure();
}
