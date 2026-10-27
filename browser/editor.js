import {MergeView, unifiedMergeView, getChunks, getOriginalDoc, originalDocChangeEffect, acceptChunk, rejectChunk, goToNextChunk, goToPreviousChunk} from '@codemirror/merge';
import {basicSetup} from 'codemirror';
import * as state from '@codemirror/state';
import * as view from '@codemirror/view';
import {undo, redo, undoDepth, redoDepth, isolateHistory, historyField, toggleComment, cursorLineStart, cursorLineEnd, cursorDocStart, cursorDocEnd, selectLine, selectAll, indentMore, indentLess, insertNewlineAndIndent, deleteLine} from '@codemirror/commands';
import {foldCode, unfoldCode, foldAll, unfoldAll} from '@codemirror/language';
import {SearchQuery, search as searchExtension, setSearchQuery, findNext, findPrevious, openSearchPanel, closeSearchPanel} from '@codemirror/search';
import {autocompletion, startCompletion, closeCompletion, snippet, nextSnippetField, prevSnippetField} from '@codemirror/autocomplete';
import {setDiagnostics, lintGutter, diagnosticCount, forEachDiagnostic} from '@codemirror/lint';
import {java} from '@codemirror/lang-java';
import {javascript} from '@codemirror/lang-javascript';
import {json} from '@codemirror/lang-json';
import {html} from '@codemirror/lang-html';
import {css} from '@codemirror/lang-css';
import {python} from '@codemirror/lang-python';
import {oneDark} from '@codemirror/theme-one-dark';
export {state, view};
export const apiVersion = 3;
const languages = {PLAIN: () => [], JAVA: java, JAVASCRIPT: javascript,
  TYPESCRIPT: () => javascript({typescript:true}), JSON: json, HTML: html, CSS: css, PYTHON: python};
const themes = {LIGHT: [], DARK: oneDark};
const layoutTheme = view.EditorView.theme({"&":{height:"100%"},".cm-scroller":{overflow:"auto"}});
export function create(parent, text, language, theme, readOnly, tabSize, wrap, listener) {
  const compartments = Object.fromEntries(['language','theme','readOnly','tabSize','wrap','completion','custom','keys','hover','palette'].map(k => [k,new state.Compartment()]));
  const handle = {view:null, compartments, revision:0, listener, destroyed:false, pending:new Set()};
  handle.view = new view.EditorView({parent, doc:text, extensions:[basicSetup, searchExtension(), lintGutter(),
    layoutTheme,
    compartments.language.of(languages[language]()), compartments.theme.of(themes[theme]),
    compartments.readOnly.of([state.EditorState.readOnly.of(readOnly),view.EditorView.editable.of(!readOnly)]),
    compartments.tabSize.of(state.EditorState.tabSize.of(tabSize)),
    compartments.wrap.of(wrap ? view.EditorView.lineWrapping : []),
    compartments.completion.of([]), compartments.custom.of([]),
    compartments.keys.of([]), compartments.hover.of([]), compartments.palette.of([]), highlightsField, markersField, markerGutter,
    view.EditorView.baseTheme({".cm-javaMarkers .cm-gutterElement":{padding:"0 4px"},".cm-javaHover":{padding:"6px 8px",whiteSpace:"pre-wrap"}}),
    view.EditorView.updateListener.of(update => {
      if (update.docChanged) { handle.revision++; cancelPending(handle); }
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
  cancelPending(h);
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
  cancelPending(h);
  closeCompletion(h.view);
  h.completionProvider = provider;
  configure(h, 'completion', provider ? autocompletion({override:[context => {
    const match = context.matchBefore(/[\w$]*/);
    return h.completionProvider ? h.completionProvider(context.state.doc.toString(),context.pos,context.explicit,match ? match.from : context.pos) : null;
  }]}) : []);
}
export function diagnostics(h, values) { h.view.dispatch(setDiagnostics(h.view.state,values)); }
export function command(h, name) {
  const commands = {undo,redo,findNext,findPrevious,openSearchPanel,closeSearchPanel,startCompletion,closeCompletion,
    UNDO:undo,REDO:redo,FOLD:foldCode,UNFOLD:unfoldCode,FOLD_ALL:foldAll,UNFOLD_ALL:unfoldAll,TOGGLE_COMMENT:toggleComment,
    LINE_START:cursorLineStart,LINE_END:cursorLineEnd,DOCUMENT_START:cursorDocStart,DOCUMENT_END:cursorDocEnd,
    SELECT_LINE:selectLine,SELECT_ALL:selectAll,INDENT_MORE:indentMore,INDENT_LESS:indentLess,
    NEWLINE_AND_INDENT:insertNewlineAndIndent,DELETE_LINE:deleteLine,NEXT_SNIPPET_FIELD:nextSnippetField,PREVIOUS_SNIPPET_FIELD:prevSnippetField};
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
export function completion(label,apply,type,detail,info,isSnippet) { return {label,apply:isSnippet ? snippet(apply) : apply,type,detail,info}; }
export function diagnostic(from,to,severity,message,actions) { return {from,to,severity,message,actions}; }
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
export function mergeCreate(parent, original, modified, mode, language, theme, readOnly, collapse, listener, saved) {
  const h = {split:null, editor:null, listener, readOnly, originalSelection:saved?.originalSelection, destroyed:false};
  const extensions = editable => [basicSetup, languages[language](), themes[theme], theme === "LIGHT" ? mergeLight : [], mergeLayout,
    state.EditorState.readOnly.of(!editable), view.EditorView.editable.of(editable)];
  const updates = view.EditorView.updateListener.of(u => {
    if (u.docChanged && h.listener) h.listener(u.state.doc.toString());
  });
  const collapseUnchanged = collapse ? {margin:3,minSize:4} : undefined;
  if (mode === 'SPLIT') {
    h.split = new MergeView({parent,
      a:{doc:original,selection:saved?.originalSelection ? state.EditorSelection.create(saved.originalSelection.ranges.map(r => state.EditorSelection.range(Math.min(r.anchor,original.length),Math.min(r.head,original.length))),saved.originalSelection.mainIndex) : undefined,extensions:extensions(false)},
      b:{doc:modified,selection:saved?.selection,extensions:[extensions(!readOnly),updates,saved ? historyField.init(() => saved.history) : []]},
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
    h.editor = new view.EditorView({parent,doc:modified,selection:saved?.selection,extensions:[extensions(!readOnly),layoutTheme,updates,saved ? historyField.init(() => saved.history) : [],
      unifiedMergeView({original,mergeControls:!readOnly,collapseUnchanged})]});
  }
  if (saved) {
    if (saved.focused) h.editor.focus();
    requestAnimationFrame(() => { if (!h.destroyed) (h.split ? h.split.dom : h.editor.scrollDOM).scrollTop = saved.scrollTop; });
  }
  return h;
}
export function mergeDestroy(h) {
  h.destroyed = true;
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

// Typed Java features use the same state/view instances as the core editor.
export function selectionRange(anchor, head) { return state.EditorSelection.range(anchor,head); }
export function selections(h, ranges, mainIndex) {
  h.view.dispatch({selection:state.EditorSelection.create(ranges,mainIndex),scrollIntoView:true});
}
export function selectionCount(h) { return h.view.state.selection.ranges.length; }
export function selectionAnchor(h,index) { return h.view.state.selection.ranges[index].anchor; }
export function selectionHead(h,index) { return h.view.state.selection.ranges[index].head; }
export function mainSelectionIndex(h) { return h.view.state.selection.mainIndex; }
export function binding(h,key,name,action) {
  return {key,run:() => { if (action) { action(); return true; } return command(h,name); }};
}
export function keyBindings(h,bindings) { configure(h,'keys',state.Prec.highest(view.keymap.of(bindings))); }
export function insertSnippet(h,template,from,to) { snippet(template)(h.view,{label:""},from,to); }
function cancelPending(h) { for (const cancel of [...h.pending]) cancel(); }
export function asyncCompletions(h,provider) {
  cancelPending(h);
  closeCompletion(h.view);
  h.completionProvider = null;
  configure(h,'completion',provider ? autocompletion({override:[context => new Promise((resolve,reject) => {
    const doc = h.view.state.doc;
    let settled = false, cancelled = false, cancel = null;
    const abort = () => {
      if (settled) return;
      settled = true; cancelled = true; h.pending.delete(abort);
      resolve(null);
      if (cancel) cancel();
    };
    const finish = value => {
      if (settled) return;
      settled = true; h.pending.delete(abort);
      resolve(h.destroyed || h.view.state.doc !== doc || context.aborted ? null : value);
    };
    const fail = message => {
      if (settled) return;
      settled = true; h.pending.delete(abort); reject(new Error(message));
    };
    h.pending.add(abort);
    context.addEventListener('abort',abort,{onDocChange:true});
    const match = context.matchBefore(/[\w$]*/);
    try {
      provider(doc.toString(),context.pos,context.explicit,match ? match.from : context.pos,
        {finish,fail,setCancel: action => { cancel = action; if (cancelled && cancel) cancel(); }});
    } catch (error) { fail(String(error)); }
  })]}) : []);
}
export function diagnosticFix(name,action) {
  return {name,apply:(editor,from,to) => { if (!editor.state.readOnly) action(from,to); }};
}
export function applyDiagnosticFix(h,index,actionIndex) {
  if (h.view.state.readOnly || index < 0 || actionIndex < 0) return false;
  let current = 0, action = null, from = 0, to = 0;
  forEachDiagnostic(h.view.state,(diagnostic,start,end) => {
    if (current++ === index) { action = diagnostic.actions?.[actionIndex]; from = start; to = end; }
  });
  if (!action) return false;
  action.apply(h.view,from,to); return true;
}
export function hoverInfo(from,to,text) { return {from,to,text}; }
export function hoverSource(h,source) {
  configure(h,'hover',source ? view.hoverTooltip((editor,pos) => {
    const info = source(editor.state.doc.toString(),pos);
    if (!info) return null;
    return {pos:info.from,end:info.to,above:true,create:() => {
      const dom = document.createElement('div'); dom.className = 'cm-javaHover'; dom.textContent = info.text; return {dom};
    }};
  }) : []);
}
const setHighlights = state.StateEffect.define();
const highlightsField = state.StateField.define({
  create:() => view.Decoration.none,
  update:(value,tr) => {
    value = value.map(tr.changes);
    for (const effect of tr.effects) if (effect.is(setHighlights)) value = effect.value;
    return value;
  },
  provide:field => view.EditorView.decorations.from(field)
});
export function highlight(from,to,color,background,underline) {
  const style = document.createElement('span').style;
  style.color = color; style.backgroundColor = background;
  if (underline) style.textDecoration = 'underline';
  return view.Decoration.mark({attributes:{style:style.cssText},class:'cm-javaHighlight'}).range(from,to);
}
export function highlights(h,ranges) { h.view.dispatch({effects:setHighlights.of(view.Decoration.set(ranges,true))}); }
const setMarkers = state.StateEffect.define();
const markersField = state.StateField.define({
  create:() => [],
  update:(value,tr) => {
    value = value.map(m => ({...m,position:tr.changes.mapPos(m.position,1)}));
    for (const effect of tr.effects) if (effect.is(setMarkers)) value = effect.value;
    return value;
  }
});
class JavaMarker extends view.GutterMarker {
  constructor(label,title) { super(); this.label=label; this.title=title; }
  eq(other) { return this.label === other.label && this.title === other.title; }
  toDOM() { const dom=document.createElement('span'); dom.textContent=this.label; dom.title=this.title; return dom; }
}
const markerGutter = view.gutter({class:'cm-javaMarkers',markers:editor => state.RangeSet.of(
  editor.state.field(markersField).map(m => new JavaMarker(m.label,m.title).range(editor.state.doc.lineAt(m.position).from)),true)
});
export function gutterMark(position,label,title) { return {position,label,title}; }
export function gutterMarks(h,marks) { h.view.dispatch({effects:setMarkers.of(marks)}); }
export function palette(h,foreground,background,caret,selection,gutterForeground,gutterBackground,dark) {
  configure(h,'palette',state.Prec.highest(view.EditorView.theme({
    '&':{color:foreground,backgroundColor:background},
    '.cm-content':{caretColor:caret}, '.cm-cursor, .cm-dropCursor':{borderLeftColor:caret},
    '&.cm-focused .cm-selectionBackground, .cm-selectionBackground, .cm-content ::selection':{backgroundColor:selection},
    '.cm-gutters':{color:gutterForeground,backgroundColor:gutterBackground}
  },{dark})));
}
export function clearPalette(h) { configure(h,'palette',[]); }
export function mergeCapture(h) {
  return {history:h.editor.state.field(historyField),selection:h.editor.state.selection,
    originalSelection:h.split ? h.split.a.state.selection : h.originalSelection,
    focused:h.editor.hasFocus,scrollTop:(h.split ? h.split.dom : h.editor.scrollDOM).scrollTop};
}
export function mergeAnchor(h) { return h.editor.state.selection.main.anchor; }
export function mergeHead(h) { return h.editor.state.selection.main.head; }
export function mergeSelect(h,anchor,head) { h.editor.dispatch({selection:{anchor,head},scrollIntoView:true}); }

export function completeResult(request,result) { request.finish(result); }
export function completeFailure(request,message) { request.fail(message); }
export function completionCancellation(request,action) { request.setCancel(action); }
