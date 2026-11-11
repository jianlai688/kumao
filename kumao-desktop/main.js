const { app, BrowserWindow, shell, ipcMain, Tray, Menu, nativeImage } = require('electron')
const path = require('node:path')

const isDev = !app.isPackaged
const DEV_URL = 'http://127.0.0.1:5173/#/'
const PROD_URL = `file://${path.join(__dirname, 'dist/index.html')}#/`

let mainWindow = null
let tray = null

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 960,
    minHeight: 640,
    title: '酷猫音乐',
    backgroundColor: '#050506',
    autoHideMenuBar: true,
    titleBarStyle: process.platform === 'darwin' ? 'hiddenInset' : 'default',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: true
    }
  })

  const url = isDev ? DEV_URL : PROD_URL
  mainWindow.loadURL(url)

  // 桌面端标识：告知渲染进程设置 device=DESKTOP
  mainWindow.webContents.on('did-finish-load', () => {
    mainWindow.webContents.executeJavaScript(
      `localStorage.setItem('km_device', 'DESKTOP'); localStorage.setItem('km_client', 'electron');`
    )
  })

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })

  mainWindow.on('closed', () => { mainWindow = null })
}

function createTray() {
  const icon = nativeImage.createEmpty()
  tray = new Tray(icon)
  tray.setToolTip('酷猫音乐')
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: '显示主窗口', click: () => mainWindow && mainWindow.show() },
    {
      label: '播放 / 暂停', click: () => {
        if (mainWindow) mainWindow.webContents.executeJavaScript('window.__kumaoPlayerToggle && window.__kumaoPlayerToggle()')
      }
    },
    { type: 'separator' },
    { label: '退出', click: () => app.quit() }
  ]))
  tray.on('click', () => mainWindow && mainWindow.show())
}

// 单实例锁
const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore()
      mainWindow.show()
      mainWindow.focus()
    }
  })

  app.whenReady().then(() => {
    createWindow()
    if (process.platform !== 'darwin') createTray()

    app.on('activate', () => {
      if (BrowserWindow.getAllWindows().length === 0) createWindow()
    })
  })
}

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})

// 渲染进程与主进程通信（预留：全局快捷键、下载管理、桌面歌词）
ipcMain.handle('kumao:platform', () => process.platform)
ipcMain.handle('kumao:version', () => app.getVersion())
ipcMain.on('kumao:minimize', () => mainWindow && mainWindow.minimize())
ipcMain.on('kumao:close', () => mainWindow && mainWindow.close())
