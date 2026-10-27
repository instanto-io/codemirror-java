package io.instanto.codemirror;

/** Document snapshot for a Java completion source; wordFrom is a suggested ASCII word boundary. */
public record CompletionContext(String text, int position, boolean explicit, int wordFrom) {}
