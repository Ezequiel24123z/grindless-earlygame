# Bounded Quern calibration (ADR-0145)

Development-only Windows PowerShell 5.1/.NET tooling; no Gradle, JVM, generator,
client, runtime change or new package ecosystem. The SH twins use the same script
through `pwsh`, following `tools/check-links.sh`. The original recovery did not
execute Bash/Core. The later portability qualification below uses a pinned
external Core and the existing Git-bundled native shell, not WSL or a global install.

**Current calibration state:** the three real previews were generated under
`7dad69df7f5f7cba2eaf83fb1db1a50c0ad6338c` and are all owner-approved for initial
listening; see the [separate approval record](../../docs/assets/t0-sound-approval.json)
and [style guide](../../docs/assets/t0-sound-style.md). Calibration 01 is exhausted
at **3/3**. No generation, old-source QC replay or new authorization is part of this
portability repair. Historical "unspent", approval and host-execution statements
below describe their original checkpoints, not the current budget.

## ADR-0152 continuation: offline recovery, no new generation

At source base `d22a8ed5efb7bbc55866ba15f5c866dcfca9239d`, the coordinator selected
**Clay Mixer wet work** as the one next candidate purpose. `MachineEffects` routes
`MIXER` through `washer`; its RUNNING sound is still the generic light hum
(.04 chance, .15 volume, 1.2 pitch), not wet-material movement. The same method also
serves `SETTLER`, which is not automatically included; Hand Pump is outside this unit.
The approved stone/granular Quern A and the switching click do not represent mixing
wet clay. Vanilla splash transients are a reuse option, not an established continuous
mixing texture. This selection is not generated audio, a routing change or acceptance.

Generation has **two independent blockers**:

- **Explicit secret configuration not located.** Presence-only inspection on the
  local child and personal PC (bounded read-only SSH) found no `ELEVENLABS_API_KEY`
  in Process/User/Machine, no API-key-file environment references, and no service
  configuration in the explicitly inspected configuration locations. This is not a
  claim about uninspected storage. No secret-file contents, values, account data or
  private paths were recorded or copied. Historical Agents-secret metadata does not
  establish injection or reveal a value. A separately authorized metadata-only check
  of the original session's artifacts and Eleven-named Downloads files found no explicit
  `SecretFile`/`ApiKeyFile` reference; that bounded follow-up is closed. Do not broaden
  the search or request the key.
- **The current contract cannot authorize this purpose.** The preserved runner,
  manifest and grant schema bind Quern/three requests/Astra and require an external
  `-SecretFile`. They do not honestly represent Clay Mixer/two requests/Sol. The old
  grant stays exhausted3/3 and immutable. Any later implementation requires a new
  reviewed purpose-specific contract, coordinator authorization and ledger, an existing
  secure credential and reserved verified codec tools; no POST is authorized now.
  Existing entitlement/game-incorporation review stays closed, and standalone audio
  library distribution remains prohibited.

The completed fallback adds `test-generation-flow.ps1` and its SH twin. It calls the
**real `Invoke-Generate`** and actual durable receipt/source/provenance helpers, while
substituting only the checkpoint/authorization boundary, transport and codec QC.
Each fixture uses a deliberately **unapproved** marker (`generation_approved=false`,
`approved_by=null`) that the real authorization guard rejects; no test grant claims
to be Astra or Sol. Transport receives an empty sentinel path, never reads a key,
and returns conspicuous fake MP3 text, not audio. All fixtures are removed in `finally`.

Ten isolated cases cover success/completed-ledger reentry, ambiguous delivery before
and after one saved response, HTTP429, reservation failure before/after durable write,
raw-file collision, provenance failure, final saved-receipt failure and refusal of a
nonempty directory without a ledger. A persisted/uncertain attempt stays blocked on
restart with no additional transport; only a never-persisted, never-sent reservation
can continue. Existing raw/provenance artifacts survive failures for review. This
proves orchestration under substituted boundaries, not live HTTP/TLS, a real crash or
power-loss filesystem semantics, codec quality, sound rights or listening.

| Executed local surface | Result |
| --- | --- |
| Focused flow, WinPS5 | 200 assertions / ten cases, exit0 |
| Default offline guard, WinPS5 | 128 existing checks +200 flow assertions, exit0 |
| Default offline guard, existing hash-verified Core7.6 portable | 128 existing checks +200 flow assertions, exit0 |
| New flow SH twin, existing Git shell + same Core | 200 assertions / ten cases, exit0 |
| Preserved `-JsonOnly` guard, WinPS5 | 117 checks, exit0 |

Source identity for these executions is the base above plus the owned changes, not a
claim to have run an unmodified base. The tested working-file SHA-256 values are:

| Input | SHA-256 |
| --- | --- |
| `test-generation-flow.ps1` | `dda3d2d840ee78c91a3043a7de259ec703a0e9527b9fd0ddd577094a1339d476` |
| `test-generation-flow.sh` | `6e0b289fef92f303f32f3c482225f60a715cd07c1ece2cff794f65d6a581cbdf` |
| `test-sound-design.ps1` | `e0940260a3ed761439d2a50bf445fbb10de7639aad7526bb7e469bb818dc5bfe` |
| Unchanged production runner | `29e1b073f14cfff9491d98e407aac110a9320f4d11759dfc9214de3519e85184` |

The normal offline guard now includes the flow suite without a workflow edit.
`-JsonOnly` retains its narrower existing boundary. To run only the new coverage:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\sound-design\test-generation-flow.ps1
```

Its `.sh` twin uses existing `pwsh`; no installation or new runtime/build dependency
is introduced. New provider requests, credential-file reads, codec/JVM/client/server
launches and deployments: **zero**. The exact34,364-byte Quern A retains SHA-256
`811b29471e3fa6a51ce76db2241259d02adfc64e244eadc096ef376d1d1970d5`;
its runtime, notice, historical manifest/contract/receipts and all game resources stay
unchanged. Native lifecycle, one/eight-machine listening and the full T0 sound pass
remain separate pending observations.

**Recovery is not a continuation pass.** The original four drafts survived the
crash; no runnable pipeline, paid output, codec QC or listening pass survived.
This phase implements and tests offline tooling only. Do not read a secret or
generate before Astra reviews these files, checkpoints them and issues a **NEW**
explicit private grant. Astra owns global documentation, actual grants and Git.

The palette's `planned_unapproved` and per-sound approval fields remain artistic/import
states. A reviewed checkpoint plus one private per-run grant authorizes only the
three internal calibration requests; it does not approve those sounds for the game.
The offline guard twin also runs in CI without FFmpeg, a credential or provider access.
Real codec QC is separately bound to the portable tool receipt and must not be inferred
from that lightweight CI step.

## Offline now

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/sound-design/sound-design.ps1 -Mode Plan
powershell -NoProfile -ExecutionPolicy Bypass -File tools/sound-design/test-sound-design.ps1
```

Plan reads only the manifest/contract (optionally the unapproved example via
`-GrantFile`), validates the exact contract, prints the three request bodies and
their non-secret input hashes, and reports tool discovery. It does not create
candidates/receipts, read a key, call a provider or assert authorization.
The tests create synthetic **TEST / NOT ART / NEVER SHIP** WAVs and fake MP3 text
in a unique local temporary directory and remove them even on failure. They never
invoke Generate; transport is replaced with a failing offline guard.

### ISO JSON portability repair (source `1ced812`)

The three reported CI runs `39175468394`, `39175472294` and `39175468238`
remain historical **failures**, not retroactive passes. The preserved verify-job
`117817109618` log from push `39175468394` stops at
`SD_VALIDATION: Missing attempt timestamp.` Foundation passed; smokes were
**SKIPPED**, not failed. No workflow/guard bypass was added.

This is reproduced locally on Microsoft PowerShell **7.6.0**: plain
`ConvertFrom-Json` converts an ISO UTC JSON string to `System.DateTime`, so the
original `Read-SoundJson` receipt roundtrip fails its required string check.
WinPS **5.1.26100.9711** preserves `System.String`. The test-only JSON clone had
the same coercion problem, allowing negative grants to reject for the wrong reason.

`ConvertFrom-SoundJson` now feature-detects the cmdlet's `DateKind` parameter and
uses **`DateKind=String`** where supported, retaining the WinPS5 parser fallback.
Both file reads and test cloning share it. The string requirement was not relaxed:
grant/receipt timestamps reject non-strings, invalid calendar dates, non-UTC/
noncanonical suffixes and malformed values. Optional completion timestamps remain
null or valid original UTC strings. Limits, reservation-before-HTTP, hashes,
duplicate detection and terminal ambiguous-state behavior are unchanged.

Tests cover exact write/read and clone roundtrips (precision, Z suffix, nested
strings, numbers/bools/null), valid grants/receipts after cloning, and non-string/
invalid dates. Each negative grant starts from a positively validated clone and
checks the expected diagnostic code, not just any failure.

| Local test | Result |
| --- | --- |
| Unmodified CI guard entry on Core 7.6.0 | exit **1**, exact original timestamp failure reproduced |
| Fixed focused guards, WinPS5 | **117 PASS**, exit **0** |
| Fixed focused guards, Core 7.6.0 | **117 PASS**, exit **0** |
| Actual SH twin via Git GNU Bash 5.2.37 and Core 7.6.0 | **117 PASS**, exit **0** |

The focused `-JsonOnly` mode stops before synthetic WAV creation; no audio is
created or decoded. It still checks JSON/contract/grant/receipt/budget failures.
The **default CI invocation is unchanged** and continues into the complete
existing audio/HTML/HTTP-object guards plus the new JSON cases. No post-fix full
CI/Linux run, full audio-fixture suite or real codec replay is claimed here.
Native Git Bash on Windows is not a Linux-host qualification. Core without
`DateKind` support is not qualified by these results; use the supported modern
Core path or the verified WinPS5 fallback.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/sound-design/test-sound-design.ps1 -JsonOnly
& 'E:\grindless-earlygame-storage\tools\powershell-7.6.0-win-x64\package\pwsh.exe' `
  -NoLogo -NoProfile -NonInteractive -File tools/sound-design/test-sound-design.ps1 -JsonOnly
```

The chosen Core test first failed to start because `pwsh` was absent. Only then
the authorized portable **Microsoft PowerShell 7.6.0 win-x64** was installed at
`E:\grindless-earlygame-storage\tools\powershell-7.6.0-win-x64`.
The exact official [release](https://github.com/PowerShell/PowerShell/releases/tag/v7.6.0),
`PowerShell-7.6.0-win-x64.zip` and `hashes.sha256` were verified before extraction/
execution; all 986 ZIP paths and symlink/reparse attributes were checked, and
`pwsh.exe` has a valid Microsoft Authenticode signature. Archive/source/licence
material and `installation-receipt.json` remain with the portable. No MSI,
registry, global PATH, C-drive installation or repository binary was introduced.

- Archive: **117125973 bytes**, SHA-256
  `9e725837af682b87bb212cd1efe3657c06c540404203810857ec2516ae2ca322`
- Official checksum file SHA-256:
  `386045028a303005b0f82544c3b47e768a443b279c29b6393399f3c4e75fa7fd`
- `pwsh.exe` SHA-256:
  `7851e9d67e141bcebc0cc37086579fde0b5f0a47297bd2d446eace5d4b8e75c1`

The actual Git distribution resolved from `Get-Command git` supplies
`usr/bin/sh.exe` (GNU Bash 5.2.37). The SH test adds Core and that shell's utility
directory to the **child process PATH only**; it does not install Bash. Core test
processes disable telemetry/update checks and load no profiles.

All original CI/reproduction/focused-test logs and exits are retained in the new
ignored `build/validation/sound-json-portability-1ced-20261018T2322` directory.
Two inspection-harness failures are preserved too: the first shell lookup checked
only `bin/sh.exe`, and a direct Git-blob/execution-file hash comparison initially
overlooked line endings. The old Git object has 593 LF lines; its historical
Windows CRLF form exactly matches the **unchanged** executed runner hash
`48eda73793e417e58689c43cb4123556c46257a3a200a6023d22953b92adf0d0`.
No historical file or binding was rewritten to hide either issue.

This repair's tested working-file hashes (not a retag of `7dad`):

- Runner: `29e1b073f14cfff9491d98e407aac110a9320f4d11759dfc9214de3519e85184`
- Offline tests: `2250986820ee9b9e66847f575bda912e01b91b677771c5f35e5bcdbfc0f79d87`

The frozen manifest/contract/approval, original spend receipt and all nine
source/preview/Ogg media hashes were checked unchanged. No key/private grant or
account was accessed; no provider/JVM/codec was invoked and no runtime, generator,
asset or other worker's fixture was changed. Only byte-hash integrity checks
read the approved audio. The original generation/codec evidence below keeps its own
source/hash/host boundary. Astra must review/checkpoint this new tooling version
and run normal CI; this unit neither publishes nor begins Quern-loop integration.

**Initial tool blocker, now resolved by the authorized portable below:** neither
`ffmpeg` nor `ffprobe` was in PATH or the bounded standard-installation checks.
The first recovery correctly failed preflight; it did not install anything or
claim real codec validation. Tools remain opt-in via explicit paths: Generate/QC
still fail without both executables and the Vorbis encoder before secret access
or paid HTTP. No PATH, registry, normal build or runtime dependency was added.

### Phase-1 recovery evidence

Base `102d68f5af0374f84aa77633269c25b1dd365c06`, owned changes uncommitted for
Astra. Runner SHA-256
`f180b560fd3e727c7b493e5d49ef58a95b84a1ee5ab6d6df6221e40686f45b02`:
WinPS5 Plan passes read-only (350/372/394-character prompts), all **67 offline
checks pass**, and QC correctly reports `SD_TOOLS_MISSING` without creating an
output directory. PowerShell syntax and scoped whitespace checks pass. Synthetic
fixtures are removed. No Generate invocation, secret read, provider/account call,
real codec/listening pass, SH execution, JVM, installation, commit or push is
claimed. Live TLS/provider behavior and actual MP3/Vorbis conversion remain
unverified, not silently accepted. This is worker evidence, not independent QA.

### Approved portable and real-codec follow-up

The owner subsequently authorized the small **Gyan 9.0.2 essentials portable**,
not a global install. Public archive:
`https://github.com/GyanD/codexffmpeg/releases/download/9.0.2/ffmpeg-9.0.2-essentials_build.zip`
(GitHub asset `574302090`), **114768076 bytes**, SHA-256
`60f467265b1e312373dbcd92200c2618a74850f98d3d078e94296bb3fa2047ba`.
The downloaded publisher `.sha256` at
`https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-9.0.2-essentials_build.zip.sha256`
matches. Size/hash were verified **before extraction or execution**. All 49 ZIP
entries were checked for traversal, absolute/ADS/device paths, collisions and
symlink/reparse attributes. All bundled notices remain with the extracted tool.

Installation: `E:\grindless-earlygame-storage\tools\ffmpeg-9.0.2`.
It contains the original archive/checksum, `installation-receipt.json`,
`source-licence-receipt.json` and `package\ffmpeg-9.0.2-essentials_build`.
The publisher's retained README identifies source
`https://github.com/FFmpeg/FFmpeg/commit/946fcce07b` and GPL v3.
This GPL tool is external development infrastructure only: **never copy its
executables into the repository, mod, pack or release**.

Both actual binaries identify `9.0.2-essentials_build-www.gyan.dev`; real
`-version`/`-encoders` preflight confirms `libvorbis` (and `libmp3lame` for TEST
fixture construction). Binary SHA-256:

- `ffmpeg.exe`: `3256173f3f8bffd7df12227c68adf68025edb1832273a9530688a7bb1ed8edec`
- `ffprobe.exe`: `f0d36ecbbdd3bcfac3efa078c96c7271c2e68b3810595552ac3b7f17e9a65c52`
- bundled `LICENSE`: `8ceb4b9ee5adedde47b31e975c1d90c73ad27b6b165a1dcd80c7c545eb65b903`

The real WinPS5 stderr defect is preserved, not rewritten as a pass:
`native-capture-before.json` records the old helper rejecting informational stderr
although an independent concurrent-stream reference exited **0** (657 stderr
characters). Only `Invoke-AudioTool` was repaired: concurrent .NET stream reads,
real exit-code checking, safe argument quoting, separate JSON stdout and
in-process diagnostics. `native-capture-after.json` passes the same actual input.
Expected nonzero exits remain failures; no preference downgrade or discarded
stderr is used. The first post-fix harness invocation also hit the host's
restricted script policy; that failure is retained separately. The documented
`-ExecutionPolicy Bypass` is process-scoped, never a registry/persistent change.

**Real qualification: PASS, 94 checks = 67 offline guards + 27 codec checks** on
WinPS `5.1.26100.9711`. `test-codecs.ps1`/`.sh` reuse the original offline suite
and fixture writer; they never invoke Generate or SendProviderAudio. Positive
Plan/QC CLI runs use explicit executable paths. Real MP3 decode → mono 44.1 kHz
PCM16 preview → Vorbis q4 → decoded float QC passes for three synthetic signals,
each measured as **3.0 seconds** after processing. The stereo/48 kHz input is
flagged before conversion; silence and deliberately broken boundaries remain
flagged after encoding. An independent fake ambiguous receipt becomes terminal
`needs_review` in the actual QC CLI, without any provider call. A concurrent-stream
stress case captures 5165 stdout/7066 stderr characters with exit 0; a genuinely
invalid input retains its nonzero exit and diagnostics.

Retained evidence, including expected failures and **TEST / NOT ART / NEVER
SHIP** fixtures/receipts, lives only under:
`E:\grindless-earlygame-storage\evidence\sound-codec-102d`.
The qualified run is `TEST-codecs-20261018T221648Z-696bae22`; its
`TEST-codec-results.json` SHA-256 is
`7515acfe81d03084ff800ccc8b1b25d5a06c88b0339b88435ce3fa8768b23d2d`.
It inventories all fixture/source/derived/report hashes, native diagnostics and
fake receipts. No audio from this run belongs in Git or the pack.

Exact source SHA-256 for this qualification (base `102d68f`, uncommitted for Astra):

The subsequent coordinator checkpoint
`a2aea87054fa63bea5c3ff045286a9096de272fe` is the effective current pin at freeze;
it changes only documentation/state, not these owned inputs. The successful run
and its receipt retain their actual `102d68f5af0374f84aa77633269c25b1dd365c06`
execution pin/hashes. They were not restarted or relabelled after the HEAD change.
Owned implementation remains uncommitted; no provider grant exists.
The external `freeze-a2aea870-verified/freeze.json` inventories all owned byte
hashes and the actual 102d-based tracked diff, with complete untracked-source
snapshots alongside it. The earlier inventory-only Git executable-resolution
failure remains in `freeze-a2aea870/failure.json`; it did not rerun codec tests.

| Source | SHA-256 |
| --- | --- |
| runner | `48eda73793e417e58689c43cb4123556c46257a3a200a6023d22953b92adf0d0` |
| original offline tests | `b76253a38d4e89044090c056d2479565136d49aa41c57503c107f1e6a966af5e` |
| real-codec tests | `8a8503390ec30115f289f827cdb66fd3d3e879da003717df5fb74e5b9c8c471d` |
| frozen manifest | `c39e76c88e4755d51b3d3ce76c4d819cdf985c6d5977fd614ce3115dc155732e` |
| frozen contract | `2a986693cb3a919a573da5de0739c0486dde3c2d462004a7d64eae3607829a43` |

Exact **synthetic TEST output** SHA-256 values, not paid/art candidates:

| Fixture | Raw MP3 | Mono preview WAV | Candidate Ogg |
| --- | --- | --- | --- |
| TEST sine/stereo48k (`quern-a`) | `5472740b8d9acd82270141100d0c8fcb71302042a010da7e305d2004cda7adc1` | `7258e8849a2214ba0f2cce62bb778bde01afbb3f5c1c73823b18c7dbce5063ac` | `d84e59989ebb4989bf69801c00bfcf8152b6f0fa8f69536f60c1466e29014bde` |
| TEST silence (`quern-b`) | `b918310707f79b561c6781e564a330d190d2817eb84b9458ff57d0ff1bd5ce13` | `611d48c765b4a948f350d2238a9eaef609bbaa7e8e4d782a2aee5ce80932db89` | `4040269cd7e12314862719c395b739a8880ee4a088c6f0c178954ac6104e11ba` |
| TEST seam (`quern-c`) | `5afc2f9995b3f8dac511c8c154257590e15494e81220afa7f528a424f3246d5f` | `817760ac0c84a2a4f5e37b63c7db5d6d7771d811b1d50ceaef355b9895fe9088` | `c8887e9373b1609ece56c53b54679982f950162231be763b5a573fbc27cea51d` |

Repeat the local test only when affected inputs justify it; each invocation
preserves a distinct TEST directory, never overwrites prior failures:

```powershell
$bin = 'E:\grindless-earlygame-storage\tools\ffmpeg-9.0.2\package\ffmpeg-9.0.2-essentials_build\bin'
powershell -NoProfile -ExecutionPolicy Bypass -File tools/sound-design/test-codecs.ps1 `
  -FfmpegPath "$bin\ffmpeg.exe" -FfprobePath "$bin\ffprobe.exe" `
  -EvidenceDirectory 'E:\grindless-earlygame-storage\evidence\sound-codec-102d'
```

**Limits:** these hashes identify this exact retained run, not a byte-identical
cross-version/platform encoding guarantee. No listening/factory/runtime pass,
live provider/TLS behavior, timeout/power-loss fault injection or SH execution is
claimed. Source/contract and all three artistic prompts stayed frozen. **Paid
budget remains UNSPENT**, with no secret read, account request, JVM, app/source
asset import, global documentation edit, commit or push. Only Astra's new grant
after review/checkpoint may authorize the three real calibration outputs.

## Later, only after Astra's new authorization

Keep the real grant and secret outside tracked source; `/local/` is already
ignored, but the **secret must remain outside the entire repository and output**.
Use `grant.example.json` only as a schema template; it is deliberately unapproved.
Astra must fill:

- a new bounded grant ID, `approved_by: "Astra"` and a non-sensitive authorization
  reference, approval/expiry UTC timestamps;
- all three commit fields with the current reviewed Git checkpoint, plus the
  manifest/contract/runner SHA-256 values reported by Plan **at that checkpoint**;
- the exact absolute, local, ignored/external `output_directory`;
- three fixed candidate IDs and at most three calls;
- generation-time `owner_confirmed_paid_subscription`, without querying account,
  plan identity or billing data. The owner already confirmed paid entitlement.

The runner rejects dirty/untracked owned source, different input paths, changed
hashes, a changed HEAD, expired grants, another output folder, expanded budgets,
publication/import approval, extra body parameters or format/model fallbacks.
SecretFile is a **path argument**, never the credential value. No default drive,
environment-variable key, CLI key, credential hashing, transcription, key copy,
account request or raw HTTP error logging is implemented.

Authorized invocation shape (placeholders, **not permission to run now**):

```powershell
# Supply absolute paths chosen privately; do not put a key in any command.
# .../sound-design.ps1 -Mode Generate -GrantFile <private-grant.json> `
#   -OutputDirectory <exact-granted-folder> -SecretFile <external-secret-file> `
#   -FfmpegPath <ffmpeg.exe> -FfprobePath <ffprobe.exe>
```

Only the official reviewed HTTPS POST is implemented; normal certificate
validation and revocation checks stay enabled, redirects are disabled, timeout is
60 seconds, response size is bounded and there is no automatic retry. One call
produces one binary MP3. No seed, batch, `enable_logging`, alternative endpoint or
format is sent. Diagnostics expose only fixed codes, numeric HTTP status and an
allowlisted request ID, never response bodies, raw headers or exceptions.

## Receipt and crash rules

`receipt.json` is a durable **spend ledger**, not disposable cache. It binds the
grant hash/ID, reviewed inputs, checkpoint and single output folder. Creation
requires an empty folder (apart from its persistent lock). An exclusive file lock
serializes use. Each attempt consumes a slot **before** any HTTP: write/flush a
same-volume atomic replacement, reread and validate, then send.

- `attempting` after an interruption becomes terminal `needs_review`;
- timeout, cancellation, unexpected success type, invalid response or uncertain
  storage/delivery becomes `needs_review`, never “retryable”;
- non-200, including redirect/auth/rate/server errors, becomes `http_error` and
  halts all remaining calls;
- `response_saved` requires the raw file/hash; duplicate candidates are skipped;
- all attempted states count toward three. Changed/corrupt/missing source or
  ledger data fail closed; no fourth call or automatic reroll.

**Never delete, edit, relocate or recreate the ledger or issue a replacement grant
to bypass an uncertain spend.** Review provider delivery manually with Astra.
File locking, flush and atomic replacement protect process crashes; this is not a
remote billing ledger, tamper-proof store or full disk/power-loss recovery system.
No tool can preserve the limit if its operator deliberately discards all evidence.

## QC and audition (offline, after saved responses exist)

```powershell
# .../sound-design.ps1 -Mode QC -OutputDirectory <candidate-folder> `
#   -FfmpegPath <ffmpeg.exe> -FfprobePath <ffprobe.exe>
```

QC takes no grant/secret and never calls HTTP. Media probes/decodes/transforms
allow only FFmpeg's local `file` protocol, not embedded remote playlist URLs.
It validates the existing receipt and source hashes. Each saved source yields:

- unchanged `quern-*.raw.mp3` and `.provenance.json`: provider, UTC, request ID if
  available, exact prompt/model/parameters, checkpoint/input/source hashes,
  intended use and internal rights scope, with no account data;
- `.preview.wav`: fixed -3 dB gain, mono 44.1 kHz PCM16;
- `.candidate.ogg`: Vorbis q4 from that preview, **not imported media**;
- `.qc.json`: source/proc/encoded format, channels, rate, duration, peak/RMS,
  near-clipping, silence fractions/gaps, leading/trailing silence, boundary sample/
  slope jumps and 10 ms energy mismatch; hashes and exact FFmpeg arguments/version;
- `audition.html`: local `<audio controls loop>` with no server, remote resources
  or scripts. Open it directly in a browser.

No trim, seam repair or hidden “improvement” is applied. Duration tolerance is
±0.12 s; clipping threshold is 0.999; silence is assessed below -60 dBFS with low
RMS/long-gap flags. Boundary thresholds are screening heuristics, **not a listening
pass**. Metrics remain visible even if flagged; compare source and processed
versions, repeated joins, one/eight-machine fatigue, gain/distance/stop latency
before any later runtime integration. A loopable file is not a managed runtime
loop; current MachineEffects callers remain randomized positional one-shots.

All outputs start **INTERNAL_CALIBRATION_ONLY**, import/publication false. Owner
paid entitlement is not CC0. The official creative terms permit game incorporation
subject to plan and prohibit standalone audio distribution; technical OGG
extractability alone does not demand a newly invented contract. Astra reviews the
actual release use before import/publication. No public GitHub audio or standalone
library now. The user chooses dry/granular, woody resonance or quiet abrasion
before a style guide, more assets or any expansion. No bundle/publication is
performed by this tool.
