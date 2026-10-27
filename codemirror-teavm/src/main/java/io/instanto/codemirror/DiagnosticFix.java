package io.instanto.codemirror;

/** A named fix. The action receives the diagnostic's current, mapped UTF-16 range. */
public record DiagnosticFix(String label, Action action) {
  @FunctionalInterface
  public interface Action {
    void apply(int from, int to);
  }

  public DiagnosticFix {
    java.util.Objects.requireNonNull(label);
    java.util.Objects.requireNonNull(action);
  }
}
