// Stop hook.
// If sources/build files changed since the last green check, runs `gradlew check` (format, compile,
// unit + architecture tests). A failure blocks the stop so the model must fix it first.
// Also reminds to update docs/PROGRESS.md (handoff file) when code changed.
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";

const input = JSON.parse(readFileSync(0, "utf8"));
const projectDir = process.env.CLAUDE_PROJECT_DIR ?? input.cwd;
const stampFile = join(projectDir, ".gradle", "claude-last-green-check");
const WATCHED = ["src", "config", "build.gradle.kts", "settings.gradle.kts", "gradle.properties"];

/** Hash of paths + mtimes of watched files: cheap change detection without running Gradle. */
function fingerprint() {
  const hash = createHash("sha1");
  const visit = (path) => {
    if (!existsSync(path)) return;
    const stat = statSync(path);
    if (stat.isDirectory()) readdirSync(path).sort().forEach((name) => visit(join(path, name)));
    else hash.update(`${path}:${stat.mtimeMs}\n`);
  };
  WATCHED.forEach((p) => visit(join(projectDir, p)));
  return hash.digest("hex");
}

/** Gradle needs a JDK; fall back to IntelliJ-downloaded JDKs when JAVA_HOME is not set. */
function javaHome() {
  if (process.env.JAVA_HOME) return process.env.JAVA_HOME;
  const jdks = join(homedir(), ".jdks");
  if (!existsSync(jdks)) return undefined;
  const match = readdirSync(jdks).sort().reverse().find((name) => /-25(\.|$)/.test(name));
  return match ? join(jdks, match) : undefined;
}

const block = (reason) => {
  process.stdout.write(JSON.stringify({ decision: "block", reason }));
  process.exit(0);
};

const current = fingerprint();
const lastGreen = existsSync(stampFile) ? readFileSync(stampFile, "utf8") : "";
if (current === lastGreen) process.exit(0);

const isWindows = process.platform === "win32";
const result = spawnSync(isWindows ? `"${join(projectDir, "gradlew.bat")}"` : "./gradlew",["check", "-q", "--console=plain"], {
  cwd: projectDir,
  env: { ...process.env, JAVA_HOME: javaHome() ?? "" },
  encoding: "utf8",
  shell: isWindows,
  timeout: 540_000,
});

if (result.status !== 0) {
  const output = `${result.stdout ?? ""}\n${result.stderr ?? ""}`
    .split("\n")
    .filter((l) => l.trim() && !/deprecat|incubating|Gradle 10|warning-mode/i.test(l))
    .slice(-40)
    .join("\n");
  if (input.stop_hook_active) {
    // Second failed attempt in a row: let the session end but make the failure visible to the user.
    process.stdout.write(JSON.stringify({ systemMessage: `gradlew check is still failing:\n${output}` }));
    process.exit(0);
  }
  block(`gradlew check failed. Fix it before finishing (run ./gradlew spotlessApply for formatting).\n${output}`);
}

writeFileSync(stampFile, current);

const status = spawnSync("git", ["status", "--porcelain"], { cwd: projectDir, encoding: "utf8" }).stdout ?? "";
const codeChanged = /^.. (src|config)\//m.test(status);
const progressUpdated = /^.. docs\/PROGRESS\.md$/m.test(status);
if (codeChanged && !progressUpdated && !input.stop_hook_active) {
  block("Code changed but docs/PROGRESS.md was not updated. Update the handoff (done / next / decisions) before finishing.");
}
