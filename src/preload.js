const { contextBridge, ipcRenderer } = require("electron");

/* index.html يبحث عن window.App.exitApp عند الضغط على "نعم" في نافذة الخروج */
contextBridge.exposeInMainWorld("App", {
  exitApp: () => ipcRenderer.send("app:exit")
});
