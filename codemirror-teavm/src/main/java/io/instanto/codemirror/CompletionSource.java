package io.instanto.codemirror;

@FunctionalInterface
public interface CompletionSource {
  CompletionResult complete(CompletionContext context);
}
