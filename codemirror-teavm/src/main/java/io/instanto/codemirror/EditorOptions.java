package io.instanto.codemirror;

/** Immutable initial configuration. Runtime changes use Editor methods and preserve history. */
public record EditorOptions(
    String text,
    Language language,
    Theme theme,
    boolean readOnly,
    int tabSize,
    boolean lineWrapping) {
  public EditorOptions {
    java.util.Objects.requireNonNull(text);
    java.util.Objects.requireNonNull(language);
    java.util.Objects.requireNonNull(theme);
    if (tabSize < 1 || tabSize > 32) throw new IllegalArgumentException("tabSize must be 1..32");
  }

  public static EditorOptions defaults() {
    return new EditorOptions("", Language.PLAIN, Theme.LIGHT, false, 4, false);
  }
}
