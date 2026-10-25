package io.instanto.codemirror;

/** An edit against the document BEFORE the transaction. */
public record TextEdit(int from, int to, String text) {
  public TextEdit {
    if (from < 0 || to < from) throw new IllegalArgumentException("Invalid edit range");
    java.util.Objects.requireNonNull(text);
  }
}
