package io.instanto.codemirror;

/** Built-in commands usable directly or in custom key bindings. */
public enum EditorCommand {
  UNDO,
  REDO,
  FOLD,
  UNFOLD,
  FOLD_ALL,
  UNFOLD_ALL,
  TOGGLE_COMMENT,
  LINE_START,
  LINE_END,
  DOCUMENT_START,
  DOCUMENT_END,
  SELECT_LINE,
  SELECT_ALL,
  INDENT_MORE,
  INDENT_LESS,
  NEWLINE_AND_INDENT,
  DELETE_LINE,
  NEXT_SNIPPET_FIELD,
  PREVIOUS_SNIPPET_FIELD
}
