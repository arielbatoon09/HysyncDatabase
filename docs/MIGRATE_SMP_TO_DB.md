# Migrate SMP Player Data to the Database

This guide explains how to **one-time migrate** existing player inventory (and hotbar) from your SMP server’s local files into the shared PostgreSQL database. After that, every server that has **HysyncCore** + **HysyncDatabase** will read and write that same DB (load on join, save on leave).

---

## 1. Prerequisites

- PostgreSQL is running and the **V1 migration** has been applied (`players` and `player_inventory` tables exist).
- **HysyncDatabase** config exists at `mods/HysyncData/config.json` (same DB URL you use in production).
- You have a **directory of player JSON files** from your SMP world save (see below).

---

## 2. Where Hytale Stores Player Files

Player data is under the **universe** folder: each player is a **directory** named by UUID, and the directory contains the player JSON file(s).

- **Pattern:** `universe/players/<uuid>/` (directory per player; JSON file(s) inside)
- **Example:** `D:\HytaleProject\Server\universe\players\142dcce7-b7b6-45c9-a3a8-352082c33e7f`

Pass the **path to the `players` folder** (the parent of the UUID-named directories). The migration tool will:
- List each child of `players/`: if it’s a directory whose name is a UUID, it uses the first `.json` file inside that directory.
- It also supports a flat layout `players/<uuid>.json` if you have that instead.

So use: **`universe/players`** (or the full path, e.g. `D:\HytaleProject\Server\universe\players`).

---

## 3. Run the Migration

### Option A: In-game command (easiest)

With **HysyncDatabase** loaded on the server (and DB connected), an op can run:

```
/migrateinventory
```

This uses the default path: **universe/players** under the server run directory. To use a custom path:

```
/migrateinventory D:/HytaleProject/Server/universe/players
```

The migration runs in the background; you get a message when it finishes (OK / SKIP / ERR counts). Restrict this command to ops/admins via your server’s permission system if available.

### Option B: Gradle

From the **HysyncDatabase** project directory, with config in `run/mods/HysyncData/config.json`:

```bash
# Create run/mods/HysyncData and config if needed, then:
./gradlew migratePlayerFilesToDb -PplayersDir=D:/HytaleProject/Server/universe/players
```

Use the path to the **players** folder (the one that contains UUID-named directories). The task uses `run` as the working directory so `mods/HysyncData/config.json` is found.

**Windows:**

```cmd
gradlew.bat migratePlayerFilesToDb -PplayersDir=D:\HytaleProject\Server\universe\players
```

### Option C: JAR manually

1. Build the plugin: `./gradlew shadowJar`
2. Ensure `run/mods/HysyncData/config.json` exists (same DB settings).
3. Run from the **run** directory (or set `user.dir` so config is found):

```bash
cd run
java -cp ../build/libs/HysyncDatabase-0.0.1.jar org.hysync.database.tools.MigratePlayerFilesToDb worlds/SMP/players
```

Replace with the path to your **players** directory (e.g. `D:\HytaleProject\Server\universe\players`).

---

## 4. What the Migration Does

- Scans the given **players** directory:
  - **universe/players/&lt;uuid&gt;/** – for each UUID-named subdirectory, uses the first `.json` file inside it.
  - **players/&lt;uuid&gt;.json** – also supports flat `<uuid>.json` files if present.
- For each file:
  - Reads `Components.Player.Inventory` and `Components.Player.HotbarManager` (if present).
  - Resolves display name from `Components.Nameplate.Text` or `Components.DisplayName.DisplayName.RawText`.
  - Calls **InventorySyncService**: `setInventory(uuid, displayName, inventoryJson, version)` and, if present, `setHotbarManager(uuid, hotbarManagerJson)`.
- Uses **ON CONFLICT** upserts: existing rows are updated; new players are inserted.

No local player files are modified or deleted.

---

## 5. After Migration

- **SMP and Main Town** (and any other server) with **HysyncCore** + **HysyncDatabase** and the same DB config will:
  - **Load** inventory from the DB when a player joins (AddPlayerToWorld).
  - **Save** inventory to the DB when a player leaves (PlayerDisconnect).
- So the DB becomes the shared source of truth; migration only needed once to backfill from SMP files.

---

## 6. Troubleshooting

| Issue | What to do |
|-------|------------|
| "Failed to load config" | Ensure `run/mods/HysyncData/config.json` exists and is valid, or run from the server run directory. |
| "Not a directory" | Check that `-PplayersDir` points to the **players** folder (e.g. `.../universe/players`). |
| "Failed to connect to database" | Check PostgreSQL is running and `config.json` (host, port, name, user, password) is correct. |
| SKIP (no Components / no Player.Inventory) | File is not a valid Hytale player JSON; safe to ignore. |
| FAIL setInventory | Check DB schema (V1 migration applied) and logs for SQL errors. |

---

## 7. Summary

1. Apply DB migration (V1) and configure `mods/HysyncData/config.json`.
2. Run **MigratePlayerFilesToDb** once with the path to your SMP **players** directory.
3. Deploy HysyncDatabase + HysyncCore on all servers (SMP, Main Town) with the same DB config.
4. Each server will then read and write the shared inventory from the database.
