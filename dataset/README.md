# Dataset

This directory contains the activity dataset and tooling used to seed the LEARN-Hub database.

## Contents

| File / Directory | Description |
|---|---|
| `dataset.csv` | Activity metadata and markdown content (camelCase columns) |
| `pdfs/` | Source PDF files referenced by `filename` column in the CSV |
| `seed.py` | CLI tool for seeding activities and exporting markdowns |

## CSV columns

### Metadata

| Column | Type | Description |
|---|---|---|
| `filename` | string | PDF filename in `pdfs/` |
| `name` | string | Activity title |
| `source` | string | Origin / publisher |
| `ageMin` | int | Minimum age |
| `ageMax` | int | Maximum age |
| `format` | string | `digital`, `unplugged`, or `hybrid` |
| `resourcesNeeded` | string | Pipe-separated list of materials |
| `bloomLevel` | string | Bloom's taxonomy level |
| `durationMinMinutes` | int | Minimum duration in minutes |
| `durationMaxMinutes` | int | Maximum duration in minutes |
| `mentalLoad` | string | `low`, `medium`, or `high` |
| `physicalEnergy` | string | `low`, `medium`, or `high` |
| `prepTimeMinutes` | int | Preparation time in minutes |
| `cleanupTimeMinutes` | int | Cleanup time in minutes |
| `topics` | string | Pipe-separated list of topics |
| `description` | string | Activity description |

### Markdown content

These columns are populated via `seed.py export` after activities have been seeded and markdowns generated on the server.

| Column | Description |
|---|---|
| `deckblattMarkdown` | Cover page markdown |
| `artikulationsschemaMarkdown` | Lesson structure (AVIVA+) markdown |
| `hintergrundwissenMarkdown` | Background knowledge markdown |

## Dataset statistics

- **Total activities:** 37
- **Sources:** Code.org (10), CS Unplugged (10), TUM LearnLabs (8), Barefoot Computing (5), Micro:bit (4)
- **Bloom levels:** Create (15), Analyze (11), Apply (7), Understand (2), Evaluate (2)
- **Topics:** Algorithms (32), Patterns (22), Abstraction (17), Decomposition (14)

## Prerequisites

```bash
pip install requests
```

The LEARN-Hub server must be running and accessible.

## Usage

The `seed.py` script has two subcommands: `seed` and `export`.

### Seeding activities

Uploads PDFs and creates activities from the CSV. By default, markdowns are read from the CSV columns. If the CSV has no markdown content (or you want fresh generation), use `--generate-markdown`.

```bash
# Seed all activities using markdowns from CSV (default, fast)
python seed.py seed --password <admin-password>

# Regenerate markdowns via LLM API (slower, requires API key on server)
python seed.py seed --password <admin-password> --generate-markdown

# Seed specific activities only
python seed.py seed --password <admin-password> --only barefoot-pizzaparty.pdf

# Skip activities that already exist on the server
python seed.py seed --password <admin-password> --skip-existing

# Use a different server
python seed.py --base-url https://learnhub-test.aet.cit.tum.de seed --password <admin-password>
```

### Exporting markdowns

Fetches all activities from the server API and writes the generated markdown content back into the CSV. This is useful for persisting LLM-generated markdowns so future seeds can reuse them without calling the LLM again.

```bash
# Export markdowns into dataset.csv (overwrites in-place)
python seed.py export

# Write to a separate file instead
python seed.py export -o dataset_backup.csv

# Export from a different server
python seed.py --base-url https://learnhub-test.aet.cit.tum.de export
```

### Typical workflow

(1. & 2. if you want to re-generate with your model; 3. if you just want data)

1. **First-time seed with LLM generation:**
   ```bash
   python seed.py seed --password secret --generate-markdown
   ```

2. **Export the generated markdowns into the CSV:**
   ```bash
   python seed.py export
   ```

3. **Future seeds reuse CSV markdowns (no LLM calls):**
   ```bash
   python seed.py seed --password secret --skip-existing
   ```

### Environment variables

All CLI flags can also be set via environment variables:

| Variable | Default | Description |
|---|---|---|
| `SEED_BASE_URL` | `http://localhost:5001` | Server base URL |
| `SEED_ADMIN_EMAIL` | `admin@learnhub.com` | Admin email |
| `SEED_ADMIN_PASSWORD` | _(none)_ | Admin password |
