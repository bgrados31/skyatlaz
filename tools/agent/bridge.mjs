// Direct client for the MCP Fabric HTTP bridge loaded by `./gradlew runWorld` (docs/TESTING.md).
// Lets an agent drive the dev world even without the MCP server registered:
//   node tools/agent/bridge.mjs cmd skyatlaz debug theme      (runs /skyatlaz debug theme)
//   node tools/agent/bridge.mjs chat hello
//   node tools/agent/bridge.mjs screenshot out.png
//   node tools/agent/bridge.mjs call ui.widgets '{}'
// Reads host/port/token from run/config/mcpfabric.config.json and never prints the token.
import { readFileSync, writeFileSync } from "node:fs";

const CONFIG = new URL("../../run/config/mcpfabric.config.json", import.meta.url);

async function call(method, params = {}) {
  const { host, port, token } = JSON.parse(readFileSync(CONFIG, "utf8"));
  const res = await fetch(`http://${host}:${port}/rpc`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ method, params }),
  });
  const body = await res.json();
  if (!body.ok) throw new Error(`${method} failed: ${JSON.stringify(body.error ?? body)}`);
  return body.result;
}

/** The screenshot result nests a base64 PNG; find the long string instead of depending on its exact shape. */
function findBase64(value) {
  if (typeof value === "string") return value.length > 1000 ? value : undefined;
  if (value && typeof value === "object") {
    for (const child of Object.values(value)) {
      const found = findBase64(child);
      if (found) return found;
    }
  }
  return undefined;
}

const [command, ...args] = process.argv.slice(2);
switch (command) {
  case "chat":
    console.log(JSON.stringify(await call("chat.send", { message: args.join(" ") })));
    break;
  // Adds the leading "/" itself: Git Bash on Windows rewrites "/skyatlaz" into a filesystem path.
  case "cmd":
    console.log(JSON.stringify(await call("chat.send", { message: "/" + args.join(" ") })));
    break;
  case "screenshot": {
    const out = args[0] ?? "screenshot.png";
    const png = findBase64(await call("vision.screenshot"));
    writeFileSync(out, Buffer.from(png.replace(/^data:image\/png;base64,/, ""), "base64"));
    console.log(out);
    break;
  }
  case "call":
    console.log(JSON.stringify(await call(args[0], JSON.parse(args[1] ?? "{}")), null, 1));
    break;
  default:
    console.log("usage: bridge.mjs cmd <command without /> | chat <text> | screenshot [out.png] | call <method> [json]");
    process.exitCode = 1;
}
