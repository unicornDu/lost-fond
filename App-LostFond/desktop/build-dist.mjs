import { copyFileSync, readFileSync } from 'node:fs'
import { execSync } from 'node:child_process'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const root = resolve(here, '..')
const pkg = JSON.parse(readFileSync(resolve(here, 'package.json'), 'utf8'))

execSync('npm run build:electron', { cwd: resolve(root, 'frontend'), stdio: 'inherit' })
execSync('npx electron-builder --win nsis', { cwd: here, stdio: 'inherit' })

const name = `Shiguang-Windows-Setup-${pkg.version}.exe`
copyFileSync(resolve(root, 'build', 'release', name), resolve(root, 'Softeare', name))
console.log(`copied -> Softeare/${name}`)
