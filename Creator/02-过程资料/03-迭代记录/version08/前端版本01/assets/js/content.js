/* ================================================================
 * 占位内容集中管理文件
 * ----------------------------------------------------------------
 * 本文件包含全站所有文字与图片配置，均为【占位内容】。
 * 后续替换为真实内容时，只需修改本文件：
 *   - 文字：直接改对应字符串
 *   - 图片：把 ph() 生成的占位图替换为真实图片 URL 字符串即可
 * ================================================================ */

/* ----------------------------------------------------------------
 * 占位图生成器（替换真实图片后不再需要使用）
 * tone: "dark" 深色横幅底 | "grey" 黑白版卡片 | "color" 彩色版卡片
 * -------------------------------------------------------------- */
function ph(width, height, label, tone) {
  const tones = {
    dark:  { bg: "#6b5a50", fg: "#F6F5F4", sub: "#8a7a70" },
    grey:  { bg: "#cfccc9", fg: "#594037", sub: "#bdb9b5" },
    color: { bg: "#E68762", fg: "#F6F5F4", sub: "#d5764f" }
  };
  const t = tones[tone] || tones.grey;
  const svg =
    '<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height + '">' +
    '<rect width="100%" height="100%" fill="' + t.bg + '"/>' +
    '<rect x="4%" y="4%" width="92%" height="92%" fill="none" stroke="' + t.sub + '" stroke-width="2"/>' +
    '<text x="50%" y="48%" text-anchor="middle" dominant-baseline="middle" ' +
    'font-family="sans-serif" font-size="' + Math.round(width / 18) + '" fill="' + t.fg + '">' + label + '</text>' +
    '<text x="50%" y="56%" text-anchor="middle" dominant-baseline="middle" ' +
    'font-family="sans-serif" font-size="' + Math.round(width / 34) + '" fill="' + t.sub + '">' +
    width + " × " + height + " 占位图</text>" +
    "</svg>";
  return "data:image/svg+xml," + encodeURIComponent(svg);
}

/* ============================ 全站内容 ============================ */
const SITE_CONTENT = {

  /* 左上角字标：strong 部分加粗，light 部分常规 */
  brand: { strong: "示例", light: "工作室", href: "index.html" },

  /* 导航菜单（桌面端内联显示 / 移动端收纳进全屏菜单） */
  nav: [
    { label: "项目",   href: "projects.html" },
    { label: "档案",   href: "archive.html" },
    { label: "工作室", href: "office.html" }
  ],

  /* 语言切换（占位，当前语言自动隐藏） */
  languages: [
    { code: "中", href: "#", current: true },
    { code: "EN", href: "#", current: false }
  ],

  /* ---------------- 首页：全屏轮播横幅 ---------------- */
  home: {
    /* 第一屏为站点介绍页 */
    intro: {
      title: "示例标题",
      subtitle: "这里是描述文字——一句话介绍工作室的方向与理念。"
    },
    /* 后续每屏一个项目，整屏可点击 */
    banners: [
      { title: "示例项目 一", href: "projects.html", img: ph(1408, 792, "横幅占位图 一", "dark") },
      { title: "示例项目 二", href: "projects.html", img: ph(1408, 792, "横幅占位图 二", "dark") },
      { title: "示例项目 三", href: "projects.html", img: ph(1408, 792, "横幅占位图 三", "dark") },
      { title: "示例项目 四", href: "projects.html", img: ph(1408, 792, "横幅占位图 四", "dark") },
      { title: "示例项目 五", href: "projects.html", img: ph(1408, 792, "横幅占位图 五", "dark") },
      { title: "示例项目 六", href: "projects.html", img: ph(1408, 792, "横幅占位图 六", "dark") }
    ]
  },

  /* ---------------- 项目列表页 ---------------- */
  projects: {
    heading: "项目",                       /* 仅供屏幕阅读器 */
    intro: "这里是描述文字——项目列表页的辅助说明。",
    items: [
      { title: "示例项目 一" }, { title: "示例项目 二" },
      { title: "示例项目 三" }, { title: "示例项目 四" },
      { title: "示例项目 五" }, { title: "示例项目 六" },
      { title: "示例项目 七" }, { title: "示例项目 八" }
    ]
  },

  /* ---------------- 档案页（复用同一网格样式） ---------------- */
  archive: {
    heading: "档案",
    intro: "这里是描述文字——档案页的辅助说明。",
    items: [
      { title: "档案条目 一" }, { title: "档案条目 二" },
      { title: "档案条目 三" }, { title: "档案条目 四" },
      { title: "档案条目 五" }, { title: "档案条目 六" }
    ]
  },

  /* ---------------- 工作室页（编辑排版） ---------------- */
  office: {
    title: "工作室",
    paragraphs: [
      "这里是描述文字。第一段介绍工作室的定位、服务范围与设计理念，保持简短克制，两到三句话即可。",
      "这里是描述文字。第二段可以说明工作方式、团队或过往经验。排版上正文使用较小字号与较大留白，延续整体的极简气质。"
    ],
    image: ph(1408, 792, "工作室配图占位", "grey"),
    caption: "图片说明占位文字"
  },

  /* ---------------- 页脚 ---------------- */
  footer: {
    email: "hello@example.com",
    phone: "+86 000 0000 0000",
    address: ["示例街道 00 号", "000000 示例城市"],
    addressHref: "#",
    scrollHint: "滚动探索"
  }
};
