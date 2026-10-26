# CodeMirror TeaVM maintenance

Keep library-specific work in this repository. Reusable compatibility fixes belong in teavm-compat and must be tested there before dependent ports are published.

- Maintain one GWT/JsInterop-compatible source API for modern GWT and TeaVM.
- Compile canonical sources with GWT; generate TeaVM sources using teavm-compat's adapter and use its GWT/Elemental2 implementations.
- Keep TeaVM-specific native types out of the shared public API. Put reusable adaptation fixes in teavm-compat.
- Target modern CodeMirror 6. Document legacy wrapper migration differences rather than silently preserving incompatible CM5 behavior.
- Use the organization TeaVM parent and WebJar resource layout.
- Regenerate pinned browser assets with `npm ci && npm run build`; never hand-edit the bundle.
- Preserve full dependency licenses, the manifest, and the npm lock file.
- Verify with JDK 21: `mvn clean verify` and `mvn -Pproduction clean verify`.
- Browser contracts must exercise compiled Java behavior, not only native JavaScript.
- Document API boundaries and version-specific escape hatches honestly.
- Publish a showcase only after its source revision passes both browser engines.
