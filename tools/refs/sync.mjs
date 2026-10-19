// Clones (shallow) or updates reference SkyBlock mods into references/ and builds a compact,
// grep-friendly index per repo: references/<name>/INDEX.md  ->  "path | declared types".
// references/ is git-ignored and read-only study material: never copy code from it (license).
// Usage: node tools/refs/sync.mjs [name...]
import { spawnSync } from "node:child_process";
import { existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "references");

/** Reference repos. `code: false` = data repo: only list directories, do not index sources. */
const REPOS = [
  { name: "skyblocker", url: "https://github.com/SkyblockerMod/Skyblocker.git", code: true },
  { name: "skyhanni", url: "https://github.com/hannibal002/SkyHanni.git", code: true },
  { name: "firmament", url: "https://github.com/nea89o/Firmament.git", code: true },
  { name: "hypixel-mod-api", url: "https://github.com/HypixelDev/ModAPI.git", code: true },
  { name: "neu-repo", url: "https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO.git", code: false },
];

const SOURCE_EXT = /\.(kt|java)$/;
const DECLARATION = /^\s*(?:(?:public|private|internal|protected|abstract|open|sealed|data|enum|annotation|final|static|inner|value)\s+)*(class|object|interface|record|enum)\s+([A-Z]\w*)/gm;

const git = (args, cwd) => spawnSync("git", args, { cwd, stdio: "inherit" }).status === 0;

function sync({ name, url }) {
  const dir = join(ROOT, name);
  if (existsSync(join(dir, ".git"))) {
    console.log(`Updating ${name}...`);
    return git(["fetch", "--depth", "1", "origin"], dir) && git(["reset", "--hard", "FETCH_HEAD"], dir);
  }
  console.log(`Cloning ${name}...`);
  return git(["clone", "--depth", "1", "--single-branch", url, dir], ROOT);
}

function walk(dir, files = []) {
  for (const entry of readdirSync(dir)) {
    if (entry === ".git" || entry === "build" || entry === ".gradle") continue;
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) walk(path, files);
    else files.push(path);
  }
  return files;
}

function buildIndex({ name, code }) {
  const dir = join(ROOT, name);
  const files = walk(dir);
  const commit = spawnSync("git", ["rev-parse", "--short", "HEAD"], { cwd: dir, encoding: "utf8" }).stdout.trim();
  const lines = [`# ${name} index (commit ${commit})`, "", "Format: `path | declared types`. Grep this file, then open only the lines you need.", ""];

  if (code) {
    for (const file of files.filter((f) => SOURCE_EXT.test(f))) {
      const types = [...readFileSync(file, "utf8").matchAll(DECLARATION)].map((m) => m[2]);
      lines.push(`${relative(dir, file).replaceAll("\\", "/")} | ${[...new Set(types)].join(", ")}`);
    }
  } else {
    const counts = new Map();
    for (const file of files) {
      const folder = relative(dir, dirname(file)).replaceAll("\\", "/") || ".";
      counts.set(folder, (counts.get(folder) ?? 0) + 1);
    }
    [...counts].sort().forEach(([folder, count]) => lines.push(`${folder}/ | ${count} files`));
  }
  writeFileSync(join(dir, "INDEX.md"), lines.join("\n") + "\n");
  console.log(`Indexed ${name}: ${lines.length - 4} entries`);
}

mkdirSync(ROOT, { recursive: true });
const wanted = process.argv.slice(2);
for (const repo of REPOS.filter((r) => wanted.length === 0 || wanted.includes(r.name))) {
  if (sync(repo)) buildIndex(repo);
  else console.error(`Failed to sync ${repo.name}`);
}
