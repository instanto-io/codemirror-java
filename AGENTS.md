# CodeMirror TeaVM maintenance

Work only in this repository. Sibling projects are references, not modification targets.

- Target modern CodeMirror 6; do not copy CM5 modes, option maps, or event contracts.
- Use the organization TeaVM parent and WebJar resource layout.
- Regenerate pinned browser assets with `npm ci && npm run build`; never hand-edit the bundle.
- Preserve full dependency licenses, the manifest, and the npm lock file.
- Verify with JDK 21: `mvn clean verify` and `mvn -Pproduction clean verify`.
- Browser contracts must exercise compiled Java behavior, not only native JavaScript.
- Document API boundaries and version-specific escape hatches honestly.
- Publish a showcase only after its source revision passes both browser engines.
