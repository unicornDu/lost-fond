const loginApi = window.shiguangLogin

const el = (id) => document.getElementById(id)
const welcomePane = el('pane-welcome')
const formPane = el('pane-form')
const enterBtn = el('enter-login')
const submitBtn = el('login-submit')
const accountInput = el('account')
const passwordInput = el('password')
const rememberInput = el('remember')
const errorEl = el('form-error')
const helpModal = el('help-modal')
const stateEls = [el('service-state'), el('service-state-form')]

let backend = { state: 'starting', version: '' }
let busy = false
let lastFocus = null

function stateText(state) {
  if (state === 'restoring') return '正在恢复登录状态…'
  if (state === 'ready') return '本地服务已就绪'
  if (state === 'error') return '本地服务启动失败'
  return '正在启动本地服务…'
}

function applyUiState() {
  const ready = backend.state === 'ready'
  for (const node of stateEls) {
    if (!node) continue
    node.className = `service-state ${backend.state || 'starting'}`
    const text = node.querySelector('span:last-child')
    if (text) text.textContent = stateText(backend.state)
  }
  enterBtn.disabled = !ready
  submitBtn.disabled = !ready || busy
}

function setBackendState(next) {
  backend = { ...backend, ...(next || {}) }
  applyUiState()
}

function showPane(name) {
  const form = name === 'form'
  welcomePane.classList.toggle('on', !form)
  formPane.classList.toggle('on', form)
  if (form) {
    errorEl.textContent = ''
    setTimeout(() => accountInput.focus(), 80)
  } else {
    enterBtn.focus()
  }
}

function showError(message) {
  errorEl.textContent = message || ''
}

function setBusy(value, label = '登录', success = false) {
  busy = value
  accountInput.disabled = value
  passwordInput.disabled = value
  rememberInput.disabled = value
  submitBtn.textContent = value ? label : '登录'
  submitBtn.classList.toggle('is-success', success)
  applyUiState()
}

async function submitLogin(event) {
  event.preventDefault()
  if (busy || backend.state !== 'ready') return
  showError('')
  const account = accountInput.value.trim()
  const password = passwordInput.value
  if (!/^\d{6,12}$/.test(account)) {
    showError('请输入 6–12 位学号')
    accountInput.focus()
    return
  }
  if (password.length < 6) {
    showError('密码至少 6 位')
    passwordInput.focus()
    return
  }
  busy = true
  setBusy(true, '正在登录…')
  try {
    const result = await loginApi.login({
      account,
      password,
      rememberMe: rememberInput.checked,
    })
    if (!result?.ok) {
      setBusy(false)
      showError(result?.message || '登录失败，请重试')
      passwordInput.focus()
      passwordInput.select()
      return
    }
    setBusy(true, '登录成功，正在进入…', true)
  } catch (e) {
    setBusy(false)
    showError('登录失败，请重试')
  }
}

function openHelp(focusTarget) {
  lastFocus = focusTarget || document.activeElement
  helpModal.hidden = false
  el('help-close').focus()
}

function closeHelp() {
  helpModal.hidden = true
  if (lastFocus && typeof lastFocus.focus === 'function') lastFocus.focus()
}

function fillDemo(account, password) {
  accountInput.value = account
  passwordInput.value = password
  closeHelp()
  showPane('form')
  setTimeout(() => submitBtn.focus(), 90)
}

if (!loginApi) {
  el('state-text').textContent = '桌面登录接口未加载'
} else {
  loginApi.onState(setBackendState)
  loginApi.getState().then(setBackendState).catch(() => {
    setBackendState({ state: 'error' })
  })

  el('win-min').addEventListener('click', () => loginApi.minimize())
  el('win-close').addEventListener('click', () => loginApi.close())

  enterBtn.addEventListener('click', () => {
    if (backend.state !== 'ready') return
    showPane('form')
  })
  el('back-welcome').addEventListener('click', () => showPane('welcome'))

  el('pwd-toggle').addEventListener('click', () => {
    const show = passwordInput.type === 'password'
    passwordInput.type = show ? 'text' : 'password'
    const button = el('pwd-toggle')
    button.classList.toggle('showing', show)
    button.setAttribute('aria-label', show ? '隐藏密码' : '显示密码')
    button.title = show ? '隐藏密码' : '显示密码'
    passwordInput.focus()
  })

  accountInput.addEventListener('input', () => {
    const cleaned = accountInput.value.replace(/\D/g, '')
    if (cleaned !== accountInput.value) accountInput.value = cleaned
  })

  el('login-form').addEventListener('submit', submitLogin)

  for (const id of ['help-open', 'help-open-form', 'demo-open']) {
    const button = el(id)
    if (button) button.addEventListener('click', () => openHelp(button))
  }
  el('help-close').addEventListener('click', closeHelp)
  helpModal.addEventListener('click', (event) => {
    if (event.target === helpModal) closeHelp()
  })
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !helpModal.hidden) closeHelp()
  })

  el('fill-demo-user').addEventListener('click', () => fillDemo('20240001', 'Demo@2026'))
  el('fill-demo-admin').addEventListener('click', () => fillDemo('10000001', 'Admin@2026'))

  for (const id of ['service-info', 'service-info-form']) {
    const button = el(id)
    if (button) button.addEventListener('click', () => loginApi.showServiceInfo())
  }
}

applyUiState()
