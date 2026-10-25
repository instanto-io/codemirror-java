package io.instanto.codemirror;

/** Plain-text completion. No HTML is interpreted. */
public record Completion(String label, String insertText, Kind kind, String detail) {
  public enum Kind {
    TEXT,
    KEYWORD,
    VARIABLE,
    FUNCTION,
    CLASS,
    PROPERTY,
    CONSTANT
  }

  public Completion {
    java.util.Objects.requireNonNull(label);
    java.util.Objects.requireNonNull(insertText);
    java.util.Objects.requireNonNull(kind);
    java.util.Objects.requireNonNull(detail);
  }
}
