package io.instanto.codemirror;

/**
 * Palette for editor chrome. CSS color values are assigned as individual properties, never HTML.
 */
public record EditorTheme(
    String foreground,
    String background,
    String caret,
    String selection,
    String gutterForeground,
    String gutterBackground,
    boolean dark) {
  public EditorTheme {
    java.util.Objects.requireNonNull(foreground);
    java.util.Objects.requireNonNull(background);
    java.util.Objects.requireNonNull(caret);
    java.util.Objects.requireNonNull(selection);
    java.util.Objects.requireNonNull(gutterForeground);
    java.util.Objects.requireNonNull(gutterBackground);
  }
}
