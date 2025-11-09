#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<EOF
Usage:
  $0 -f <logfile> [options]

Runs a complete scenario using the local append.sh helper:
- open a map (MapStart)
- sleep between actions
- drop item(s) (BagMod) one or more times
- sleep between actions
- close the map (MapEnd)
- optionally repeat for multiple maps

Options:
  -f, --file <path>           Output log file (required)
  --maps <n>                  Number of maps to run (default: 1)
  --drops <n>                 Number of item drops per map (default: 1)
  --sleepAction <sec>         Sleep seconds between actions (start/drop/end) (default: 1)
  --sleepMap <sec>            Sleep seconds between consecutive maps (default: 2)
  --itemId <id>               Single item id to drop (ConfigBaseId). If not set, defaults to 10042.
  --itemIds <id1,id2,...>     Comma-separated list of item ids to cycle through across drops.
  --append <path>             Path to append.sh (default: locate next to this script)
  --startNext <ScenePath>     Override NextSceneName for MapStart
  --startLast <ScenePath>     Override LastSceneName for MapStart
  --endNext <ScenePath>       Override NextSceneName for MapEnd
  --endLast <ScenePath>       Override LastSceneName for MapEnd
  -h, --help                  Show this help

Notes:
- If both --itemId and --itemIds are provided, --itemIds wins.
- append.sh is invoked to produce UE-style log lines that the parser understands.

Examples:
  $0 -f ./tmp/test.log --maps 3 --drops 5 --itemId 10042
  $0 -f ./tmp/test.log --maps 2 --drops 3 --itemIds 3001,3002,3003 --sleepAction 0 --sleepMap 1
  $0 -f ./tmp/test.log --startNext \
     "/Game/Art/Maps/07YJ/YJ_YongZhouHuiLang200/YJ_YongZhouHuiLang200.YJ_YongZhouHuiLang200"
EOF
}

if [[ $# -eq 0 ]]; then
  usage
  exit 1
fi

# Defaults
outfile=""
map_count=1
drops_per_map=1
sleep_action=1
sleep_map=2
single_item_id=""
item_ids_csv=""
append_path=""
start_next=""
start_last=""
end_next=""
end_last=""

# Parse CLI
while [[ $# -gt 0 ]]; do
  case "$1" in
    -f|--file)
      [[ $# -ge 2 ]] || { echo "Error: --file requires a path" >&2; exit 1; }
      outfile="$2"; shift 2;;
    --maps)
      [[ $# -ge 2 ]] || { echo "Error: --maps requires a number" >&2; exit 1; }
      map_count="$2"; shift 2;;
    --drops)
      [[ $# -ge 2 ]] || { echo "Error: --drops requires a number" >&2; exit 1; }
      drops_per_map="$2"; shift 2;;
    --sleepAction)
      [[ $# -ge 2 ]] || { echo "Error: --sleepAction requires seconds" >&2; exit 1; }
      sleep_action="$2"; shift 2;;
    --sleepMap)
      [[ $# -ge 2 ]] || { echo "Error: --sleepMap requires seconds" >&2; exit 1; }
      sleep_map="$2"; shift 2;;
    --itemId)
      [[ $# -ge 2 ]] || { echo "Error: --itemId requires a numeric id" >&2; exit 1; }
      single_item_id="$2"; shift 2;;
    --itemIds)
      [[ $# -ge 2 ]] || { echo "Error: --itemIds requires a comma-separated list" >&2; exit 1; }
      item_ids_csv="$2"; shift 2;;
    --append)
      [[ $# -ge 2 ]] || { echo "Error: --append requires a path to append.sh" >&2; exit 1; }
      append_path="$2"; shift 2;;
    --startNext)
      [[ $# -ge 2 ]] || { echo "Error: --startNext requires a scene path" >&2; exit 1; }
      start_next="$2"; shift 2;;
    --startLast)
      [[ $# -ge 2 ]] || { echo "Error: --startLast requires a scene path" >&2; exit 1; }
      start_last="$2"; shift 2;;
    --endNext)
      [[ $# -ge 2 ]] || { echo "Error: --endNext requires a scene path" >&2; exit 1; }
      end_next="$2"; shift 2;;
    --endLast)
      [[ $# -ge 2 ]] || { echo "Error: --endLast requires a scene path" >&2; exit 1; }
      end_last="$2"; shift 2;;
    -h|--help)
      usage; exit 0;;
    *)
      echo "Unknown argument: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$outfile" ]]; then
  echo "Error: output file not specified (-f/--file)" >&2
  usage
  exit 1
fi

# Resolve append.sh path
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
append_sh="${append_path:-$script_dir/append.sh}"
if [[ ! -x "$append_sh" ]]; then
  # If not executable but exists, still attempt to run with bash
  if [[ -f "$append_sh" ]]; then
    true
  else
    echo "Error: append.sh not found at '$append_sh'. Use --append to specify its location." >&2
    exit 1
  fi
fi

# Prepare item id array
IFS=',' read -r -a ids <<< "${item_ids_csv}" || true
if [[ ${#ids[@]} -eq 0 || -z "${ids[0]:-}" ]]; then
  if [[ -n "$item_ids_csv" ]]; then
    # CSV provided but parsing yielded nothing
    echo "Error: could not parse --itemIds list" >&2
    exit 1
  fi
  if [[ -n "$single_item_id" ]]; then
    ids=("$single_item_id")
  else
    ids=("10042")
  fi
fi

# Simple numeric validation
re_number='^[0-9]+$'
if ! [[ "$map_count" =~ $re_number && "$drops_per_map" =~ $re_number ]]; then
  echo "Error: --maps and --drops must be integers" >&2
  exit 1
fi

mkdir -p "$(dirname -- "$outfile")"

echo "Scenario start: maps=$map_count, drops/map=$drops_per_map, items=${ids[*]}, log=$outfile"

for ((m=1; m<=map_count; m++)); do
  echo "[Scenario] Map $m/$map_count: start"
  start_args=("-file" "$outfile" "MapStart")
  [[ -n "$start_next" ]] && start_args+=("--next" "$start_next")
  [[ -n "$start_last" ]] && start_args+=("--last" "$start_last")
  bash "$append_sh" "${start_args[@]}"

  # Sleep between start and first drop
  sleep "$sleep_action"

  for ((d=1; d<=drops_per_map; d++)); do
    # Cycle through ids
    idx=$(( (d-1) % ${#ids[@]} ))
    item_id="${ids[$idx]}"
    echo "[Scenario] Map $m/$map_count: drop $d/$drops_per_map (itemId=$item_id)"
    bash "$append_sh" -file "$outfile" BagMod --itemId "$item_id"
    # Sleep between drops (avoid trailing sleep before MapEnd if only one drop? keep simple)
    if [[ $d -lt $drops_per_map ]]; then
      sleep "$sleep_action"
    fi
  done

  # Sleep before ending the map
  sleep "$sleep_action"

  end_args=("-file" "$outfile" "MapEnd")
  [[ -n "$end_next" ]] && end_args+=("--next" "$end_next")
  [[ -n "$end_last" ]] && end_args+=("--last" "$end_last")
  bash "$append_sh" "${end_args[@]}"

  # Sleep between maps (skip after last)
  if [[ $m -lt $map_count ]]; then
    sleep "$sleep_map"
  fi

echo "[Scenario] Map $m/$map_count: end"

done

echo "Scenario complete. Log written to $outfile"
