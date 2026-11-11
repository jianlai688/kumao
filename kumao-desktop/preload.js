const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('kumao', {
  isDesktop: true,
  platform: () => ipcRenderer.invoke('kumao:platform'),
  version: () => ipcRenderer.invoke('kumao:version'),
  minimize: () => ipcRenderer.send('kumao:minimize'),
  close: () => ipcRenderer.send('kumao:close'),
  device: 'DESKTOP'
})
