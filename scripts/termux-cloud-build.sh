#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v gh >/dev/null; then echo 'Install the verified Termux package: pkg install gh' >&2; exit 1; fi
gh auth status
REPO="$(gh repo view --json nameWithOwner --jq .nameWithOwner)"
BRANCH="$(git branch --show-current)"
HEAD="$(git rev-parse HEAD)"
printf 'Dispatching Android APK for %s at %s (%s)\n' "$REPO" "$BRANCH" "$HEAD"
STARTED="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
gh workflow run android.yml --repo "$REPO" --ref "$BRANCH"
# Match the workflow + dispatch + branch + commit rather than accidentally downloading an unrelated older run.
RUN_ID=''
for attempt in $(seq 1 30); do
  RUN_ID="$(gh run list --repo "$REPO" --workflow android.yml --event workflow_dispatch --branch "$BRANCH" --limit 10 --json databaseId,headSha,createdAt --jq ".[] | select(.headSha == \"$HEAD\" and .createdAt >= \"$STARTED\") | .databaseId" | head -n 1)"
  [ -n "$RUN_ID" ] && break
  sleep 3
done
if [ -z "$RUN_ID" ]; then echo 'Workflow not visible yet. Run gh run list and inspect the newest matching run manually.' >&2; exit 1; fi
gh run watch "$RUN_ID" --repo "$REPO" --exit-status
DEST="apk-download/$RUN_ID"
mkdir -p "$DEST"
gh run download "$RUN_ID" --repo "$REPO" --name YESMIN-AI-debug --dir "$DEST"
echo "Downloaded: $DEST/app-debug.apk"
echo "Install after reviewing Android's prompt: termux-open $DEST/app-debug.apk"
