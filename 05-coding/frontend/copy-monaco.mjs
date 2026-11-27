import { cpSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const source = "node_modules/monaco-editor/min/vs";
const destination = "public/monaco/vs";
const editorBundles = readdirSync(source).filter((name) =>
  /^editor\.api-.*\.js$/.test(name),
);
if (editorBundles.length !== 1) {
  throw new Error("Monaco bundle layout changed; review its CSP nonce patch.");
}

const [editorBundle] = editorBundles;
const code = readFileSync(join(source, editorBundle), "utf8");
const styleCreation = 'document.createElement("style")';
if (code.split(styleCreation).length !== 3) {
  throw new Error("Monaco style creation changed; review its CSP nonce patch.");
}

// Monaco 0.55.1 has no style nonce hook. Set the nonce before inserting each style.
const patched = code.replaceAll(
  styleCreation,
  `Object.assign(${styleCreation},{nonce:document.querySelector("script[nonce]")?.nonce??""})`,
);
cpSync(source, destination, { recursive: true });
writeFileSync(join(destination, editorBundle), patched);
