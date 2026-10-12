# Private Evidence development

This standalone add-on is limited to offline review of existing messages,
privacy-preserving summaries, accessibility, export and software quality.
Follow ../../docs/personal-fork-development.md and README.md.

- Keep source messages unchanged. Never introduce network senders, scanner,
  proxy, active probing, payload, authentication, OAST or automation hooks.
- Default exports must omit bodies, raw header values, hosts, query strings,
  fragments and URL paths. Path disclosure must remain an explicit option.
- Keep all visible strings in the message resource bundle.
- Separate summary/search/file behavior from Swing UI code.
- Test genuine privacy and data-loss risks with supplied or synthetic data only.
  Preserve existing upstream tests; do not rewrite or disable them.
- Reuse the core Gradle wrapper with `-p add-ons/private-evidence`,
  `--no-daemon --max-workers=1 --no-parallel` and the local bounded JVM settings.
- Use serial builds, check host headroom first, and leave FE8 paused.
- Keep commits local. Pushing, merging or release publication needs approval.
