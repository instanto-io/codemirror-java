package io.instanto.codemirror;

/** A plain-text gutter marker on the line containing the given UTF-16 position. */
public record GutterMark(int position, String label, String title) {
  public GutterMark {
    if (position < 0) throw new IllegalArgumentException("Negative marker position");
    java.util.Objects.requireNonNull(label);
    java.util.Objects.requireNonNull(title);
  }
}
