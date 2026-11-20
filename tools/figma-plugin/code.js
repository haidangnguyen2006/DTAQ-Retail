// Plugin Figma: dựng frame cho từng màn hình DTAQ từ ảnh chụp + screens.json, rồi nối vùng bấm thành
// luồng prototype (menu trái, mở/đóng hộp thoại, đăng nhập ↔ quên mật khẩu).

const GROUP_ORDER = ['Xác thực', 'Quản lý', 'Nhân viên bán hàng', 'Hộp thoại'];
const COLUMNS = 5;
const GAP = 160;
const PADDING = 120;

figma.showUI(__html__, { width: 400, height: 360, themeColors: true });

let spec = null;
const frames = {};
const containers = {};

figma.ui.onmessage = async (msg) => {
  try {
    if (msg.type === 'begin') {
      await begin(msg.spec);
      figma.ui.postMessage({ type: 'ack' });
    } else if (msg.type === 'screen') {
      addScreen(msg.id, msg.bytes);
      figma.ui.postMessage({ type: 'ack' });
    } else if (msg.type === 'finish') {
      const count = await finish();
      figma.notify('Đã dựng ' + Object.keys(frames).length + ' màn hình, ' + count + ' liên kết prototype');
      figma.ui.postMessage({ type: 'done', frames: Object.keys(frames).length, links: count });
    } else if (msg.type === 'close') {
      figma.closePlugin();
    }
  } catch (e) {
    figma.ui.postMessage({ type: 'error', message: String((e && e.message) || e) });
  }
};

/** Tạo trang mới và các khu vực (section) theo nhóm; tính sẵn vị trí lưới cho từng màn hình. */
async function begin(data) {
  spec = data;
  const page = figma.createPage();
  page.name = 'DTAQ – Giao diện (' + new Date().toLocaleDateString('vi-VN') + ')';
  await figma.setCurrentPageAsync(page);

  const cellW = Math.max.apply(null, spec.screens.map((s) => s.width));
  const cellH = Math.max.apply(null, spec.screens.map((s) => s.height));
  const groups = GROUP_ORDER.concat(spec.screens.map((s) => s.group).filter((g) => GROUP_ORDER.indexOf(g) < 0));
  let top = 0;
  for (const group of groups) {
    const items = spec.screens.filter((s) => s.group === group);
    if (items.length === 0) continue;
    const cols = Math.min(COLUMNS, items.length);
    const rows = Math.ceil(items.length / COLUMNS);
    const width = PADDING * 2 + cols * cellW + (cols - 1) * GAP;
    const height = PADDING * 2 + rows * cellH + (rows - 1) * GAP;
    let container = null;
    if (typeof figma.createSection === 'function') {
      container = figma.createSection();
      container.name = group;
      container.x = 0;
      container.y = top;
      container.resizeWithoutConstraints(width, height);
    }
    items.forEach((s, i) => {
      s._x = PADDING + (i % COLUMNS) * (cellW + GAP);
      s._y = PADDING + Math.floor(i / COLUMNS) * (cellH + GAP);
      if (!container) s._y += top;
      containers[s.id] = container;
    });
    top += height + GAP;
  }
}

/** Một màn hình = một frame kích thước logic, nền là ảnh chụp 2x (giữ nét khi phóng to). */
function addScreen(id, bytes) {
  const s = spec.screens.find((x) => x.id === id);
  if (!s) return;
  const frame = figma.createFrame();
  frame.name = s.title + '  ·  ' + s.id;
  frame.resize(s.width, s.height);
  frame.clipsContent = true;
  frame.fills = [{ type: 'IMAGE', scaleMode: 'FILL', imageHash: figma.createImage(bytes).hash }];
  const container = containers[s.id];
  if (container) container.appendChild(frame);
  frame.x = s._x;
  frame.y = s._y;
  frames[s.id] = frame;
}

/** Vùng bấm trong suốt nối tới màn hình đích; đặt điểm bắt đầu cho từng luồng. */
async function finish() {
  let links = 0;
  for (const s of spec.screens) {
    const frame = frames[s.id];
    if (!frame) continue;
    for (const h of s.hotspots) {
      const dest = frames[h.target];
      // Bỏ phần vùng bấm nằm ngoài khung (ví dụ nút đóng của hộp thoại cao hơn cửa sổ).
      const x = Math.max(0, h.x);
      const y = Math.max(0, h.y);
      const w = Math.min(s.width, h.x + h.w) - x;
      const hh = Math.min(s.height, h.y + h.h) - y;
      if (!dest || w < 4 || hh < 4) continue;
      const hotspot = figma.createRectangle();
      hotspot.name = '↪ ' + h.label;
      hotspot.resize(w, hh);
      hotspot.x = x;
      hotspot.y = y;
      hotspot.fills = [];
      frame.appendChild(hotspot);
      const reaction = {
        trigger: { type: 'ON_CLICK' },
        actions: [{
          type: 'NODE',
          destinationId: dest.id,
          navigation: 'NAVIGATE',
          transition: { type: 'DISSOLVE', easing: { type: 'EASE_OUT' }, duration: 0.2 },
          preserveScrollPosition: false
        }]
      };
      if (typeof hotspot.setReactionsAsync === 'function') {
        await hotspot.setReactionsAsync([reaction]);
      } else {
        hotspot.reactions = [reaction];
      }
      links++;
    }
  }

  const starts = [];
  if (frames['login']) starts.push({ nodeId: frames['login'].id, name: '1. Đăng nhập → Quản lý' });
  if (frames['QL-MANAGER_HOME']) starts.push({ nodeId: frames['QL-MANAGER_HOME'].id, name: '2. Quản lý' });
  if (frames['BH-SALES_HOME']) starts.push({ nodeId: frames['BH-SALES_HOME'].id, name: '3. Nhân viên bán hàng' });
  figma.currentPage.flowStartingPoints = starts;

  const nodes = figma.currentPage.children;
  if (nodes.length > 0) figma.viewport.scrollAndZoomIntoView(nodes);
  return links;
}
