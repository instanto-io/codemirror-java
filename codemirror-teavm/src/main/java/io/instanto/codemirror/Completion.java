package io.instanto.codemirror;

/** Completion with plain-text documentation and optional CodeMirror snippet placeholders. */
public record Completion(
    String label,
    String insertText,
    Kind kind,
    String detail,
    String documentation,
    boolean snippet) {
  public Completion(String label, String insertText, Kind kind, String detail) {
    this(label, insertText, kind, detail, "", false);
  }

  public static Completion snippet(
      String label, String template, String detail, String documentation) {
    return new Completion(label, template, Kind.SNIPPET, detail, documentation, true);
  }

  public enum Kind {
    TEXT,
    SNIPPET,
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
    java.util.Objects.requireNonNull(documentation);
  }
}
