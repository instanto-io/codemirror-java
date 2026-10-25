package io.instanto.codemirror;

/** A plain-text diagnostic in UTF-16 offsets. */
public record Diagnostic(int from, int to, Severity severity, String message) {
  public enum Severity {
    ERROR,
    WARNING,
    INFO,
    HINT
  }

  public Diagnostic {
    if (from < 0 || to < from) throw new IllegalArgumentException("Invalid diagnostic range");
    java.util.Objects.requireNonNull(severity);
    java.util.Objects.requireNonNull(message);
  }
}
