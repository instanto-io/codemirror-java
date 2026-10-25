package io.instanto.codemirror;

/**
 * Context for a synchronous Java completion source; wordFrom is a suggested ASCII word boundary.
 */
public record CompletionContext(String text, int position, boolean explicit, int wordFrom) {}
