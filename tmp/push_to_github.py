#!/usr/bin/env python3
import os
import sys
import json
import base64
import urllib.request
import urllib.error

TOKEN = sys.argv[1]
OWNER = "prashant-shelar-persistent"
REPO  = "kafka-example"
BRANCH = "dev"
WORKSPACE = "/Users/prashant_shelar/git/JavaMigration/kafka-example"

EXCLUDE_DIRS  = {".git", ".gradle", "build", ".idea", ".sasva", "Users", "target", "out", "bin"}
EXCLUDE_FILES = {".DS_Store", ".classpath", ".project"}
EXCLUDE_EXTS  = {".iml", ".class", ".lock", ".bin", ".probe"}

API = f"https://api.github.com/repos/{OWNER}/{REPO}"

def api(method, path, data=None):
    url = f"{API}{path}"
    body = json.dumps(data).encode() if data else None
    req  = urllib.request.Request(url, data=body, method=method)
    req.add_header("Authorization", f"token {TOKEN}")
    req.add_header("Content-Type",  "application/json")
    req.add_header("Accept", "application/vnd.github.v3+json")
    try:
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        print(f"HTTP {e.code} on {method} {path}: {e.read().decode()[:200]}")
        return None

# Collect files
files = []
for root, dirs, filenames in os.walk(WORKSPACE):
    dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]
    for fname in filenames:
        if fname in EXCLUDE_FILES:
            continue
        ext = os.path.splitext(fname)[1]
        if ext in EXCLUDE_EXTS:
            continue
        abs_path = os.path.join(root, fname)
        rel_path = os.path.relpath(abs_path, WORKSPACE)
        files.append((rel_path, abs_path))

print(f"📦 Found {len(files)} files to push")

# Get base tree SHA from dev branch
ref_data = api("GET", f"/git/refs/heads/{BRANCH}")
if not ref_data:
    print("❌ Could not get dev branch ref"); sys.exit(1)
base_sha  = ref_data["object"]["sha"]
commit_data = api("GET", f"/git/commits/{base_sha}")
base_tree = commit_data["tree"]["sha"]
print(f"🌳 Base tree SHA: {base_tree}")

# Create blobs and build tree
tree_items = []
for i, (rel_path, abs_path) in enumerate(files):
    try:
        with open(abs_path, "rb") as f:
            raw = f.read()
        try:
            content  = raw.decode("utf-8")
            encoding = "utf-8"
        except UnicodeDecodeError:
            content  = base64.b64encode(raw).decode()
            encoding = "base64"

        blob = api("POST", "/git/blobs", {"content": content, "encoding": encoding})
        if not blob:
            print(f"  ⚠️  Skipping {rel_path} (blob creation failed)")
            continue
        tree_items.append({
            "path": rel_path,
            "mode": "100755" if rel_path in ("gradlew", "gradlew.bat") else "100644",
            "type": "blob",
            "sha":  blob["sha"]
        })
        print(f"  ✅ [{i+1}/{len(files)}] {rel_path}")
    except Exception as e:
        print(f"  ⚠️  Skipping {rel_path}: {e}")

# Create tree
print(f"\n🌲 Creating tree with {len(tree_items)} items...")
new_tree = api("POST", "/git/trees", {"base_tree": base_tree, "tree": tree_items})
if not new_tree:
    print("❌ Failed to create tree"); sys.exit(1)
print(f"✅ New tree SHA: {new_tree['sha']}")

# Create commit
print("📝 Creating commit...")
new_commit = api("POST", "/git/commits", {
    "message": "feat: Add Kafka example project - client, common, and service modules",
    "tree":    new_tree["sha"],
    "parents": [base_sha]
})
if not new_commit:
    print("❌ Failed to create commit"); sys.exit(1)
print(f"✅ Commit SHA: {new_commit['sha']}")

# Update dev branch ref
print(f"🔀 Updating {BRANCH} branch...")
result = api("PATCH", f"/git/refs/heads/{BRANCH}", {
    "sha":   new_commit["sha"],
    "force": True
})
if result:
    print(f"\n🎉 Successfully pushed to {BRANCH} branch!")
    print(f"🔗 https://github.com/{OWNER}/{REPO}/tree/{BRANCH}")
else:
    print("❌ Failed to update branch ref")
