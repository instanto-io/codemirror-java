package io.instanto.codemirror;

/** UTF-16 range and plain text for a hover tooltip. HTML is never interpreted. */
public record HoverInfo(int from, int to, String text) {
  public HoverInfo {
    if (from < 0 || to < from) throw new IllegalArgumentException("Invalid hover range");
    java.util.Objects.requireNonNull(text);
  }
}
