# SopCrates

`SopCrates` is a Paper/Spigot crate plugin for the Sop ecosystem. It lets you place physical crate blocks in the world, open them with keys or placeholders, preview rewards, run prize commands, and show crate holograms.

The plugin is built around configurable crate files in `plugins/SopCrates/crates/*.yml` and uses `SopLib` for item/NBT helpers.

## Features

- Physical crate blocks placed directly in the world
- Key-based or PlaceholderAPI-based opening logic
- Weighted reward rolls
- Reward preview inventory
- Prize commands and item rewards
- Multi-line crate holograms
- Optional hologram backend via `SopDisplays`
- PlaceholderAPI expansion support
- Admin commands for placing, removing, reloading, and inspecting crate chances

## Requirements

- Paper or Spigot `1.16.5+`
- `SopLib` required
- `PlaceholderAPI` optional
- `SopDisplays` optional for crate holograms

## Commands

- `/sopcrates chances <crateName>`
  Shows calculated reward chances for the specified crate.
- `/sopcrates give <player> <crateName> <amount> [virtual]`
  Gives physical crate keys or virtual keys.
- `/sopcrates create <crateName>`
  Creates a crate block on the targeted block.
- `/sopcrates remove`
  Removes the targeted crate block.
- `/sopcrates reload`
  Reloads crates, blocks, keys, and related config state.

All commands are currently op-only.

## How It Works

1. Configure one or more crate files in `plugins/SopCrates/crates/`.
2. Use `/sopcrates create <crateName>` while looking at a block to place a crate.
3. Give keys with `/sopcrates give ...` or use placeholder-based opening.
4. Players right-click the crate to preview or open it.

## Crate Files

The plugin ships with:

- `example.yml`
- `afk-case.yml`
- `free-case.yml`
- `mysterious-case.yml`
- `pillars-free.yml`
- `treasure-box.yml`

Each crate file can define:

- display name
- key material, model, name, lore
- opening requirements
- deny/open commands
- hologram settings
- weighted prizes
- preview display items
- reward commands and reward items

## Holograms

`SopCrates` supports crate holograms through an internal abstraction:

- `SopDisplays` backend when the plugin is installed
- noop fallback when no hologram provider is available

This makes crate holograms optional instead of hard-required.

## Placeholders

If `PlaceholderAPI` is installed, `SopCrates` registers its expansion and also resolves placeholders in command execution and placeholder-based opening checks.

## Build

```bash
mvn clean package
```

Built jar:

- `target/SopCrates.jar`

## Repository

- GitHub: [enels0n/SopCrates](https://github.com/enels0n/SopCrates)
