import fs from "node:fs/promises";
import path from "node:path";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const rootDir = process.env.LEARN_ROOT ?? "D:/moniC/project/learn";
const topicDir = path.join(rootDir, "01-topics", "image-enhancement-restoration-course-design");
const outputDir = path.join(rootDir, "03-outputs", "image-enhancement-restoration-course-design");
const finalPptx = path.join(outputDir, "course-design3-academic-presentation.pptx");
const previewDir = process.env.PREVIEW_DIR ?? path.join(outputDir, "ppt-preview");
const layoutDir = process.env.LAYOUT_DIR ?? path.join(outputDir, "ppt-layout");

const W = 1280;
const H = 720;
const page = { left: 72, top: 58, width: 1136, height: 594 };
const C = {
  ink: "#0B2545",
  blue: "#1F4D78",
  lightBlue: "#EAF2F8",
  teal: "#0F766E",
  slate: "#334155",
  muted: "#64748B",
  gray: "#E2E8F0",
  pale: "#F8FAFC",
  gold: "#B7791F",
  white: "#FFFFFF",
};

async function writeBlob(filePath, blob) {
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, new Uint8Array(await blob.arrayBuffer()));
}

async function readImageBlob(imagePath) {
  const bytes = await fs.readFile(imagePath);
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength);
}

function addText(slide, text, position, style = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    position,
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    fontSize: style.fontSize ?? 18,
    bold: style.bold ?? false,
    color: style.color ?? C.slate,
    alignment: style.alignment ?? "left",
  };
  return shape;
}

function addSlideTitle(slide, title, kicker, index) {
  addText(slide, kicker, { left: page.left, top: 28, width: 460, height: 28 }, {
    fontSize: 16,
    bold: true,
    color: C.teal,
  });
  addText(slide, title, { left: page.left, top: 60, width: 900, height: 48 }, {
    fontSize: 37,
    bold: true,
    color: C.ink,
  });
  slide.shapes.add({
    geometry: "line",
    position: { left: page.left, top: 122, width: page.width, height: 0 },
    fill: "none",
    line: { style: "solid", fill: C.gray, width: 1 },
  });
  addFooter(slide, index);
}

function addFooter(slide, index) {
  addText(slide, "数字图像处理实践 | 图像增强与复原算法综合应用", {
    left: 72,
    top: 682,
    width: 600,
    height: 24,
  }, { fontSize: 13, color: C.muted });
  addText(slide, `李林浩 202483290054 | ${index}`, {
    left: 1010,
    top: 682,
    width: 198,
    height: 24,
  }, { fontSize: 13, color: C.muted, alignment: "right" });
}

function addPanel(slide, position, fill = C.white, line = C.gray) {
  return slide.shapes.add({
    geometry: "rect",
    position,
    fill,
    line: { style: "solid", fill: line, width: 1 },
  });
}

function addSectionLabel(slide, text, x, y, color = C.teal) {
  addText(slide, text, { left: x, top: y, width: 360, height: 30 }, {
    fontSize: 20,
    bold: true,
    color,
  });
}

async function addImage(slide, fileName, position, alt, fit = "contain") {
  const imagePath = path.join(outputDir, fileName);
  const blob = await readImageBlob(imagePath);
  return slide.images.add({
    blob,
    contentType: "image/png",
    alt,
    fit,
    position,
  });
}

function addSmallMetric(slide, label, value, x, y, color = C.blue) {
  addText(slide, label, { left: x, top: y, width: 190, height: 28 }, {
    fontSize: 18,
    bold: true,
    color: C.muted,
    alignment: "center",
  });
  addText(slide, value, { left: x, top: y + 32, width: 190, height: 48 }, {
    fontSize: 30,
    bold: true,
    color,
    alignment: "center",
  });
}

function addArrow(slide, x1, y1, x2, y2) {
  slide.shapes.add({
    geometry: "line",
    position: { left: x1, top: y1, width: x2 - x1, height: y2 - y1 },
    fill: "none",
    line: { style: "solid", fill: C.muted, width: 2 },
  });
}

function addFlowNode(slide, text, x, y, w, h, fill = C.lightBlue) {
  const node = slide.shapes.add({
    geometry: "roundRect",
    position: { left: x, top: y, width: w, height: h },
    fill,
    line: { style: "solid", fill: "#BFD7EA", width: 1 },
    borderRadius: 12,
  });
  node.text = text;
  node.text.style = { fontSize: 18, bold: true, color: C.ink, alignment: "center" };
  return node;
}

function addFormula(slide, text, x, y, w, h) {
  addPanel(slide, { left: x, top: y, width: w, height: h }, "#FBFDFF", "#BFD7EA");
  addText(slide, text, { left: x + 22, top: y + 22, width: w - 44, height: h - 44 }, {
    fontSize: 21,
    bold: false,
    color: C.ink,
  });
}

function styleTable(table, headerFill = C.ink) {
  table.styleOptions = { headerRow: true, bandedRows: true };
  table.borders.assign({ style: "solid", fill: "#CBD5E1", width: 1 });
  for (let c = 0; c < table.columns.length; c += 1) {
    const cell = table.getCell(0, c);
    cell.fill = headerFill;
    cell.text.style = { fontSize: 16, bold: true, color: C.white };
  }
  for (let r = 1; r < table.rows.length; r += 1) {
    for (let c = 0; c < table.columns.length; c += 1) {
      table.getCell(r, c).text.style = { fontSize: 16, color: C.ink };
    }
  }
}

async function buildDeck() {
  await fs.mkdir(outputDir, { recursive: true });
  await fs.mkdir(previewDir, { recursive: true });
  await fs.mkdir(layoutDir, { recursive: true });

  const deck = Presentation.create({ slideSize: { width: W, height: H } });

  // 1. Title
  {
    const slide = deck.slides.add();
    slide.background.fill = C.pale;
    slide.shapes.add({
      geometry: "rect",
      position: { left: 0, top: 0, width: 18, height: H },
      fill: C.teal,
      line: { style: "solid", fill: C.teal, width: 0 },
    });
    addText(slide, "图像增强与复原算法综合应用", {
      left: 96,
      top: 160,
      width: 940,
      height: 72,
    }, { fontSize: 54, bold: true, color: C.ink });
    addText(slide, "混合噪声抑制、维纳复原与质量评价", {
      left: 100,
      top: 248,
      width: 760,
      height: 42,
    }, { fontSize: 27, color: C.blue });
    addText(slide, "《数字图像处理实践》课程设计 3", {
      left: 100,
      top: 328,
      width: 620,
      height: 30,
    }, { fontSize: 20, color: C.slate });
    addText(slide, "李林浩 | 202483290054 | 学号尾数偶数", {
      left: 100,
      top: 370,
      width: 640,
      height: 30,
    }, { fontSize: 18, color: C.muted });
    addPanel(slide, { left: 850, top: 148, width: 280, height: 300 }, C.white, C.gray);
    addSmallMetric(slide, "Enhancement", "MSE ↓ 90.8%", 895, 196, C.teal);
    addSmallMetric(slide, "SNR", "+10.37 dB", 895, 306, C.blue);
  }

  // 2. Requirements and selected data.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "任务要求与选图规则", "01  Problem Definition", 2);
    addPanel(slide, { left: 82, top: 160, width: 510, height: 390 }, C.pale, C.gray);
    addSectionLabel(slide, "课程设计要求", 112, 188);
    addText(slide,
      "1. 设计空间域与频率域结合的图像增强算法\n2. 噪声类型：随机噪声 + 周期噪声\n3. 使用 MSE、SNR、SSIM 等指标评价\n4. 针对未知降质图像设计算法复原\n5. 形成完整科技报告与实验分析",
      { left: 112, top: 238, width: 430, height: 230 },
      { fontSize: 22, color: C.ink },
    );
    addPanel(slide, { left: 666, top: 160, width: 480, height: 390 }, "#FBFDFF", C.gray);
    addSectionLabel(slide, "本次选择", 696, 188, C.blue);
    addText(slide,
      "学号：202483290054\n尾数：4，属于偶数\n\n增强任务：第二组 dog 图像\n复原任务：图 1 模糊树林图像\n\n图 2 低照度瀑布图像保留为备选结果。",
      { left: 696, top: 238, width: 410, height: 240 },
      { fontSize: 23, color: C.ink },
    );
  }

  // 3. Degradation analysis.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "退化特征分析", "02  Observation", 3);
    await addImage(slide, "enhancement_comparison.png", {
      left: 74,
      top: 152,
      width: 690,
      height: 374,
    }, "增强任务原图、退化图、频谱和阶段结果", "contain");
    addPanel(slide, { left: 810, top: 152, width: 360, height: 374 }, C.pale, C.gray);
    addText(slide,
      "核心观察",
      { left: 840, top: 180, width: 240, height: 32 },
      { fontSize: 26, bold: true, color: C.ink },
    );
    addText(slide,
      "退化图像中存在规则条纹，说明有周期噪声。\n\n频谱中出现远离中心的成对亮点，主峰约位于中心左右 ±71、上下 ±74 像素。\n\n随机噪声在空间域呈分散分布，需要配合中值/均值滤波。",
      { left: 840, top: 232, width: 300, height: 240 },
      { fontSize: 20, color: C.slate },
    );
  }

  // 4. Enhancement algorithm.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "混合噪声图像增强算法", "03  Enhancement Method", 4);
    const xs = [92, 292, 492, 692, 892];
    const labels = ["退化图像", "频谱分析", "高斯陷波", "空间域滤波", "锐化与拉伸"];
    for (let i = 0; i < labels.length - 1; i += 1) {
      addArrow(slide, xs[i] + 158, 284, xs[i + 1] - 28, 284);
    }
    for (let i = 0; i < labels.length; i += 1) {
      addFlowNode(slide, labels[i], xs[i], 242, 150, 84, i === 2 ? "#E6F4EA" : C.lightBlue);
    }
    addFormula(slide,
      "H_k(u,v) = 1 - exp(-D_k(u,v)^2 / 2D0^2)\nH(u,v) = Π H_k(u,v),  D0 = 10",
      92,
      400,
      500,
      112,
    );
    addFormula(slide,
      "g(x,y) = f(x,y) + α · [f(x,y) - f_smooth(x,y)]\nα = 0.15",
      646,
      400,
      500,
      112,
    );
    addText(slide,
      "设计思想：先在频率域定点压制周期分量，再在空间域降低随机噪声；最后补偿平滑带来的细节损失。",
      { left: 92, top: 548, width: 1040, height: 44 },
      { fontSize: 21, color: C.slate },
    );
  }

  // 5. Notch filter details.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "频率域陷波滤波器设计", "04  Frequency-Domain Filtering", 5);
    await addImage(slide, "enhancement_spectrum_and_notch.png", {
      left: 78,
      top: 152,
      width: 714,
      height: 330,
    }, "周期噪声频谱与陷波滤波器", "contain");
    addPanel(slide, { left: 830, top: 152, width: 330, height: 330 }, C.pale, C.gray);
    addText(slide, "参数设定", { left: 858, top: 182, width: 240, height: 32 }, {
      fontSize: 26,
      bold: true,
      color: C.ink,
    });
    addText(slide,
      "陷波中心：\n(0, ±71), (±74, 0)\n\n陷波半径：D0 = 10\n\n滤波类型：高斯陷波拒绝滤波\n\n优点：过渡平滑，较少产生振铃。",
      { left: 858, top: 232, width: 270, height: 220 },
      { fontSize: 20, color: C.ink },
    );
    addText(slide,
      "频谱处理目标不是削弱所有高频，而是仅抑制周期噪声对应的离散峰值，从而尽量保留狗图像本身的边缘和纹理。",
      { left: 88, top: 518, width: 1040, height: 52 },
      { fontSize: 21, color: C.slate },
    );
  }

  // 6. Restoration model.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "图像复原模型与维纳滤波", "05  Restoration Method", 6);
    addPanel(slide, { left: 84, top: 152, width: 516, height: 380 }, C.pale, C.gray);
    addText(slide, "退化模型", { left: 118, top: 188, width: 240, height: 34 }, {
      fontSize: 28,
      bold: true,
      color: C.ink,
    });
    addFormula(slide,
      "g(x,y) = h(x,y) * f(x,y) + n(x,y)",
      118,
      252,
      410,
      88,
    );
    addText(slide,
      "图1主要表现为模糊，退化可近似为低通型点扩散函数导致的高频衰减。",
      { left: 118, top: 378, width: 410, height: 78 },
      { fontSize: 21, color: C.slate },
    );
    addPanel(slide, { left: 664, top: 152, width: 516, height: 380 }, "#FBFDFF", C.gray);
    addText(slide, "维纳复原", { left: 698, top: 188, width: 240, height: 34 }, {
      fontSize: 28,
      bold: true,
      color: C.blue,
    });
    addFormula(slide,
      "F̂(u,v) = H*(u,v) / (|H(u,v)|² + K) · G(u,v)",
      698,
      252,
      410,
      88,
    );
    addText(slide,
      "参数：Gaussian PSF size = 13, σ = 1.35, K = 0.004。维纳滤波在复原高频时抑制噪声放大，比直接逆滤波更稳定。",
      { left: 698, top: 378, width: 410, height: 102 },
      { fontSize: 21, color: C.slate },
    );
  }

  // 7. Restoration result.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "图1模糊树林图像复原结果", "06  Restoration Result", 7);
    await addImage(slide, "restoration_fig1_comparison.png", {
      left: 78,
      top: 150,
      width: 718,
      height: 314,
    }, "图1模糊树林复原前后和指标", "contain");
    addPanel(slide, { left: 834, top: 150, width: 332, height: 314 }, C.pale, C.gray);
    addText(slide, "现象解释", { left: 862, top: 180, width: 240, height: 32 }, {
      fontSize: 26,
      bold: true,
      color: C.ink,
    });
    addText(slide,
      "复原后树林边缘更锐利，纹理层次增强。\n\n亮度分量复原后再替换回彩色图像，避免直接对 RGB 三通道分别复原导致颜色偏移。\n\n由于没有清晰参考图，采用无参考指标评价。",
      { left: 862, top: 228, width: 270, height: 196 },
      { fontSize: 20, color: C.slate },
    );
    addText(slide,
      "关键提升：平均梯度 0.03489 → 0.11841；拉普拉斯方差 0.01926 → 0.12463。",
      { left: 90, top: 512, width: 1020, height: 40 },
      { fontSize: 24, bold: true, color: C.blue },
    );
  }

  // 8. Quantitative enhancement evaluation.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "增强任务质量评价", "07  Quantitative Evaluation", 8);
    const table = slide.tables.add({
      rows: 3,
      columns: 6,
      left: 82,
      top: 160,
      width: 720,
      height: 142,
      values: [
        ["阶段", "MSE", "RMSE", "SNR/dB", "PSNR/dB", "SSIM"],
        ["处理前", "0.071191", "0.266816", "4.664", "11.476", "0.1039"],
        ["处理后", "0.006541", "0.080877", "15.032", "21.844", "0.6558"],
      ],
    });
    styleTable(table, C.ink);
    slide.charts.add("bar", {
      position: { left: 850, top: 160, width: 300, height: 250 },
      categories: ["SNR", "PSNR"],
      series: [
        { name: "处理前", values: [4.664, 11.476], fill: "#94A3B8" },
        { name: "处理后", values: [15.032, 21.844], fill: C.teal },
      ],
      hasLegend: true,
      legend: { position: "bottom", overlay: false, textStyle: { fontSize: 13, fill: C.slate } },
      barOptions: { direction: "column", grouping: "clustered", gapWidth: 70 },
      yAxis: { majorGridlines: { style: "solid", fill: "#E2E8F0", width: 1 }, textStyle: { fontSize: 12, fill: C.muted } },
      xAxis: { textStyle: { fontSize: 13, fill: C.slate } },
      dataLabels: { showValue: true, position: "outEnd", textStyle: { fontSize: 12, fill: C.ink, bold: true } },
    });
    addPanel(slide, { left: 86, top: 362, width: 1044, height: 150 }, C.pale, C.gray);
    addText(slide,
      "指标解读",
      { left: 118, top: 392, width: 170, height: 32 },
      { fontSize: 26, bold: true, color: C.ink },
    );
    addText(slide,
      "MSE 下降约 90.8%，说明像素误差显著降低；SNR 与 PSNR 均提升约 10 dB，说明噪声能量被有效抑制；SSIM 提升表明结构信息恢复明显。",
      { left: 300, top: 392, width: 760, height: 76 },
      { fontSize: 21, color: C.slate },
    );
  }

  // 9. Restoration metrics.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "复原任务无参考评价", "08  No-Reference Evaluation", 9);
    const table = slide.tables.add({
      rows: 3,
      columns: 5,
      left: 82,
      top: 158,
      width: 625,
      height: 142,
      values: [
        ["阶段", "信息熵", "标准差", "平均梯度", "拉普拉斯方差"],
        ["复原前", "7.320", "0.176", "0.03489", "0.01926"],
        ["复原后", "7.607", "0.228", "0.11841", "0.12463"],
      ],
    });
    styleTable(table, C.blue);
    slide.charts.add("bar", {
      position: { left: 765, top: 146, width: 390, height: 282 },
      categories: ["平均梯度", "拉普拉斯方差"],
      series: [
        { name: "复原前", values: [0.03489, 0.01926], fill: "#94A3B8" },
        { name: "复原后", values: [0.11841, 0.12463], fill: C.blue },
      ],
      hasLegend: true,
      legend: { position: "bottom", overlay: false, textStyle: { fontSize: 13, fill: C.slate } },
      barOptions: { direction: "column", grouping: "clustered", gapWidth: 58 },
      yAxis: { majorGridlines: { style: "solid", fill: "#E2E8F0", width: 1 }, textStyle: { fontSize: 12, fill: C.muted } },
      xAxis: { textStyle: { fontSize: 13, fill: C.slate } },
      dataLabels: { showValue: true, position: "outEnd", textStyle: { fontSize: 12, fill: C.ink, bold: true } },
    });
    addText(slide,
      "为何使用无参考指标？",
      { left: 92, top: 372, width: 400, height: 36 },
      { fontSize: 27, bold: true, color: C.ink },
    );
    addText(slide,
      "题目未提供图1对应清晰参考图，因此不能计算 MSE/SSIM。信息熵和标准差用于评价灰度信息量与对比度；平均梯度和拉普拉斯方差用于评价边缘变化和高频细节。",
      { left: 92, top: 426, width: 1020, height: 82 },
      { fontSize: 22, color: C.slate },
    );
  }

  // 10. Discussion.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.white;
    addSlideTitle(slide, "讨论：方法有效性与限制", "09  Discussion", 10);
    addPanel(slide, { left: 92, top: 158, width: 480, height: 345 }, "#F7FBFA", "#BFD7EA");
    addText(slide, "有效性", { left: 124, top: 192, width: 220, height: 34 }, {
      fontSize: 30,
      bold: true,
      color: C.teal,
    });
    addText(slide,
      "频域陷波针对周期噪声峰值，抑制条纹干扰。\n\n空间域滤波进一步降低随机噪声。\n\n高提升锐化补偿滤波造成的局部细节损失。",
      { left: 124, top: 250, width: 400, height: 182 },
      { fontSize: 22, color: C.ink },
    );
    addPanel(slide, { left: 664, top: 158, width: 480, height: 345 }, "#FFFCF7", "#E9D8A6");
    addText(slide, "限制", { left: 696, top: 192, width: 220, height: 34 }, {
      fontSize: 30,
      bold: true,
      color: C.gold,
    });
    addText(slide,
      "陷波中心依赖当前图像频谱，需要针对不同图像重新估计。\n\n复原任务的 PSF 是近似模型，可能存在局部过锐化。\n\n无参考指标只能间接反映视觉质量。",
      { left: 696, top: 250, width: 400, height: 182 },
      { fontSize: 22, color: C.ink },
    );
  }

  // 11. Conclusion.
  {
    const slide = deck.slides.add();
    slide.background.fill = C.pale;
    addSlideTitle(slide, "结论", "10  Conclusion", 11);
    addText(slide,
      "本课程设计完成了空间域与频率域结合的图像增强算法，并针对图1模糊树林图像完成基于维纳滤波的复原实验。",
      { left: 112, top: 170, width: 980, height: 80 },
      { fontSize: 28, bold: true, color: C.ink },
    );
    addPanel(slide, { left: 112, top: 296, width: 286, height: 150 }, C.white, C.gray);
    addSmallMetric(slide, "MSE", "0.071 → 0.0065", 160, 326, C.teal);
    addPanel(slide, { left: 474, top: 296, width: 286, height: 150 }, C.white, C.gray);
    addSmallMetric(slide, "SSIM", "0.104 → 0.656", 522, 326, C.blue);
    addPanel(slide, { left: 836, top: 296, width: 286, height: 150 }, C.white, C.gray);
    addSmallMetric(slide, "Avg. Gradient", "0.035 → 0.118", 884, 326, C.gold);
    addText(slide,
      "后续可将频谱峰值检测自动化，并引入盲去卷积或更强的无参考质量评价方法作为对比。",
      { left: 150, top: 512, width: 900, height: 48 },
      { fontSize: 24, color: C.slate, alignment: "center" },
    );
  }

  for (const [i, slide] of deck.slides.items.entries()) {
    const stem = `slide-${String(i + 1).padStart(2, "0")}`;
    const png = await deck.export({ slide, format: "png", scale: 1 });
    await writeBlob(path.join(previewDir, `${stem}.png`), png);
    const layout = await slide.export({ format: "layout" });
    await fs.writeFile(path.join(layoutDir, `${stem}.layout.json`), await layout.text(), "utf8");
  }
  const montage = await deck.export({ format: "webp", montage: true, scale: 1 });
  await writeBlob(path.join(previewDir, "deck-montage.webp"), montage);

  const inspect = await deck.inspect({
    kind: "slide,textbox,shape,image,table,chart,layout",
    maxChars: 20000,
  });
  await fs.writeFile(path.join(previewDir, "inspect.ndjson"), inspect.ndjson, "utf8");

  const pptx = await PresentationFile.exportPptx(deck);
  await pptx.save(finalPptx);
  console.log(finalPptx);
}

buildDeck().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
