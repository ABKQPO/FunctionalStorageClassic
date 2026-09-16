<div align="center">
  <h1>Functional Storage Classic</h1>
  <h2><em>Drawers, done properly, on 1.7.10</em></h2>

  <p>
    <strong>High capacity drawer storage for items, fluids, and Thaumcraft essentia.</strong>
    <br>
    A faithful backport of <b>Functional Storage</b> to <b>Minecraft 1.7.10</b>, with
    <b>More Functional Storage</b> merged in and first class <b>Applied Energistics 2</b> and
    <b>Waila</b> integration.
  </p>

  <p>
    <a href="https://www.gnu.org/licenses/lgpl-3.0.html">
      <img src="https://img.shields.io/badge/License-LGPLv3-blue?style=for-the-badge" alt="License">
    </a>
    <img src="https://img.shields.io/badge/Minecraft-1.7.10-green?style=for-the-badge" alt="Minecraft 1.7.10">
    <img src="https://img.shields.io/badge/Forge-10.13.4.1614-orange?style=for-the-badge" alt="Forge">
    <img src="https://img.shields.io/badge/GTNHLib-required-red?style=for-the-badge" alt="GTNHLib">
  </p>
</div>

<hr>

## 📑 Table of Contents
- [📑 Table of Contents](#-table-of-contents)
- [✨ Overview](#-overview)
- [🧱 Storage Blocks](#-storage-blocks)
- [⬆️ Upgrade System](#️-upgrade-system)
- [🧪 Storage Kinds](#-storage-kinds)
- [🔌 Mod Integration](#-mod-integration)
- [🧩 Configuration](#-configuration)
- [🌍 Localization](#-localization)
- [📥 Installation](#-installation)
- [🧱 Dependency Requirements](#-dependency-requirements)
- [🛠️ Building](#️-building)
- [📜 Credits & Attributions](#-credits--attributions)

<hr>

## ✨ Overview

Functional Storage Classic brings the modern drawer experience to 1.7.10 without the modern
engine. Storage is built on a small generic core: one indexed, long-capacity handler powers
items, fluids, and essentia, so every drawer type shares the same routing, upgrade, and
synchronization behaviour.

* **Long capacity storage** — a single slot holds far more than an `int`, with the amount
  rendered on the drawer face.
* **Multiplicative storage upgrades** — copper, gold, diamond, and netherite tiers stack
  multiplicatively instead of overwriting each other.
* **Contents survive breakage** — stored contents, filters, lock state, and installed upgrades
  are written into the dropped block's NBT.
* **1.7.10 automation interfaces** — item drawers expose vanilla `IInventory`; fluid drawers
  expose Forge `IFluidHandler`; essentia drawers use Thaumcraft's aspect and tube interfaces.
* **Controller networks** — a storage controller aggregates every linked drawer into one
  logical inventory for automation.

<hr>

## 🧱 Storage Blocks

### Drawers

| Family | Variants | Notes |
|:-------|:---------|:------|
| Wood Drawers | 6 woods × 1/2/4 slots | Oak, spruce, birch, jungle, acacia, dark oak |
| Framed Drawers | 1/2/4 slots | Exterior, front, and divider take any block's texture |
| Fluid Drawers | 1/2/4 tanks | One long-capacity tank per slot |
| Essentia Drawers | 1/2/4 slots | Thaumcraft essentia; registered when Thaumcraft is present |
| Compacting Drawers | 3 tiers | Compacts 9→1 and 4→1 chains automatically |
| Simple Compacting Drawers | 2 tiers | Lightweight compacting |
| Ender Drawers | 1 slot | Frequency based shared storage, per save |
| Armory Cabinet | 1 block | Bulk storage for gear and tools |

Every drawer is a full cube that can be mounted on a **wall**, a **floor**, or a **ceiling**.
The front faces the player. Looking steeply down or up selects floor or ceiling mounting,
following FunctionalStorageLegacy. Models, contents and clickable regions rotate together.

### Controllers

* **Storage Controller** — indexes every linked drawer and exposes their combined contents.
* **Controller Extension** — extends a controller's reach without consuming another controller.

Drawers are linked with the **Linking Tool**: right-click a controller to select it, then
right-click drawers to bind them.

### Framed Drawers

Craft an unstyled framed drawer from three oak planks and a matching oak drawer in a 2×2
grid. Apply materials with the Legacy styling recipe: exterior and front blocks on the top
row, the framed drawer and an optional divider block below. This preserves the drawer's
contents and upgrades. Each face uses the corresponding material texture, including log ends.

<hr>

## ⬆️ Upgrade System

Each drawer has four **storage upgrade** slots and three **utility upgrade** slots.

### Storage Upgrades

| Upgrade | Default Capacity Multiplier |
|:--------|:--------------------|
| Iron Downgrade | resets to base capacity |
| Copper Upgrade | ×8 |
| Gold Upgrade | ×16 |
| Diamond Upgrade | ×24 |
| Netherite Upgrade | ×32 |
| Max Storage Upgrade | removes the capacity ceiling |

Copper, gold, diamond, and netherite multipliers come from the `storage` configuration.
Fluid and essentia capacity modifiers apply their configured divisors. Reopen the world
after changing storage settings.

Right-clicking installs an upgrade into an available slot. Use the drawer interface to
remove an installed upgrade or exchange tiers.

### Functional Upgrades

| Upgrade | Effect |
|:--------|:-------|
| Void Upgrade | destroys overflow that no longer fits |
| Redstone Upgrade | comparator-style redstone output based on fill level |
| Pulling Upgrade | pulls items, fluids, or essentia from the chosen side |
| Pushing Upgrade | pushes items, fluids, or essentia to the chosen side |
| Collector Upgrade | collects nearby dropped items and fluid sources |
| Ore Dictionary Upgrade | treats ore dictionary equivalents as one stored type |
| Wireless Pulling / Pushing | transfer to and from a recorded coordinate |

### Generation Upgrades

Each comes in four tiers that differ only in output rate.

| Upgrade | Effect |
|:--------|:-------|
| Water Generation | fills a fluid drawer with water |
| Stone Generation | produces cobblestone in a drawer |
| Universal Item Generation | produces the configured item, or the upgrade's own filter |

### More Functional Storage upgrades

The following upgrades from **More Functional Storage** are merged into this mod:

| Upgrade | Effect |
|:--------|:-------|
| Breaker Upgrade | breaks the block in front of the drawer and stores the drops |
| Placer Upgrade | places blocks from the drawer into the world |
| Refill Upgrade | keeps a player's held stack topped up |
| Dimensional Refill Upgrade | as above, across dimensions |
| Stonecutting Upgrade | runs stonecutter-style conversions on stored items |

<hr>

## 🧪 Storage Kinds

| Kind | Container | Interface |
|:-----|:----------|:-----------|
| Items | Wood, compacting, ender drawers, armory cabinet | Vanilla `IInventory` and AE2 ME inventory |
| Fluids | Fluid drawers | Forge `IFluidHandler` |
| Essentia | Essentia drawers | Thaumcraft `IAspectContainer` and `IEssentiaTransport` |

All three kinds share one storage core. A resource kind only has to describe its identity,
capacity, and persistence, and it inherits routing, overflow voiding, creative mode,
lock-filter retention, and change notifications. Essentia icons are drawn with Thaumcraft's own
aspect textures, so compound aspects and addon aspects render correctly without this mod
shipping any Thaumcraft assets.

<hr>

## 🔌 Mod Integration

* **Applied Energistics 2** — drawers are registered with AE2's
  `IExternalStorageRegistry`, so storage buses can address them as ME storage.
* **Waila** — hover any drawer to see its contents, capacity, and installed upgrades.
* **Thaumcraft** — essentia phials exchange eight units with the clicked slot; tubes use
  `IEssentiaTransport` suction and transfer, alongside `IAspectContainer`. Addons can register
  explicit container capacities through `EssentiaContainerRegistry`. Wired and wireless pulling
  and pushing upgrades use `upgradePullAspect` and `upgradePushAspect` as their per-operation
  limits and respect native essentia input/output ports.
* **Hoppers and pipes** — drawers implement vanilla `IInventory` and Forge `IFluidHandler`
  directly, which is how 1.7.10 automation discovers inventories.
* **Comparators** — a drawer emits a comparator signal based on how full it is.
* **The One Probe** — *not supported.* The One Probe was never released for Minecraft 1.7.10,
  so Waila is used instead.

No mixins are used. Every integration goes through a public API or a documented plugin
callback, so this mod never depends on another mod's internals.

<hr>

## 🖥️ Opening a Drawer's Interface

Sneak and right-click a drawer with an empty hand to open its interface. It shows the
drawer's storage slots, its storage and utility upgrade slots, and your inventory, so
upgrades can be swapped without breaking the block.

Right-click a slot with an item to insert the held stack. A second click within six ticks
inserts matching items from your inventory. Left-click extracts one item, or one stack while
sneaking. Buckets and registered fluid containers exchange with the clicked tank; Thaumcraft
phials exchange with the clicked essentia slot.

Right-clicking with an upgrade installs it in the appropriate upgrade group. Sneak with the
Configuration Tool to toggle locking; use it normally to cycle display options.

<hr>

## 🧩 Configuration

Configuration is provided through **GTNHLib** and is editable in game from the mod list.

Categories:

* `general` — content retention, controller range and link limit, armory size, compacting rules,
  ore dictionary filters
* `storage` — upgrade multipliers, per-kind capacity divisors, base slot capacities
* `upgrades` — transfer amounts and tick intervals for every automation upgrade; `universalGenerationTick`
  controls the interval for Universal Item Generation upgrades
* `compatibility` — Waila, Applied Energistics 2, and Thaumcraft toggles
* `client` — drawer render distance and default display options

<hr>

## 🌍 Localization

The mod ships with:

* `en_US` — English
* `zh_CN` — Simplified Chinese

Both files cover block names, item names, tooltips, upgrade descriptions, Waila text, and every
configuration entry.

<hr>

## 📥 Installation

1. Install **Minecraft 1.7.10** with **Forge 10.13.4.1614**.
2. Install **GTNHLib 0.11.48 or newer** — it is a hard requirement.
3. Drop the Functional Storage Classic jar into your `mods` folder.
4. Optional: install Applied Energistics 2, Waila, and Thaumcraft for their integrations.

<hr>

## 🧱 Dependency Requirements

### Runtime

* **Minecraft**: `1.7.10`
* **Forge**: `10.13.4.1614`
* **GTNHLib**: `0.11.48` or newer, required

Optional integrations activate automatically when the relevant mod is present.

### Development

* **Java**: JDK `25` (the build downgrades bytecode to Java 8)
* **Gradle wrapper**: use the included `gradlew` / `gradlew.bat`

<hr>

## 🛠️ Building

```bash
# Windows
gradlew.bat clean build -x test

# Linux / macOS
./gradlew clean build -x test
```

<hr>

## 📜 Credits & Attributions

This project is a port. The original designs, behaviour, and assets come from the following
projects, all of which permit reuse under the terms below.

| Project | Author | License | What was used |
|:--------|:-------|:--------|:--------------|
| **Functional Storage** | [Buuz135](https://github.com/Buuz135), Rid | [MIT](https://mit-license.org/) | Original design, drawer and upgrade behaviour, textures |
| **Functional Storage Legacy** | [xinyihl](https://github.com/xinyihl) | [MIT](https://mit-license.org/) | 1.12.2 port used as the primary reference, textures, models |
| **More Functional Storage** | [Matyrobbrt](https://github.com/Matyrobbrt) | [MIT](https://mit-license.org/) | Breaker, placer, refill, dimensional refill, and stonecutting upgrades |
| **GTNHLib** | [GTNewHorizons](https://github.com/GTNewHorizons/GTNHLib) | [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html) | Configuration system, block state API, JSON model pipeline |
| **Applied Energistics 2** | [AlgorithmX2](https://github.com/AppliedEnergistics) and contributors | [LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html) | Compile-time API for the storage bridge |

### Notes on Thaumcraft

This mod compiles against the **Thaumcraft 4 API** but does **not** redistribute any Thaumcraft
code or assets. Essentia storage is implemented against the public `thaumcraft.api.aspects`
interfaces only. Thaumcraft itself must be obtained separately by the user, and all rights to it
remain with its author.

### Notes on FunctionalChemical

The third-party storage integration pattern in this mod was informed by studying how
**FunctionalChemical** integrates an external chemical system into Functional Storage. No
FunctionalChemical code or assets are included; only the general approach of exposing a foreign
storage kind through the same drawer machinery was adopted, and that approach was re-expressed
for Thaumcraft essentia specifically.

---

<p align="center">Made with ❤️ for the GTNH Community. Licensed under <strong>LGPLv3</strong>.</p>
