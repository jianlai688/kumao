/**
 * 最小 OBS WebSocket v5 客户端：
 * - 连接 OBS、处理 Hello 鉴权
 * - SetStreamServiceSettings 写入 RTMP 服务器/串流密钥
 * - StartStream 开始推流
 *
 * 用法：
 *   await pushToObs({ url: 'ws://127.0.0.1:4455', password: 'xxx' }, pushUrl)
 */
function bytesToBase64(bytes) {
  let binary = ''
  for (const byte of new Uint8Array(bytes)) binary += String.fromCharCode(byte)
  return btoa(binary)
}

// Web Crypto 只在 localhost / HTTPS 下可用；局域网 IP 的 HTTP 页面没有 crypto.subtle，
// OBS WebSocket 鉴权需要 SHA-256，因此保留一份纯 JS 实现作为兜底。
function sha256Bytes(bytes) {
  const K = [
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
  ]
  const H = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19]
  const data = new Uint8Array(bytes)
  const length = data.length
  const paddedLength = Math.ceil((length + 9) / 64) * 64
  const padded = new Uint8Array(paddedLength)
  padded.set(data)
  padded[length] = 0x80
  const bitLength = length * 8
  const view = new DataView(padded.buffer)
  view.setUint32(paddedLength - 8, Math.floor(bitLength / 0x100000000))
  view.setUint32(paddedLength - 4, bitLength >>> 0)
  const words = new Uint32Array(64)

  for (let offset = 0; offset < paddedLength; offset += 64) {
    for (let i = 0; i < 16; i += 1) words[i] = view.getUint32(offset + i * 4)
    for (let i = 16; i < 64; i += 1) {
      const s0 = (words[i - 15] >>> 7 | words[i - 15] << 25) ^ (words[i - 15] >>> 18 | words[i - 15] << 14) ^ (words[i - 15] >>> 3)
      const s1 = (words[i - 2] >>> 17 | words[i - 2] << 15) ^ (words[i - 2] >>> 19 | words[i - 2] << 13) ^ (words[i - 2] >>> 10)
      words[i] = (words[i - 16] + s0 + words[i - 7] + s1) >>> 0
    }
    let [a, b, c, d, e, f, g, h] = H
    for (let i = 0; i < 64; i += 1) {
      const S1 = (e >>> 6 | e << 26) ^ (e >>> 11 | e << 21) ^ (e >>> 25 | e << 7)
      const ch = (e & f) ^ (~e & g)
      const temp1 = (h + S1 + ch + K[i] + words[i]) >>> 0
      const S0 = (a >>> 2 | a << 30) ^ (a >>> 13 | a << 19) ^ (a >>> 22 | a << 10)
      const maj = (a & b) ^ (a & c) ^ (b & c)
      const temp2 = (S0 + maj) >>> 0
      h = g
      g = f
      f = e
      e = (d + temp1) >>> 0
      d = c
      c = b
      b = a
      a = (temp1 + temp2) >>> 0
    }
    H[0] = (H[0] + a) >>> 0
    H[1] = (H[1] + b) >>> 0
    H[2] = (H[2] + c) >>> 0
    H[3] = (H[3] + d) >>> 0
    H[4] = (H[4] + e) >>> 0
    H[5] = (H[5] + f) >>> 0
    H[6] = (H[6] + g) >>> 0
    H[7] = (H[7] + h) >>> 0
  }

  const out = new Uint8Array(32)
  const outView = new DataView(out.buffer)
  H.forEach((value, index) => outView.setUint32(index * 4, value))
  return out
}

export async function sha256Base64(text) {
  const data = new TextEncoder().encode(text)
  if (globalThis.crypto?.subtle) {
    const digest = await globalThis.crypto.subtle.digest('SHA-256', data)
    return bytesToBase64(digest)
  }
  return bytesToBase64(sha256Bytes(data))
}

function splitRtmpUrl(pushUrl) {
  const url = new URL(pushUrl)
  const parts = url.pathname.split('/').filter(Boolean)
  if (parts.length < 2) throw new Error('推流地址格式不正确')
  const key = `${parts.pop()}${url.search || ''}`
  const server = `${url.protocol}//${url.host}/${parts.join('/')}`
  return { server, key }
}

export async function pushToObs(config, pushUrl, broadcastUrl = '', sourceMode = 'page') {
  const { url = 'ws://127.0.0.1:4455', password = '' } = config || {}
  if (!url) throw new Error('未配置 OBS WebSocket 地址')
  if (!pushUrl) throw new Error('推流地址为空')

  const { server, key } = splitRtmpUrl(pushUrl)
  const socket = new WebSocket(url)
  const pending = new Map()
  let seq = 0
  let identified = false
  let helloResolve
  let helloReject
  const hello = new Promise((resolve, reject) => {
    helloResolve = resolve
    helloReject = reject
  })
  const timeout = window.setTimeout(() => {
    try { socket.close() } catch (e) { /* ignore */ }
    helloReject(new Error('连接 OBS WebSocket 超时'))
  }, 5000)

  function request(requestType, requestData) {
    return new Promise((resolve, reject) => {
      const requestId = `kumao-${++seq}`
      pending.set(requestId, { resolve, reject })
      socket.send(JSON.stringify({
        op: 6,
        d: { requestType, requestId, requestData }
      }))
    })
  }

  async function prepareScene() {
    const screenMode = sourceMode === 'screen'
    if (!screenMode && !broadcastUrl) return
    const sceneName = screenMode ? '酷猫屏幕直播' : '酷猫直播'
    const pageInputName = '酷猫直播画面'
    const screenInputName = '酷猫屏幕画面'
    const scenes = await request('GetSceneList')
    const hasScene = (scenes.scenes || []).some((scene) => scene.sceneName === sceneName)
    if (!hasScene) {
      await request('CreateScene', { sceneName })
    }
    await request('SetCurrentProgramScene', { sceneName })

    const inputs = await request('GetInputList')
    let inputName = pageInputName
    if (screenMode) {
      // 复用 OBS 已有的显示器采集源，保留主播选择的那块屏幕；没有则新建主屏采集
      const monitor = (inputs.inputs || []).find((input) => input.inputKind === 'monitor_capture')
      inputName = monitor?.inputName || screenInputName
      if (!monitor) {
        await request('CreateInput', {
          sceneName,
          inputName,
          inputKind: 'monitor_capture',
          inputSettings: { monitor: 0 },
          sceneItemEnabled: true
        })
      }
    } else {
      const hasInput = (inputs.inputs || []).some((input) => input.inputName === pageInputName)
      const settings = {
        url: broadcastUrl,
        width: 1920,
        height: 1080,
        fps: 30,
        shutdownWhenNotVisible: false,
        reroute_audio: false
      }
      if (!hasInput) {
        await request('CreateInput', {
          sceneName,
          inputName: pageInputName,
          inputKind: 'browser_source',
          inputSettings: settings,
          sceneItemEnabled: true
        })
      } else {
        await request('SetInputSettings', { inputName: pageInputName, inputSettings: settings, overlay: true })
      }
    }

    let sceneItemId
    try {
      const item = await request('GetSceneItemId', { sceneName, sourceName: inputName })
      sceneItemId = item.sceneItemId
    } catch (e) {
      const created = await request('CreateSceneItem', {
        sceneName,
        sourceName: inputName,
        sceneItemEnabled: true
      })
      sceneItemId = created.sceneItemId
    }
    await request('SetSceneItemEnabled', { sceneName, sceneItemId, sceneItemEnabled: true })
    await request('SetSceneItemTransform', {
      sceneName,
      sceneItemId,
      sceneItemTransform: {
        positionX: 0,
        positionY: 0,
        boundsType: 'OBS_BOUNDS_STRETCH',
        boundsWidth: 1920,
        boundsHeight: 1080,
        alignment: 5
      }
    })
  }

  socket.onmessage = async (event) => {
    let message
    try { message = JSON.parse(event.data) } catch (e) { return }
    if (message.op === 0) {
      const auth = message.d?.authentication
      if (auth) {
        const secret = await sha256Base64(`${password}${auth.salt}`)
        const authentication = await sha256Base64(`${secret}${auth.challenge}`)
        socket.send(JSON.stringify({
          op: 1,
          d: { rpcVersion: 1, authentication }
        }))
      } else {
        socket.send(JSON.stringify({ op: 1, d: { rpcVersion: 1 } }))
      }
      return
    }
    if (message.op === 2) {
      identified = true
      helloResolve()
      return
    }
    if (message.op === 7) {
      const item = pending.get(message.d?.requestId)
      if (!item) return
      pending.delete(message.d.requestId)
      if (message.d.requestStatus?.result) item.resolve(message.d.responseData || {})
      else item.reject(new Error(message.d.requestStatus?.comment || 'OBS 请求失败'))
    }
  }

  socket.onerror = () => {
    if (!identified) helloReject(new Error('无法连接 OBS WebSocket，请确认 OBS 已开启 WebSocket 服务器'))
  }
  socket.onclose = () => {
    if (!identified) helloReject(new Error('OBS WebSocket 连接已关闭'))
    for (const item of pending.values()) item.reject(new Error('OBS WebSocket 已断开'))
    pending.clear()
  }

  try {
    await hello
    await prepareScene()
    const status = await request('GetStreamStatus')
    if (status.outputActive) {
      // 已开播时比较 OBS 当前推流目标；目标一致则直接复用，不一致则重启推流到当前直播间
      let current = null
      try { current = await request('GetStreamServiceSettings') } catch (e) { /* ignore */ }
      const settings = current?.streamServiceSettings || {}
      if (settings.server === server && settings.key === key) {
        return { alreadyStreaming: true }
      }
      await request('StopStream')
      for (let i = 0; i < 12; i += 1) {
        await new Promise((resolve) => setTimeout(resolve, 300))
        const next = await request('GetStreamStatus')
        if (!next.outputActive) break
      }
    }
    await request('SetStreamServiceSettings', {
      streamServiceType: 'rtmp_custom',
      streamServiceSettings: { server, key }
    })
    await request('StartStream')
    return { started: true }
  } finally {
    window.clearTimeout(timeout)
    try { socket.close() } catch (e) { /* ignore */ }
  }
}

/** 下播时停止 OBS 推流；OBS 未开启或未在推流时静默返回 */
export async function stopObsStream(config) {
  const { url = 'ws://127.0.0.1:4455', password = '' } = config || {}
  if (!url) return { stopped: false }
  const socket = new WebSocket(url)
  const pending = new Map()
  let seq = 0
  let identified = false
  let helloResolve
  let helloReject
  const hello = new Promise((resolve, reject) => {
    helloResolve = resolve
    helloReject = reject
  })
  const timeout = window.setTimeout(() => {
    try { socket.close() } catch (e) { /* ignore */ }
    helloReject(new Error('连接 OBS WebSocket 超时'))
  }, 3500)

  function request(requestType, requestData) {
    return new Promise((resolve, reject) => {
      const requestId = `kumao-stop-${++seq}`
      pending.set(requestId, { resolve, reject })
      socket.send(JSON.stringify({ op: 6, d: { requestType, requestId, requestData } }))
    })
  }

  socket.onmessage = async (event) => {
    let message
    try { message = JSON.parse(event.data) } catch (e) { return }
    if (message.op === 0) {
      const auth = message.d?.authentication
      if (auth) {
        const secret = await sha256Base64(`${password}${auth.salt}`)
        const authentication = await sha256Base64(`${secret}${auth.challenge}`)
        socket.send(JSON.stringify({ op: 1, d: { rpcVersion: 1, authentication } }))
      } else {
        socket.send(JSON.stringify({ op: 1, d: { rpcVersion: 1 } }))
      }
      return
    }
    if (message.op === 2) {
      identified = true
      helloResolve()
      return
    }
    if (message.op === 7) {
      const item = pending.get(message.d?.requestId)
      if (!item) return
      pending.delete(message.d.requestId)
      if (message.d.requestStatus?.result) item.resolve(message.d.responseData || {})
      else item.reject(new Error(message.d.requestStatus?.comment || 'OBS 请求失败'))
    }
  }

  socket.onerror = () => {
    if (!identified) helloReject(new Error('无法连接 OBS WebSocket'))
  }
  socket.onclose = () => {
    if (!identified) helloReject(new Error('OBS WebSocket 连接已关闭'))
    for (const item of pending.values()) item.reject(new Error('OBS WebSocket 已断开'))
    pending.clear()
  }

  try {
    await hello
    const status = await request('GetStreamStatus')
    if (!status.outputActive) return { stopped: false }
    await request('StopStream')
    return { stopped: true }
  } finally {
    window.clearTimeout(timeout)
    try { socket.close() } catch (e) { /* ignore */ }
  }
}
