const { app, BrowserWindow, Menu, protocol, session, ipcMain, shell } = require("electron");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const SCHEME = "app", HOST = "whatsapp-lite";
/* يستبدلها scripts/prepare.js بمفتاح عشوائي جديد في كل بناء */
const KEY_PARTS = "__KEY_PARTS__";

protocol.registerSchemesAsPrivileged([{ scheme: SCHEME, privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true, stream: true } }]);

if(!app.requestSingleInstanceLock()) app.quit();

/* الواجهة مخزّنة مشفّرة (AES-256-GCM) داخل الحزمة ولا تُفك إلا في الذاكرة */
function loadPage(){
  const key = crypto.createHash("sha256").update(KEY_PARTS.map(p => Buffer.from(p, "base64").toString("binary")).join("")).digest();
  const buf = fs.readFileSync(path.join(__dirname, "app.dat"));
  const d = crypto.createDecipheriv("aes-256-gcm", key, buf.subarray(0, 12));
  d.setAuthTag(buf.subarray(12, 28));
  return Buffer.concat([d.update(buf.subarray(28)), d.final()]);
}

let win = null;
const back = () => { if(win && !win.isDestroyed()) win.webContents.executeJavaScript("window.__back && window.__back()").catch(() => {}); };

function createWindow(){
  win = new BrowserWindow({
    width: 1100, height: 760, minWidth: 360, minHeight: 520,
    backgroundColor: "#111b21", title: "WhatsApp Lite", show: false, autoHideMenuBar: true,
    icon: path.join(__dirname, "icon.png"),
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true, nodeIntegration: false, sandbox: true,
      devTools: false, spellcheck: false
    }
  });
  win.once("ready-to-show", () => win.show());

  /* زر الرجوع: زر الفأرة الجانبي، Alt+← ، Esc ، Backspace خارج حقول الكتابة */
  win.on("app-command", (e, cmd) => { if(cmd === "browser-backward"){ e.preventDefault(); back(); } });
  win.webContents.on("before-input-event", (e, i) => {
    if(i.type !== "keyDown") return;
    const k = i.key, ctl = i.control || i.meta;
    if(k === "F12" || (ctl && i.shift && /^[ijc]$/i.test(k)) || (ctl && /^u$/i.test(k))){ e.preventDefault(); return; }
    if(k === "BrowserBack" || (i.alt && k === "ArrowLeft") || k === "Escape"){ e.preventDefault(); back(); }
  });

  /* لا تنقّل خارج التطبيق: الروابط الخارجية تُفتح في المتصفح */
  win.webContents.setWindowOpenHandler(({ url }) => { if(/^https?:/.test(url)) shell.openExternal(url); return { action: "deny" }; });
  win.webContents.on("will-navigate", (e, url) => { if(!url.startsWith(`${SCHEME}://${HOST}/`)){ e.preventDefault(); if(/^https?:/.test(url)) shell.openExternal(url); } });
  win.webContents.on("context-menu", e => e.preventDefault());

  win.loadURL(`${SCHEME}://${HOST}/index.html`);
}

ipcMain.on("app:exit", () => app.quit());

app.on("second-instance", () => { if(win){ if(win.isMinimized()) win.restore(); win.focus(); } });

app.whenReady().then(() => {
  Menu.setApplicationMenu(null);
  const page = loadPage();
  protocol.handle(SCHEME, req => {
    const u = new URL(req.url);
    if(u.host === HOST && (u.pathname === "/" || u.pathname === "/index.html"))
      return new Response(page, { headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-store" } });
    return new Response("Not found", { status: 404 });
  });
  const ok = new Set(["media", "notifications", "clipboard-sanitized-write", "clipboard-read", "fullscreen", "display-capture"]);
  session.defaultSession.setPermissionRequestHandler((wc, p, cb) => cb(ok.has(p)));
  session.defaultSession.setPermissionCheckHandler((wc, p) => ok.has(p));
  /* مشاركة الشاشة في المكالمات */
  session.defaultSession.setDisplayMediaRequestHandler((req, cb) => {
    require("electron").desktopCapturer.getSources({ types: ["screen"] }).then(s => cb(s[0] ? { video: s[0] } : {})).catch(() => cb({}));
  });
  createWindow();
});

app.on("window-all-closed", () => app.quit());
