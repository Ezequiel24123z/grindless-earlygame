# Public override source

Only files explicitly selected by [`../public-overrides.json`](../public-overrides.json)
can enter the opt-in playable profile. This source directory is not copied recursively;
this guide is not an override.

The current list is deliberately empty. The selected upstream mods use their defaults.
No official Superflat preset, quest replacement, personal options or generated launcher
configuration is supplied here. The official preset belongs to versioned mod resources,
not a bundled save.

A future reviewed entry has exactly `source` and `destination` string fields. `source`
is relative to this directory; `destination` is relative to the installed instance and
must name a file below `config/` or `defaultconfigs/`. Commit the actual source file.
Use portable slash-separated path segments without absolute paths, dot/traversal
segments, backslashes, drive prefixes or Windows device names. Missing/untracked files,
escaping symlinks and case-insensitive duplicate or file/directory-conflicting
destinations are rejected. Symlink targets must also be committed under this root.

Additional content roots require an explicit reviewed whitelist change, not an arbitrary
launcher-directory export. The builder records each selected source, public destination
and SHA-256, and distinguishes those overrides from upstream-generated defaults.
