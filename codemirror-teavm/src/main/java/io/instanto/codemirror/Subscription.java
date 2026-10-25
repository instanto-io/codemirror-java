package io.instanto.codemirror;

/** A listener registration. Closing twice is safe. */
@FunctionalInterface
public interface Subscription extends AutoCloseable {
  @Override
  void close();
}
