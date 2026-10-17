/**
 * 打包前置：把 frontend 以桌面专用配置构建到 desktop/renderer/。
 * 差异点：base=./（file:// 必须相对）+ 禁用 manualChunks（见 vite.desktop.config.js）。
 * 不触碰 frontend/dist（Web 部署口径不变）。
 */
const { execSync } = require('node:child_process');
const path = require('node:path');
const fs = require('node:fs');

const frontendDir = path.resolve(__dirname, '../frontend');
const outDir = path.resolve(__dirname, 'renderer');

if (!fs.existsSync(path.join(frontendDir, 'node_modules'))) {
  console.error('错误：frontend/node_modules 不存在，请先在 frontend 目录 npm install');
  process.exit(1);
}

execSync('npx vite build --config vite.desktop.config.js --outDir ' + outDir + ' --emptyOutDir', {
  cwd: frontendDir,
  stdio: 'inherit',
});
console.log('renderer 就绪：' + outDir);
