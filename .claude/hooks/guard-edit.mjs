// PreToolUse hook (Edit | Write | MultiEdit).
// Blocks AI edits that break project rules before they hit disk. Pattern rules come from
// config/source-rules.json, the same file the build's ArchitectureTest reads.
// Exit code 2 = block the tool call and show stderr to the model.
import { readFileSync } from "node:fs";
import { join, relative, sep } from "node:path";

const BASE_PACKAGE_PATH = "org/tnozamas/skyatlaz/";
const SOURCE_ROOTS = ["src/main/kotlin/", "src/main/java/"];

const input = JSON.parse(readFileSync(0, "utf8"));
const projectDir = process.env.CLAUDE_PROJECT_DIR ?? input.cwd;
const toolInput = input.tool_input ?? {};
const filePath = relative(projectDir, toolInput.file_path ?? "").split(sep).join("/");

const fail = (message) => {
  process.stderr.write(`Blocked by project rules: ${message}\n`);
  process.exit(2);
};

// Reference repos are read-only study material.
if (filePath.startsWith("references/")) fail("references/ is read-only. Never edit or copy code from it.");

const sourceRoot = SOURCE_ROOTS.find((root) => filePath.startsWith(root));
const extension = filePath.split(".").pop();
if (!sourceRoot || !["kt", "java"].includes(extension)) process.exit(0);

const packagePath = filePath.slice(sourceRoot.length);
if (!packagePath.startsWith(BASE_PACKAGE_PATH)) fail(`sources must live under ${BASE_PACKAGE_PATH}`);
if (extension === "java" && !packagePath.startsWith(BASE_PACKAGE_PATH + "mixin/")) {
  fail("Java is only allowed for mixins (org.tnozamas.skyatlaz.mixin). Write Kotlin.");
}

// Only inspect the text being added.
const added = [toolInput.content, toolInput.new_string, ...(toolInput.edits ?? []).map((e) => e.new_string)]
  .filter(Boolean)
  .join("\n")
  .split("\n")
  .filter((line) => !/^\s*(\/\/|\*|\/\*)/.test(line));

const { rules } = JSON.parse(readFileSync(join(projectDir, "config/source-rules.json"), "utf8"));
const subPackage = packagePath.slice(BASE_PACKAGE_PATH.length);
const problems = [];
for (const rule of rules) {
  if (!rule.extensions.includes(extension)) continue;
  if (rule.allowedIn.some((pkg) => subPackage.startsWith(pkg + "/"))) continue;
  const regex = new RegExp(rule.pattern);
  const hits = added.filter((line) => regex.test(line));
  if (hits.length) problems.push(`[${rule.id}] ${rule.message}\n    ${hits.map((h) => h.trim()).join("\n    ")}`);
}

if (problems.length) fail(`${filePath}\n  ${problems.join("\n  ")}`);
