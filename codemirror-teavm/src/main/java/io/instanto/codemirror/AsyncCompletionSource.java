package io.instanto.codemirror;

import java.util.function.Consumer;

/**
 * Start a completion request and deliver a result (or null) once. Return an optional subscription
 * that cancels the work. Cancellation runs when the request is aborted, the document changes, the
 * provider is replaced, or the widget detaches. Late results are ignored.
 */
@FunctionalInterface
public interface AsyncCompletionSource {
  Subscription complete(CompletionContext context, Consumer<CompletionResult> result);
}
