package io.instanto.codemirror;

/** CodeMirror key name (for example Mod-s), bound to a command or Java action. */
public record KeyBinding(String key, EditorCommand command, Runnable action) {
  public KeyBinding(String key, EditorCommand command) {
    this(key, command, null);
  }

  public KeyBinding(String key, Runnable action) {
    this(key, null, action);
  }

  public KeyBinding {
    if (key == null || key.trim().isEmpty())
      throw new IllegalArgumentException("Empty key binding");
    if ((command == null) == (action == null))
      throw new IllegalArgumentException("Specify exactly one command or action");
  }
}
