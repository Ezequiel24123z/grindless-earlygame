# Production smoke inventory: 2026-10-17

**41 of 41 distinct scenarios completed successfully on Windows.** This is combined evidence
from bounded batches, not a claim that either interrupted aggregate finished. Each retained
scenario pass includes boot/handshake, all its declared assertions and natural process exit 0.
The manifest is `tools/smoke/scenarios.txt`; interactive acceptance is a separate gate.

| Batch | Revision / interval (UTC+02:00) | Result / runner log under `build/validation/` |
| --- | --- | --- |
| A | `0ca31e3`, 00:03:19 through approximately 00:36 | 23 completed scenarios; interrupted during next preparation, no final exit. `full-smokes.log` |
| B | `9fd6d41`, 13:25:53 through approximately 13:38 | Nine completed scenarios; interrupted during next preparation, no final exit. `remaining-smokes.log` |
| C | `9fd6d41` plus the Planner fixture committed as `e2dfc2c`, 13:47:53-13:54:30 | Five selected scenarios, exit 0; Planner repeated with its corrected stationary fixture. `final-smokes-a.log` |
| D | `e2dfc2c`, 13:55:31-14:01:19 | Final five scenarios, exit 0. `final-smokes-b.log` |

Runtime Java and resources are unchanged between `0ca31e3` and `e2dfc2c`; the later commits
change validation/preparation and documentation. The built and loaded mod jars still share
SHA-256 `e0267ee23fdcfde776749df069e29effc72379dfca3b4fb58dc0b0e95a307866`.
No smoke listener remained on port 25599 after the final batch.

Each raw server log is `forge/run/smoke-<scenario>.log`. Hashes below identify the retained
files; these ignored logs are local execution artifacts, not a promise of permanent availability.
Batch C supersedes B's earlier Planner run. A generated local index also exists at
`build/validation/complete-smoke-inventory.csv`.

| Scenario | Batch | Server log SHA-256 |
| --- | --- | --- |
| boot | A | `1a1e0aefe4ce7518fa4bc5a57add33400b6cfef426ef49719b5b8bd85490a611` |
| foreign-providers | A | `9a4954aafd46e5545978b9e281f43b8d4afa6781dfbae054b9151aabf6fefd1b` |
| blocks | A | `8179f1f42a725efd1dfb2f86c1f427ba11d58eaedec7d34620cf61aefb6ec961` |
| states | A | `2c31a76a0b739c68ffa88ef16a039818abf4039485a3b12fe362e5dc9f46493a` |
| loop | A | `30787b6c3616f5c2c9087f69c1b47778def68990dc510d60dc57dcf04d40e454` |
| bootstrap | A | `e52efbbb441da4a29a85eb3ecd87acc623fe77e9d311d6b6aaec2f47f33a4ec5` |
| first-iron | A | `2ecdd246b4dcf69dee5a997555de394263d4ca5ca854ead5809bbd539f6eba10` |
| first-factory | A | `43745cf561a2153e240a665824a7c20f817be6a46d61986e96ca2025ca9ebb24` |
| first-fluids | A | `22b2328c90103703b142126d9d672db383045efd657ef378c01641b7d6424919` |
| first-assembler | A | `1612d6ca283720d047dfe8693b2f83ce2d520e21f7eac1a1ad39f2ba97b3910a` |
| matrix-lifecycle | A | `f2d227a2f87a60aef85ff70cca8c8568d41d1d1f94fa0abc5dffbf3915a5b08a` |
| first-energy | A | `19523779a3b79422c6e91b2b1737ba511222ab0af6d6494c2113253a6d7e1e3b` |
| first-kiln | A | `cea90679b7c98a8439dbdcf5ab561257626d619983677dd11fb11aaaaf2693e9` |
| first-wire | A | `21d0f9064e568f6b9a7ce4ee9cfc481c44aeff973668bcb68441bf0c351028a4` |
| first-contact | A | `159fe31054cf42f390bbd1e460f5d3926c97b18b03ccab39d1d1fdd7f05cfbd5` |
| first-washer | A | `3e30fcf8e8a762fa2cd52316ff980cf153dec6e97d481453a2c5945cde8feebd` |
| first-electrolysis | A | `8251dd31a5d4b40f475b608f6cf8f4de24a970c2fe41ccd0a5ba190b60f06d8e` |
| first-well | A | `31d5cafe8addcf11f5721d8e65800473a932f7062cca4a82c586a2030e78909a` |
| first-cast | A | `45db253a9cbfd7050c18feb0d9f6691656794ddc84f6f45651594230c77fc022` |
| first-separation | A | `f49b00a92ab97ab3ef2aea4703e98d82b1653b7f8a002100ee51bb312fd88977` |
| first-steam | A | `22757bc1d88ff97f5a9ac4d8f3717cceb64c7ba050f79fac52d101e1d370856d` |
| first-logistics | A | `36cc5f9c0fd567ef78d5e2e3338e11bab1f43bf6bd525d4aa89816b78955494d` |
| first-logic | A | `7d24f557d934f017001368e22a55209cf124f9291ce2df5e5f061e694d4dd337` |
| first-drill | B | `1680528e7cb80ae9e7b258f63eb97165f0cbfab0d5f4ff7105c645bc9c4addf1` |
| first-blueprint | B | `35099918a858402a8c64dce6edb78caf92f232f3127e413a914a1dc13edb6715` |
| first-planner | C | `549a09ca5557c60e166fec8c0c23cde5cb3bc3a3637977834de539079946b78b` |
| first-patterns | B | `3f1ac6dd67d968304ccdc6e8900107fb72ec2730bfab67114602f71f9e0a0352` |
| first-harness | B | `8e785dcafea46de9fe67fc1f02ea25c5a5e945d331c5498830b7d02cd6da040e` |
| first-exosuit | B | `05dbec637c404ec6b1d21b53b33020910e2e849d3437529bd0d5c385dc142c39` |
| first-refractory | B | `0cf6299016cec94fe83094399bb2f0e980742652789683fd4cfdbf7344b4d98a` |
| first-silicon | B | `e35a0abed3032657346c9b2d7563f860bb951ede7ef6d2692e092bb5e290a19d` |
| first-electronic | B | `e269479e5f4c1c4e2f9f67434d460c08d016ee61e9ef58c23bb5ebee3ce0d47e` |
| first-array | C | `0a494dc76308646dcb0db70defdab84f7a90237bcfe75812acde5f1f04677fa6` |
| first-luna | C | `7d6e963f7ec7fdc77821f79265d39d028ad7064e744853a780fe6f2311687032` |
| factory-world | C | `4c0db037ebc4a1c53c8034e6c31aab91685f338865468027a91bfb429f8c5a3b` |
| first-rocket | C | `f8c3d905c9bfda0cc9c6168fcd84ff45eab98d4c0517302a02101ffe565cd690` |
| first-station | D | `a43083fc9f9aea1c05ace8aa5eb620ed4d3110513c44f7b2f82324a85c51476a` |
| first-drift | D | `5607fce5f1f6ba171230bcdd328f6c566b02173451e8d267630ec4727ea0d857` |
| first-centre | D | `5e3eecab44a5d89d37c8dd3e28c5251ca6518a1ebdb0d7373f119bca8c830016` |
| first-atlas | D | `a8aadb5fc52869a1be36202d0256c7dbddaee5b0fb08ecf59116b346398b9ecf` |
| first-book | D | `e7a32b8ce96305c1a84a59c7bec9550497eeb22758a91c340d465ecff9ef0fbb` |

The earlier development shutdown failures, Planner entity-selector failure and two interrupted
aggregates remain in [the chronological ledger](latest-validation.md). Nothing here claims
interactive screen behavior, actual player claims, no-command survival, or retained-world
OS-process restart acceptance.
