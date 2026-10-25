package io.instanto.codemirror;

/** A directional selection in UTF-16 offsets; end positions are exclusive. */
public record Selection(int anchor, int head) {
  public Selection {
    if (anchor < 0 || head < 0) throw new IllegalArgumentException("Negative selection offset");
  }

  public int from() {
    return Math.min(anchor, head);
  }

  public int to() {
    return Math.max(anchor, head);
  }

  public boolean isEmpty() {
    return anchor == head;
  }
}
