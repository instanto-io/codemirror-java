package io.instanto.codemirror;

/** Replacement range and immutable options. Return null from a source to provide no result. */
public record CompletionResult(int from, int to, java.util.List<Completion> options) {
  public CompletionResult {
    if (from < 0 || to < from) throw new IllegalArgumentException("Invalid completion range");
    options = java.util.List.copyOf(options);
  }
}
