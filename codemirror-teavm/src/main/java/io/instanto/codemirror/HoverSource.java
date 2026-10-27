package io.instanto.codemirror;

/** Synchronous plain-text hover help. Return null when there is no help at the position. */
@FunctionalInterface
public interface HoverSource {
  HoverInfo hover(String text, int position);
}
