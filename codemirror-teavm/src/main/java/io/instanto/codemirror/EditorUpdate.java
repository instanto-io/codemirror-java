package io.instanto.codemirror;

/** Immutable notification. Do not dispatch or close the editor inside its update listener. */
public record EditorUpdate(
    boolean documentChanged, boolean selectionChanged, int revision, Selection selection) {}
