<div align="center">

<a href="https://github.com/RONNCC/mihon">
    <img src="./.github/assets/logo.png" alt="Mihon logo" title="Mihon logo" width="80"/>
</a>

# Mihon, RONNCC fork

### Unofficial fork of [Mihon](https://github.com/mihonapp/mihon): a manga and comic reader for Android

[![Latest fork release](https://img.shields.io/github/v/release/RONNCC/mihon?label=fork%20release&labelColor=27303D&color=0877d2)](https://github.com/RONNCC/mihon/releases/latest)
[![CI](https://img.shields.io/github/actions/workflow/status/RONNCC/mihon/build.yml?labelColor=27303D)](https://github.com/RONNCC/mihon/actions/workflows/build.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/RONNCC/mihon?labelColor=27303D&color=0877d2)](/LICENSE)

</div>

> [!IMPORTANT]
> **This is a fork, not official Mihon.** Please read before you install or ask for help.
>
> * **Unofficial.** This is [RONNCC/mihon](https://github.com/RONNCC/mihon), a fork of [mihonapp/mihon](https://github.com/mihonapp/mihon). The Mihon team does not make, review or support it.
> * **Where to get builds:** only the [Releases page of this repository](https://github.com/RONNCC/mihon/releases). Do not use [mihon.app/download](https://mihon.app/download), the upstream Stable or Beta builds or any other site. They give you upstream Mihon, not this fork.
> * **Links on other sites do not apply.** Upstream's website, [Discord server](https://discord.gg/mihon), FAQ, Weblate and the links in the app's *About* screen are for upstream Mihon. Do not report fork bugs there.
> * **Same app ID, different signing key.** The fork's package name is `app.mihon`, the same as upstream Mihon. The two cannot be installed side by side, and Android will not install one over the other because the keys differ. If upstream Mihon is on your phone, back it up and uninstall it first (see step 1 below).
> * **Update checker:** fork releases are built with the in-app update checker turned off, so the app should not offer upstream updates. Never install an upstream Mihon APK over the fork. It would fail to install, or you would uninstall the fork and lose its data. Update only from this repository's Releases page. Details: [Updating](#updating-the-fork).
> * **Signing key** (SHA-256 of the signing certificate), which every fork release must show: `43f55df565f730d647833070947a6b478e160a0e06052f49fcce9ad3cd0244ca`. See [Verify the download](#verify-the-download).
> * **Extensions:** the fork has its own extension store for the fixed extensions. See [step 8](#8-add-the-extension-stores-and-install-extensions).
> * **Source, license and credits:** the code is Mihon's, under the Apache License 2.0. See [Credits](#credits) and [License](#license). Upstream is [mihonapp/mihon](https://github.com/mihonapp/mihon).

## Contents

* [Install on a new Android phone](#install-on-a-new-android-phone)
* [Verify the download](#verify-the-download)
* [Updating the fork](#updating-the-fork)
* [Troubleshooting](#troubleshooting)
* [What is different from stock Mihon](#what-is-different-from-stock-mihon)
* [Features](#features), [Contributing](#contributing), [Credits](#credits), [Disclaimer](#disclaimer), [License](#license)

## Install on a new Android phone

You need Android 8.0 or newer (the app's `minSdk` is 26). Mihon is an app that reads manga from sources. It does not contain any manga itself. The app reads from *extensions*, which you add in step 8.

### 1. If upstream Mihon (or a Mihon from another site) is already installed

Skip this step on a phone that never had Mihon.

1. In the old app, open *More → Settings → Data and storage* and tap *Create backup*. Save the file outside the app, for example in your Downloads folder, or copy it to another device or cloud storage.
2. Uninstall the old app. Android deletes its library and settings when you uninstall. Files in the storage folder you picked (downloads and backups) stay on the phone.

You cannot keep both apps. They share the package name `app.mihon`, and their signing keys are different.

### 2. Download the APK from the Releases page

1. On the phone, open the [latest release](https://github.com/RONNCC/mihon/releases/latest) in a browser.
2. Under *Assets*, tap one APK file. Each release has two:
   * `mihon-arm64-v8a-<tag>.apk`: about 35 MB. For 64-bit ARM phones, which is most phones made in recent years.
   * `mihon-<tag>.apk` (the universal file): about 98 MB. It runs on every phone type. If you are not sure, take this one.
3. If the arm64 file does not install, use the universal file. The release has no other per-CPU files.

`<tag>` is the version, for example `v0.21.3-ronncc.3`. You can ignore the `.zip` and source-code links.

### 3. Allow your browser to install apps

Android blocks installs from outside the Play Store until you allow them for the app you used to download the file.

1. Open the downloaded APK (from the browser's download list, or from the *Files* or *Downloads* app).
2. Android shows a message that your phone is not allowed to install unknown apps from this source. Tap *Settings*, then turn on *Allow from this source*, and go back.
3. You can turn this setting off again after the install.

The exact wording depends on the phone brand and Android version.

### 4. Install and handle Play Protect

1. Tap *Install*.
2. Google Play Protect may say the app is unknown or ask to scan it. Tap *More details*, then *Install anyway*, or choose *Scan app* and let it finish. The fork is not on the Play Store, so this prompt is normal.
3. Tap *Open*.

If Android says *App not installed*, see [Troubleshooting](#troubleshooting).

### 5. First start: the welcome screens

The app opens a short setup. You can change everything later in *More → Settings*.

1. **Theme.** Pick a look. This is optional.
2. **Storage location.** Tap *Select a folder* and pick a folder for chapter downloads, backups and more. The folder picker is Android's own. A dedicated, new folder is recommended, for example one named `Mihon`. Android's picker has a way to create a new folder. Use a folder on the phone's own storage, not on a card you may remove. You cannot continue until you pick one. If you restore a backup later, pick the same folder to find your old downloads. You can change it in *Settings → Data and storage → Storage location*.
3. **Permissions.** Tap *Grant* on each line:
   * *Install apps permission* is needed to install extensions. It opens an Android screen. Turn on *Allow from this source* for Mihon.
   * *Notification permission* (Android 13 and newer): for library update and download notices. Tap *Allow*.
   * *Background battery usage*: lets library updates, downloads and backup restores run without Android stopping them. Tap *Allow*.
   * The fork has no crash-log or analytics switches, because those are not built in (from the code; the switches are hidden when telemetry is not included).
4. **Guides.** This page links to upstream's *getting started* guide, which also works for the fork. If you have a backup, tap the restore button here instead of step 9. Then tap *Get started*.

### 6. Let the app run in the background

Library updates and downloads run in the background. Some phones stop them even after step 5.

1. Open *More → Settings → Advanced → Background activity → Disable battery optimization* and allow it.
2. Samsung: *Settings → Battery → Background usage limits*. Take Mihon off *Sleeping apps* and *Deep sleeping apps*.
3. Xiaomi / MIUI / HyperOS: in the app's info page set battery saver to *No restrictions* and turn on *Autostart*.
4. Other brands: look up your phone on [dontkillmyapp.com](https://dontkillmyapp.com/). The app links the same site under *Settings → Advanced*.

The menu names change between phone models and versions. I did not test these on every brand.

### 7. Choose how often the library updates

*More → Settings → Library → Global update → Automatic updates*. Turn it off if you only update by hand.

### 8. Add the extension stores and install extensions

Mihon ships with no extensions and no sources. You add an *extension store* (a web address of a list of extensions) and install extensions from it.

1. Open the *Browse* tab and then the *Extensions* tab. If it is empty, tap *Extension stores*. You can also go to *More → Settings → Browse → Extension stores*.
2. Tap *Add extension store*.
3. Enter the store index URL and tap *Add*:

   ```
   https://github.com/RONNCC/extensions-source/raw/repo/index.pb
   ```

   This is the fork's own store. It holds nine fixed extensions: bbato, rinkocomics, aedex, komiwa, kumanga, comicaso, westmanga, mangaworld and mangaworldadult (the list is [`fork-extensions.txt`](https://github.com/RONNCC/extensions-source/blob/main/fork-extensions.txt) in the store's repository). They are signed with the store's own key, whose SHA-256 fingerprint is `0f09d897f1f2229fd5c450b982cb800657d31786a33566c87f5613854dace535` (the `signingKeyFingerprint` in the store's [`repo.json`](https://github.com/RONNCC/extensions-source/raw/repo/repo.json)).
4. The fork's store has only those nine extensions. For the other sources, also add a regular store, such as [Keiyoushi](https://github.com/keiyoushi/extensions-source) (address `https://github.com/keiyoushi/extensions/raw/repo/index.pb`, from its README). I did not test it with the fork. Keep it if you want those sources. For the nine fork extensions, install only the fork's copies (see the last point below).
5. Go back to *Browse → Extensions*. Tap *Install* next to an extension. Android may ask you to confirm each install.

About trust:

* An extension is code from the internet, and it can read the logins saved in the app. Mihon loads an extension only if it is signed by the key of a store you added, or if you trust it by hand.
* When you add a store, the app reads that store's signing key from its list. There is no extra prompt at that time. So only add stores whose address you trust, and compare the key with the fingerprint above for the fork's store.
* An extension signed by an unknown key shows as *Untrusted*. Tap it and read the warning *Untrusted extension*. Tap *Trust* only if you know where it came from.
* If a store changes its key later, the app stops trusting that store's extensions and shows a notice. You must remove the store and add it again.
* If you install an extension from the fork's store that you already have from the Keiyoushi store (same package name), Android refuses to update it across keys. Uninstall the Keiyoushi copy first. Do not install those nine extensions from both stores.

Store addresses: from `v0.21.3-ronncc.4` on, the app accepts extension stores at plain `http://` addresses again, as well as `https://`. (`v0.21.3-ronncc.1` to `.3` required `https://`.) Plain `http://` is not encrypted, so prefer `https://` stores. A restored backup can also bring back such a store.

### 9. Restore a backup (optional)

1. *More → Settings → Data and storage → Restore backup*, then choose your `.tachibk` file. You can pick what to restore: library, categories, app settings, extension stores and source settings. Then tap *Restore*. You can also open a `.tachibk` file from a file manager with Mihon.
2. The app lists *Missing sources* and *Trackers not logged into*. Install the missing extensions (step 8) and log in to the trackers (step 10).
3. Extension stores in the backup are added again. The app fetches each store's signing key from the store, not from the backup.
4. Backups that come from upstream Mihon should work in the fork, because the fork uses the same backup format (`.tachibk`). I did not test restoring a backup from upstream or from other Tachiyomi forks. JSON backups are refused.

### 10. Log in to trackers (optional)

*More → Settings → Tracking*, then tap the tracker. For AniList, MyAnimeList, MangaBaka, Shikimori, Bangumi, Hikka and Comick the app opens your browser. Log in there. Android returns to Mihon. You must start the login from inside the app, and finish it within 15 minutes. A login link from another app or website is ignored on purpose. Kitsu and MangaUpdates ask for a name and password in the app instead.

### 11. Turn on automatic backups

*More → Settings → Data and storage → Automatic backup frequency*. Pick a frequency. Backups go to the storage folder from step 5. Also copy a backup off the phone now and then.

### 12. If a source does not load

* *Settings → Advanced → Networking → DNS over HTTPS (DoH)*: choose a provider, then restart the app. This helps when your network blocks a site by its name or a source shows a TLS or "can't reach" error. It may not help.
* Some sources only load from some countries or networks. A VPN such as Mullvad can help or can make it worse, because some sites block VPNs. I cannot say it will work for your source.
* Open the source in *Browse*, then use its menu and choose *Open in WebView*. Pass the Cloudflare check there, then go back and refresh.

## Verify the download

Every fork release is signed with the same key. Its SHA-256 fingerprint is:

```
43f55df565f730d647833070947a6b478e160a0e06052f49fcce9ad3cd0244ca
```

Upstream Mihon v0.21.3 shows a different fingerprint (`9add655a78e96c4ec7a53ef89dccb557cb5d767489fac5e785d671a5a75d4da2`). I checked the fork's `v0.21.3-ronncc.3` APKs (both files) and the `v0.21.3-ronncc.4` arm64 APK against the first value.

* On a computer with the Android SDK build-tools: `apksigner verify --print-certs mihon-arm64-v8a-<tag>.apk`. Look for `certificate SHA-256 digest`.
* On the phone: apps such as [AppVerifier](https://github.com/soupslurpr/AppVerifier) show an installed app's fingerprint. I did not test this app.
* Do not install a file that shows another fingerprint.

## Updating the fork

* Go to the [Releases page](https://github.com/RONNCC/mihon/releases), download the new APK (same file type as before) and open it. Android installs it over the old one and keeps your data. This only works because every release uses the same signing key.
* You do not need to uninstall first.
* **Update checker:** upstream Mihon checks `mihonapp/mihon` for new versions. Fork releases are built without that checker (the release workflow builds without upstream's distribution flag), and *Settings → About* has no *Check for updates* row in such builds. I read this in the code and did not test it on a phone. The app also does not tell you when a fork update exists. Check the Releases page yourself, or watch the repository with *Watch → Custom → Releases* on GitHub.
* *What's new* in *Settings → About* opens upstream's page for the same version number. It is not the fork's release notes.
* If the app ever offers an update that links to `mihonapp/mihon` or to mihon.app, do not install it.
* Extension updates come from the stores you added, as usual.

## Troubleshooting

* **"App not installed" or "App not installed as package conflicts with an existing package":** an app with the same package name and another signing key is installed (upstream Mihon or a Mihon from another site). Back up, uninstall it, install the fork. See step 1. A phone with too little free space, or an old Android version below 8.0, gives a similar error.
* **"There was a problem parsing the package" or "Package appears to be invalid":** the download is incomplete or not an APK. Download it again from the Releases page. If you use the arm64 file, try the universal file. The assets list on the release page shows the sizes: about 35 MB and 98 MB for `v0.21.3-ronncc.4`. I did not test how each phone words this error.
* **Extensions list is empty or an extension does not show up:** open *Settings → Browse → Extension stores* and check that a store is added. Pull down on *Browse → Extensions* to refresh. Check that the phone is online. The language filter and the *Allowed content warnings* setting in *Settings → Browse* can hide extensions you did not allow. An extension you already installed from another store with the same key is not listed again as available.
* **Extension installs fail:** allow Mihon to install apps (step 5). You can change the installer in *Settings → Advanced → Extensions → Installer*.
* **Extension says *Untrusted*:** see the trust notes in step 8.
* **Source shows a certificate or HTTPS error:** check that the phone's date and time are right. Then try DNS over HTTPS (step 12). Update Android System WebView and Chrome from the Play Store.
* **Library does not update in the background, downloads stop:** see step 6.
* **Tracker login does nothing:** start it again from *Settings → Tracking* and finish within 15 minutes.

## What is different from stock Mihon

The [CHANGELOG](./CHANGELOG.md) has the full list. It is grouped here:

* **Own releases.** Builds from this repository are signed with the fork's key, are published as a universal APK and an arm64-v8a APK, and contain no Firebase crash reporting or analytics code (I found no Firebase code in the `ronncc.3` APK) and no upstream update checker.
* **Downloads.** They resume after a restart or when the network returns. They are not counted as done when a rename fails, and they are not lost when a restore queues chapters or when entries are removed.
* **Library updates.** They no longer hang on a source that keeps failing to connect. A cancelled update is not reported as failed. The last update time is recorded when the update finishes. Entries are not skipped when the clocks change.
* **Backups and restore.** Automatic backups keep the old ones until the new one is valid. A restore no longer changes internal app state such as trusted extensions, no longer replaces an extension store's signing key, and no longer crashes on an invalid date format.
* **Security.** Trackers accept an OAuth login only while one started in the app is pending (15 minutes). An extension store's signing key is not replaced by a changed index. Verbose logs hide cookies and the Kavita API key.
* **Trackers.** Progress is not lowered. Queued updates of logged-out trackers are not dropped. Hikka is not logged out by short errors. Kitsu, Kavita, Suwayomi and MangaUpdates fixes.
* **Extensions.** Extensions are no longer marked obsolete when their store is unreachable. Long installs no longer freeze the app on Android 14 and newer. Plain-`http` extension stores work again (since `v0.21.3-ronncc.4`).
* **Reader, images and UI.** The CHANGELOG's *Unreleased* part lists the reader and image changes (an experimental high quality renderer, a new decoder for AVIF, JPEG XL, HEIF and JPEG 2000) and many small fixes to search, selection, history and the chapter list. Some of these come from upstream's development branch and are not part of upstream's last stable release.

## Features

* Local reading of content.
* A configurable reader with multiple viewers, reading directions and other settings.
* Tracker support: [MangaBaka](https://mangabaka.org), [MyAnimeList](https://myanimelist.net/), [AniList](https://anilist.co/), [Kitsu](https://kitsu.app/), [MangaUpdates](https://mangaupdates.com), [Shikimori](https://shikimori.one), [Bangumi](https://bgm.tv/), and [Hikka](https://hikka.io/) support.
* Categories to organize your library.
* Light and dark themes.
* Schedule updating your library for new chapters.
* Create backups locally to read offline or to your desired cloud service.
* Plus much more...

## Contributing

[Code of conduct](./CODE_OF_CONDUCT.md) · [Contributing guide](./CONTRIBUTING.md)

* **Bugs in this fork:** tell the fork's maintainer, [RONNCC](https://github.com/RONNCC). GitHub Issues are turned off on this repository for now. Give the version from *More → Settings → About → Version*.
* **Bugs that also happen in upstream Mihon:** report them upstream, after you check that [upstream's issues](https://github.com/mihonapp/mihon/issues) do not list them yet. Upstream's [FAQ](https://mihon.app/docs/faq/general) and [Discord server](https://discord.gg/mihon) are for upstream Mihon. Do not ask them about fork builds.
* **Translations** are done upstream with [Weblate](https://hosted.weblate.org/engage/mihon/). This fork does not run its own translation project.
* Pull requests to this fork are welcome. For major changes, please open an issue first.

### Repositories (upstream)

[![mihonapp/website - GitHub](https://github-stats-extended.vercel.app/api/pin/?username=mihonapp&repo=website&bg_color=161B22&text_color=c9d1d9&title_color=0877d2&icon_color=0877d2&border_radius=8&hide_border=true&description_lines_count=2)](https://github.com/mihonapp/website/)
[![mihonapp/bitmap.kt - GitHub](https://github-stats-extended.vercel.app/api/pin/?username=mihonapp&repo=bitmap.kt&bg_color=161B22&text_color=c9d1d9&title_color=0877d2&icon_color=0877d2&border_radius=8&hide_border=true&description_lines_count=2)](https://github.com/mihonapp/bitmap.kt/)

### Credits

This fork is built on the work of the [upstream Mihon contributors](https://github.com/mihonapp/mihon/graphs/contributors) and the Tachiyomi project before it. Thank you to all the people who have contributed!

<a href="https://github.com/mihonapp/mihon/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=mihonapp/mihon" alt="Mihon app contributors" title="Mihon app contributors" width="800"/>
</a>

### Disclaimer

The developer(s) of this application does not have any affiliation with the content providers available, and this application hosts zero content.

### License

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
