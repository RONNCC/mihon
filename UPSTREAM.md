# Keeping up with upstream

This fork ([RONNCC/mihon](https://github.com/RONNCC/mihon)) keeps upstream's app name, icon and package (`app.mihon`). Only the version carries the fork tag: `<upstream version>-ronncc.<N>`, for example `0.21.3-ronncc.5`. A weekly workflow ([upstream_check.yml](.github/workflows/upstream_check.yml)) opens or updates one issue when [mihonapp/mihon](https://github.com/mihonapp/mihon) has commits that are not in `main`. It merges nothing.

## Merge upstream

Use a merge, never a rebase, so later merges stay clean. Do it in a worktree, not in your main clone.

```sh
git remote add upstream https://github.com/mihonapp/mihon.git   # once
git fetch upstream --tags
git worktree add -b upstream-sync ../wt/upstream-sync main
cd ../wt/upstream-sync
git merge upstream/main -m "Merge upstream/main"   # or: git merge v0.21.4 -m "Merge upstream v0.21.4"
```

Resolve conflicts, then verify (one run, on a machine with enough RAM or a Railway sandbox):

```sh
./gradlew spotlessCheck lintDebug testDebugUnitTest verifySqlDelightMigration :app:assembleDebug --continue
```

Fast-forward `main` to the result (`git push origin upstream-sync:main` after `git fetch origin`, no force) and delete the branch and worktree.

## Conflict hot spots

* `CHANGELOG.md`: keep both sides. Upstream's new entries go under the right heading. The fork's entries stay under *Unreleased*.
* `app/build.gradle.kts`, `versionName` and `versionCode`: when upstream releases, set `versionCode` to upstream's and `versionName` to `<upstream version>-ronncc.1`. Do not raise `versionCode` ahead of upstream: its migrations are keyed on it, and the fork would skip them.
* `README.md`: the fork rewrote it. Keep the fork's text. Take upstream's only for facts that changed.
* `.github/workflows/release.yml`: the fork's version differs a lot (signing key from secrets, tag check, universal and arm64 APKs). Keep the fork's.
* `.github/ISSUE_TEMPLATE/*`, `CONTRIBUTING.md`, `AboutScreen.kt`, `AppUpdateChecker.kt`, `GetApplicationRelease.kt`: retargeted to the fork. Keep the fork's repository and link choices.
* Most diverged source files (by `git diff --stat upstream/main...main`, tests left out): `LibraryUpdateWorker.kt`, `ImageUtil.kt`, `DownloadWorker.kt`, `DownloadManager.kt`, `TrackLoginActivity.kt`, `Downloader.kt`, `ReaderActivity.kt`, `TrackInfoDialog.kt`, `ImageDecoder.kt`, `ReaderViewModel.kt`. When upstream changes one of these, read both sides before you pick one.

## Release a fork version

1. Set `versionName` in `app/build.gradle.kts` to `<upstream version>-ronncc.<N>` (raise `N`, or restart at `1` after merging a new upstream release). Add the *Unreleased* changelog entries.
2. Merge to `main` and wait for the *Build & Test* run to pass.
3. Tag `main` with an annotated tag `v<versionName>` and push it: `git tag -a v0.21.3-ronncc.5 -m "..." && git push origin v0.21.3-ronncc.5`. The *Release* workflow fails at once if the tag is not `v` plus `versionName`. It then builds both APKs with the fork's key and creates the GitHub release.
4. Check the APKs: `apksigner verify --print-certs` must show the signing certificate SHA-256 listed in the README, and `aapt dump badging` must show `package: name='app.mihon'` and your `versionName`.

The in-app update checker follows [the fork's releases](https://github.com/RONNCC/mihon/releases). It compares the upstream version first, then the `ronncc` number, as numbers (`ronncc.10` is newer than `ronncc.9`). A plain upstream tag is never newer than a fork build. See `GetApplicationReleaseTest`.
