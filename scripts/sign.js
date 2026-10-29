/* خطّاف توقيع مخصّص لـ electron-builder: يستعمل osslsigncode النظامي (OpenSSL 3) */
const { execFileSync } = require("child_process");
const fs = require("fs");

exports.default = async function(cfg){
  const src = cfg.path;
  const pfx = process.env.CSC_LINK;
  const pass = process.env.CSC_KEY_PASSWORD || "";
  if(!pfx){ console.warn("sign: CSC_LINK not set, skipping", src); return; }
  const hash = cfg.hash || "sha256";
  const tmp = src + ".signed";
  const args = [
    "sign", "-pkcs12", pfx, "-pass", pass,
    "-h", hash, "-n", "WhatsApp Lite",
    "-in", src, "-out", tmp
  ];
  execFileSync("osslsigncode", args, { stdio: "inherit" });
  fs.renameSync(tmp, src);
  console.log("signed (" + hash + "):", src);
};
