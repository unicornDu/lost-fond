/* ================================================================
 * 渲染与交互逻辑
 * 内容全部来自 content.js 的 SITE_CONTENT（占位内容集中管理）
 * ================================================================ */
(function () {
  "use strict";
  const C = SITE_CONTENT;
  const $ = (s, el) => (el || document).querySelector(s);
  const $$ = (s, el) => Array.from((el || document).querySelectorAll(s));

  /* ---------------- 页头 ---------------- */
  function renderHeader() {
    $(".header-logo a").innerHTML =
      "<strong>" + C.brand.strong + "</strong> " + C.brand.light;
    $(".header-logo a").href = C.brand.href;

    const navHtml = C.nav
      .map(function (n) {
        const current =
          location.pathname.endsWith(n.href) ? ' aria-current="page"' : "";
        return '<li><a href="' + n.href + '"' + current + ">" + n.label + "</a></li>";
      })
      .join("");
    $(".menu-desktop-links").innerHTML = navHtml;
    $(".menu-overlay-links").innerHTML = navHtml;

    const langHtml = C.languages
      .map(function (l) {
        return (
          '<a href="' + l.href + '"' +
          (l.current ? ' aria-current="true"' : "") +
          ">" + l.code + "</a>"
        );
      })
      .join("");
    $$(".languages").forEach(function (el) { el.innerHTML = langHtml; });
  }

  /* ---------------- 移动端菜单开合 ---------------- */
  function initMenu() {
    const btn = $(".menu-toggle");
    btn.addEventListener("click", function () {
      const open = document.body.classList.toggle("menu-open");
      btn.setAttribute("aria-expanded", String(open));
    });
    // 点击覆盖层内链接后收起菜单
    $$(".menu-overlay a").forEach(function (a) {
      a.addEventListener("click", function () {
        document.body.classList.remove("menu-open");
        btn.setAttribute("aria-expanded", "false");
      });
    });
  }

  /* ---------------- 首页：全屏轮播 ---------------- */
  function renderHome() {
    const wrap = $(".home-banners .swiper-wrapper");
    if (!wrap) return;

    // 第一屏：介绍页
    let html =
      '<section class="banner swiper-slide">' +
      '<div class="media" data-swiper-parallax="60%" style="background-image:url(\'' +
      ph(1408, 792, "首屏背景占位", "dark") + "')\"></div>" +
      '<h1 class="banner-title" data-swiper-parallax="40%"><span>' +
      C.home.intro.title +
      '<span class="banner-sub">' + C.home.intro.subtitle + "</span>" +
      "</span></h1></section>";

    // 后续每屏一个项目，整屏可点击
    C.home.banners.forEach(function (b) {
      html +=
        '<a class="banner swiper-slide" href="' + b.href + '">' +
        '<div class="media" data-swiper-parallax="60%" style="background-image:url(\'' +
        b.img + "')\"></div>" +
        '<h2 class="banner-title" data-swiper-parallax="40%"><span>' +
        b.title + "</span></h2></a>";
    });
    wrap.innerHTML = html;

    /* global Swiper */
    new Swiper(".home-banners", {
      direction: "vertical",
      speed: 800,
      parallax: true,
      mousewheel: { releaseOnEdges: true },
      keyboard: { enabled: true },
      touchReleaseOnEdges: true
    });
  }

  /* ---------------- 列表页：作品网格（双色调悬停） ---------------- */
  function renderWorks(key) {
    const grid = $('[data-works="' + key + '"]');
    if (!grid) return;
    const data = C[key];
    document.title = data.heading + " | " + C.brand.strong + " " + C.brand.light;
    $(".sr-only h1").textContent = data.heading;
    $(".sr-only p").textContent = data.intro;

    grid.innerHTML = data.items
      .map(function (item, i) {
        const label = item.title;
        return (
          '<li class="work"><a class="work-link" href="#">' +
          '<div class="media cover parallax duotone" style="--ratio:3/4">' +
          '<div class="img img-gray" style="background-image:url(\'' +
          ph(600, 800, label, "grey") + "')\"></div>" +
          '<div class="img img-color" style="background-image:url(\'' +
          ph(600, 800, label, "color") + "')\"></div>" +
          "</div>" +
          '<h2 class="work-title">' + item.title + "</h2>" +
          "</a></li>"
        );
      })
      .join("");
  }

  /* ---------------- 工作室页 ---------------- */
  function renderOffice() {
    const el = $(".office");
    if (!el) return;
    $(".office-title").textContent = C.office.title;
    $(".office-body").innerHTML = C.office.paragraphs
      .map(function (p) { return "<p>" + p + "</p>"; })
      .join("");
    $(".office-figure .media").style.backgroundImage =
      "url('" + C.office.image + "')";
    $(".office-figure figcaption").textContent = C.office.caption;
  }

  /* ---------------- 页脚 ---------------- */
  function renderFooter() {
    const f = C.footer;
    $(".footer .contacts").innerHTML =
      '<a href="mailto:' + f.email + '">' + f.email + "</a>" +
      '<a href="tel:' + f.phone.replace(/\s/g, "") + '">' + f.phone + "</a>";
    $(".footer .address a").innerHTML =
      f.address.map(function (l) { return "<span>" + l + "</span>"; }).join("");
    $(".footer .address a").href = f.addressHref;
    $(".footer .scroll-hint").textContent = f.scrollHint;
  }

  /* ---------------- 页脚滚动淡入 ---------------- */
  function initFooterReveal() {
    const footer = $(".footer");
    new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (e.isIntersecting) footer.classList.add("is-visible");
      });
    }, { threshold: 0.15 }).observe(footer);
  }

  /* ---------------- 卡片滚动视差（轻微） ---------------- */
  function initParallax() {
    const items = $$(".parallax .img");
    if (!items.length) return;
    let ticking = false;
    function update() {
      ticking = false;
      const vh = window.innerHeight;
      items.forEach(function (img) {
        const r = img.parentElement.getBoundingClientRect();
        if (r.bottom < 0 || r.top > vh) return;
        const p = (r.top + r.height / 2 - vh / 2) / vh;   // -0.5 ~ 0.5
        img.style.transform =
          "scale(1.05) translateY(" + (p * -5).toFixed(2) + "%)";
      });
    }
    window.addEventListener("scroll", function () {
      if (!ticking) { ticking = true; requestAnimationFrame(update); }
    }, { passive: true });
    update();
  }

  /* ---------------- 启动 ---------------- */
  renderHeader();
  renderFooter();
  initMenu();
  initFooterReveal();

  const page = document.body.dataset.page;
  if (page === "home") renderHome();
  if (page === "projects") renderWorks("projects");
  if (page === "archive") renderWorks("archive");
  if (page === "office") renderOffice();
  if (page !== "home") initParallax();
})();
