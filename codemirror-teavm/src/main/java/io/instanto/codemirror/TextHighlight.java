package io.instanto.codemirror;

/** A highlighted UTF-16 range. Empty colors inherit the editor's colors. */
public record TextHighlight(
    int from, int to, String color, String backgroundColor, boolean underline) {
  public TextHighlight {
    if (from < 0 || to <= from)
      throw new IllegalArgumentException("Empty or invalid highlight range");
    java.util.Objects.requireNonNull(color);
    java.util.Objects.requireNonNull(backgroundColor);
  }
}
