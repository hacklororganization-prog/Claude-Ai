/* يجهّز مجلد dist: يشفّر app/index.html ويموّه كود Electron */
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const Obf = require("javascript-obfuscator");

const root = path.join(__dirname, ".."), dist = path.join(root, "dist");
fs.rmSync(dist, { recursive: true, force: true });
fs.mkdirSync(dist);

/* مفتاح عشوائي في كل بناء، مقسّم إلى أجزاء داخل main.js المموّه */
const parts = Array.from({ length: 6 }, () => crypto.randomBytes(16).toString("base64"));
const key = crypto.createHash("sha256").update(parts.map(p => Buffer.from(p, "base64").toString("binary")).join("")).digest();
const iv = crypto.randomBytes(12), c = crypto.createCipheriv("aes-256-gcm", key, iv);
const enc = Buffer.concat([c.update(fs.readFileSync(path.join(root, "app", "index.html"))), c.final()]);
fs.writeFileSync(path.join(dist, "app.dat"), Buffer.concat([iv, c.getAuthTag(), enc]));

const obf = src => Obf.obfuscate(src, {
  target: "node", compact: true, stringArray: true, stringArrayEncoding: ["rc4"], stringArrayThreshold: 1,
  controlFlowFlattening: true, deadCodeInjection: false, identifierNamesGenerator: "hexadecimal", selfDefending: false
}).getObfuscatedCode();

const main = fs.readFileSync(path.join(root, "src", "main.js"), "utf8").replace('"__KEY_PARTS__"', JSON.stringify(parts));
fs.writeFileSync(path.join(dist, "main.js"), obf(main));
fs.writeFileSync(path.join(dist, "preload.js"), obf(fs.readFileSync(path.join(root, "src", "preload.js"), "utf8")));
fs.copyFileSync(path.join(root, "build", "icon.png"), path.join(dist, "icon.png"));
console.log("dist ready");
