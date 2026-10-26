package io.instanto.codemirror;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Loads the complete editor bundle, including languages, themes and injected styles. */
public final class CodeMirrorResources {
  private static String scriptUrl = Editor.ASSET_PATH;
  private static boolean loading;
  private static final List<Request> pending = new ArrayList<>();

  private CodeMirrorResources() {}

  /** Set a page-relative or absolute bundle URL before loading, or after a failed attempt. */
  public static void setScriptUrl(String url) {
    if (loading || Editor.isAvailable())
      throw new IllegalStateException("Editor resources are already loading or loaded");
    if (url == null || url.trim().isEmpty())
      throw new IllegalArgumentException("Empty editor script URL");
    scriptUrl = url;
  }

  /**
   * Calls one callback asynchronously. Concurrent requests share a download; a later request can
   * retry after a failure. Required CSS is injected by CodeMirror when editors are created.
   */
  public static void whenReady(Runnable ready, Consumer<Throwable> failed) {
    Request request = new Request(Objects.requireNonNull(ready), Objects.requireNonNull(failed));
    if (Editor.isAvailable()) {
      deliver(request, null);
      return;
    }
    pending.add(request);
    if (loading) return;
    loading = true;
    try {
      ScriptInjector.fromUrl(scriptUrl)
          .setWindow(ScriptInjector.TOP_WINDOW)
          .setRemoveTag(true)
          .setCallback(
              new Callback<Void, Exception>() {
                @Override
                public void onSuccess(Void ignored) {
                  finish(
                      Editor.isAvailable()
                          ? null
                          : new IllegalStateException(
                              "Script did not provide the supported CodeMirror Java API: "
                                  + scriptUrl));
                }

                @Override
                public void onFailure(Exception error) {
                  finish(
                      new IllegalStateException(
                          "Unable to load editor assets: " + scriptUrl, error));
                }
              })
          .inject();
    } catch (RuntimeException error) {
      finish(error);
    }
  }

  private static void finish(Throwable failure) {
    loading = false;
    List<Request> callbacks = new ArrayList<>(pending);
    pending.clear();
    for (Request callback : callbacks) deliver(callback, failure);
  }

  private static void deliver(Request request, Throwable failure) {
    // One scheduled callback per registration: an application callback cannot prevent others.
    Scheduler.get()
        .scheduleDeferred(
            () -> {
              if (failure == null) request.ready.run();
              else request.failed.accept(failure);
            });
  }

  private static final class Request {
    final Runnable ready;
    final Consumer<Throwable> failed;

    Request(Runnable ready, Consumer<Throwable> failed) {
      this.ready = ready;
      this.failed = failed;
    }
  }
}
