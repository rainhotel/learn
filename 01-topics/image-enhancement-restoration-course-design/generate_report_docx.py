from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


TOPIC_DIR = Path(__file__).resolve().parent
ROOT_DIR = TOPIC_DIR.parents[1]
OUTPUT_DIR = ROOT_DIR / "03-outputs" / "image-enhancement-restoration-course-design"
DOCX_PATH = OUTPUT_DIR / "course-design3-report-final.docx"


BLUE = "2E74B5"
DARK_BLUE = "1F4D78"
INK_BLUE = "0B2545"
LIGHT_GRAY = "F2F4F7"
BORDER_GRAY = "A6A6A6"


def set_run_font(run, name="Calibri", east_asia="SimSun", size=None, bold=None, color=None):
    run.font.name = name
    r_fonts = run._element.get_or_add_rPr().get_or_add_rFonts()
    r_fonts.set(qn("w:ascii"), name)
    r_fonts.set(qn("w:hAnsi"), name)
    r_fonts.set(qn("w:eastAsia"), east_asia)
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if color is not None:
        run.font.color.rgb = RGBColor.from_string(color)


def configure_styles(doc):
    section = doc.sections[0]
    section.page_width = Inches(8.5)
    section.page_height = Inches(11)
    section.top_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.left_margin = Inches(1)
    section.right_margin = Inches(1)
    section.header_distance = Inches(0.492)
    section.footer_distance = Inches(0.492)

    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "Calibri"
    normal.font.size = Pt(11)
    normal._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    normal._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "SimSun")
    normal.paragraph_format.space_before = Pt(0)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.line_spacing = 1.10

    h1 = styles["Heading 1"]
    h1.font.name = "Calibri"
    h1.font.size = Pt(16)
    h1.font.color.rgb = RGBColor.from_string(BLUE)
    h1._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    h1._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    h1._element.rPr.rFonts.set(qn("w:eastAsia"), "SimHei")
    h1.paragraph_format.space_before = Pt(16)
    h1.paragraph_format.space_after = Pt(8)
    h1.paragraph_format.line_spacing = 1.10

    h2 = styles["Heading 2"]
    h2.font.name = "Calibri"
    h2.font.size = Pt(13)
    h2.font.color.rgb = RGBColor.from_string(BLUE)
    h2._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    h2._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    h2._element.rPr.rFonts.set(qn("w:eastAsia"), "SimHei")
    h2.paragraph_format.space_before = Pt(12)
    h2.paragraph_format.space_after = Pt(6)
    h2.paragraph_format.line_spacing = 1.10

    h3 = styles["Heading 3"]
    h3.font.name = "Calibri"
    h3.font.size = Pt(12)
    h3.font.color.rgb = RGBColor.from_string(DARK_BLUE)
    h3._element.rPr.rFonts.set(qn("w:ascii"), "Calibri")
    h3._element.rPr.rFonts.set(qn("w:hAnsi"), "Calibri")
    h3._element.rPr.rFonts.set(qn("w:eastAsia"), "SimHei")
    h3.paragraph_format.space_before = Pt(8)
    h3.paragraph_format.space_after = Pt(4)
    h3.paragraph_format.line_spacing = 1.10


def add_header_footer(doc):
    section = doc.sections[0]
    header_p = section.header.paragraphs[0]
    header_p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    run = header_p.add_run("图像增强与复原算法综合应用课程设计")
    set_run_font(run, size=9, color="666666")
    add_paragraph_bottom_border(header_p, "D9E2F3")

    footer_p = section.footer.paragraphs[0]
    footer_p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = footer_p.add_run("第 ")
    set_run_font(run, size=9, color="666666")
    add_page_field(footer_p)
    run = footer_p.add_run(" 页")
    set_run_font(run, size=9, color="666666")


def add_paragraph_bottom_border(paragraph, color):
    p_pr = paragraph._p.get_or_add_pPr()
    p_bdr = p_pr.find(qn("w:pBdr"))
    if p_bdr is None:
        p_bdr = OxmlElement("w:pBdr")
        p_pr.append(p_bdr)
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), "4")
    bottom.set(qn("w:space"), "1")
    bottom.set(qn("w:color"), color)
    p_bdr.append(bottom)


def add_page_field(paragraph):
    run = paragraph.add_run()
    fld_char1 = OxmlElement("w:fldChar")
    fld_char1.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = "PAGE"
    fld_char2 = OxmlElement("w:fldChar")
    fld_char2.set(qn("w:fldCharType"), "end")
    run._r.append(fld_char1)
    run._r.append(instr)
    run._r.append(fld_char2)
    set_run_font(run, size=9, color="666666")


def add_title(doc):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_after = Pt(12)
    run = p.add_run("图像增强与复原算法综合应用课程设计报告")
    set_run_font(run, size=20, bold=True, color=INK_BLUE, east_asia="SimHei")

    meta = doc.add_paragraph()
    meta.alignment = WD_ALIGN_PARAGRAPH.CENTER
    meta.paragraph_format.space_after = Pt(14)
    run = meta.add_run("课程：数字图像处理实践    指导教师：范春年    姓名：李林浩    学号：202483290054    班级：2024级4班")
    set_run_font(run, size=10.5, color="666666")


def add_heading(doc, text, level=1):
    doc.add_heading(text, level=level)


def add_body(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.line_spacing = 1.10
    run = p.add_run(text)
    set_run_font(run, size=11)
    return p


def add_caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(8)
    run = p.add_run(text)
    set_run_font(run, size=9, color="666666")


def add_figure(doc, image_name, caption):
    image_path = OUTPUT_DIR / image_name
    if not image_path.exists():
        return
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run()
    run.add_picture(str(image_path), width=Inches(6.3))
    add_caption(doc, caption)


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_width(cell, width_dxa):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_w = tc_pr.find(qn("w:tcW"))
    if tc_w is None:
        tc_w = OxmlElement("w:tcW")
        tc_pr.append(tc_w)
    tc_w.set(qn("w:w"), str(width_dxa))
    tc_w.set(qn("w:type"), "dxa")


def set_table_geometry(table, widths_dxa):
    table.alignment = WD_TABLE_ALIGNMENT.LEFT
    table.autofit = False

    tbl = table._tbl
    tbl_pr = tbl.tblPr
    tbl_w = tbl_pr.find(qn("w:tblW"))
    if tbl_w is None:
        tbl_w = OxmlElement("w:tblW")
        tbl_pr.append(tbl_w)
    tbl_w.set(qn("w:w"), str(sum(widths_dxa)))
    tbl_w.set(qn("w:type"), "dxa")

    tbl_ind = tbl_pr.find(qn("w:tblInd"))
    if tbl_ind is None:
        tbl_ind = OxmlElement("w:tblInd")
        tbl_pr.append(tbl_ind)
    tbl_ind.set(qn("w:w"), "120")
    tbl_ind.set(qn("w:type"), "dxa")

    layout = tbl_pr.find(qn("w:tblLayout"))
    if layout is None:
        layout = OxmlElement("w:tblLayout")
        tbl_pr.append(layout)
    layout.set(qn("w:type"), "fixed")

    tbl_grid = tbl.find(qn("w:tblGrid"))
    if tbl_grid is not None:
        tbl.remove(tbl_grid)
    tbl_grid = OxmlElement("w:tblGrid")
    for width in widths_dxa:
        col = OxmlElement("w:gridCol")
        col.set(qn("w:w"), str(width))
        tbl_grid.append(col)
    tbl.insert(1, tbl_grid)

    for row in table.rows:
        for idx, cell in enumerate(row.cells):
            set_cell_width(cell, widths_dxa[idx])
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER


def set_table_borders(table):
    tbl_pr = table._tbl.tblPr
    borders = tbl_pr.find(qn("w:tblBorders"))
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ["top", "left", "bottom", "right", "insideH", "insideV"]:
        tag = "w:" + edge
        element = borders.find(qn(tag))
        if element is None:
            element = OxmlElement(tag)
            borders.append(element)
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), "4")
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), BORDER_GRAY)


def add_metrics_table(doc, headers, rows, widths_dxa):
    table = doc.add_table(rows=1, cols=len(headers))
    set_table_geometry(table, widths_dxa)
    set_table_borders(table)

    header_cells = table.rows[0].cells
    for idx, text in enumerate(headers):
        cell = header_cells[idx]
        cell.text = ""
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = p.add_run(text)
        set_run_font(run, size=9.5, bold=True)
        set_cell_shading(cell, LIGHT_GRAY)

    for row in rows:
        cells = table.add_row().cells
        for idx, value in enumerate(row):
            cells[idx].text = ""
            p = cells[idx].paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER if idx > 0 else WD_ALIGN_PARAGRAPH.LEFT
            run = p.add_run(value)
            set_run_font(run, size=9.5)
            set_cell_width(cells[idx], widths_dxa[idx])

    doc.add_paragraph()
    return table


def add_code_block(doc, code):
    for line in code.strip("\n").splitlines():
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(0)
        run = p.add_run(line)
        set_run_font(run, name="Courier New", east_asia="SimSun", size=9.5)
    doc.add_paragraph()


def build_docx():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    doc = Document()
    configure_styles(doc)
    add_header_footer(doc)
    add_title(doc)

    add_heading(doc, "摘要", 1)
    add_body(
        doc,
        "本课程设计围绕混合噪声图像增强和未知降质图像复原两个任务展开。图像增强任务中，"
        "先通过傅里叶频谱分析定位周期噪声峰值，再设计高斯陷波滤波器在频率域抑制周期噪声，"
        "随后使用中值滤波、均值滤波、高提升锐化和百分位灰度拉伸在空间域进一步降低随机噪声。"
        "图像复原任务中，学号 202483290054 尾数为偶数，选择图1模糊树林图像，采用近似高斯点扩散函数的维纳复原方法，"
        "并结合高提升锐化和灰度拉伸恢复边缘细节。"
    )
    add_body(
        doc,
        "实验结果表明，增强任务的 MSE 由 0.071191 降至 0.006541，SNR 由 4.664 dB 提升至 15.032 dB，"
        "PSNR 由 11.476 dB 提升至 21.844 dB，SSIM 由 0.1039 提升至 0.6558。图1复原后平均梯度由 "
        "0.03489 提升至 0.11841，拉普拉斯方差由 0.01926 提升至 0.12463，说明图像细节和边缘清晰度明显增强。"
    )
    add_body(doc, "关键词：图像增强；周期噪声；陷波滤波；中值滤波；维纳滤波；图像复原")

    add_heading(doc, "1. 任务描述", 1)
    add_body(
        doc,
        "课程设计包含图像增强和图像复原两部分。增强任务要求对含随机噪声和周期噪声的图像进行处理，"
        "清晰图像仅用于算法评估。复原任务要求在两幅未知降质图像中按学号尾数选择一幅进行复原。"
        "本文默认按偶数学号选择图1模糊树林图像；脚本同时生成图2低照度瀑布图像的备选结果。"
    )

    add_heading(doc, "2. 问题分析", 1)
    add_heading(doc, "2.1 混合噪声图像增强", 2)
    add_body(
        doc,
        "退化图像中存在明显规则条纹，并叠加随机噪声。周期噪声在空间域中表现为规则明暗变化，"
        "在频率域中表现为远离中心的成对亮点。因此，单纯使用空间域滤波难以彻底去除周期条纹，"
        "而单纯使用频域滤波又不能充分处理随机噪声。本文采用频率域陷波和空间域滤波相结合的方案。"
    )
    add_heading(doc, "2.2 模糊图像复原", 2)
    add_body(
        doc,
        "图1模糊树林图像主要表现为边缘不清晰、纹理细节弱。该退化过程可近似为清晰图像经过低通型点扩散函数模糊后再叠加少量噪声。"
        "由于真实退化函数未知，本文使用高斯点扩散函数近似退化模型，并采用维纳滤波抑制逆滤波可能导致的噪声放大。"
    )

    add_heading(doc, "3. 算法设计", 1)
    add_heading(doc, "3.1 图像增强算法", 2)
    add_body(
        doc,
        "增强算法流程为：读入图像并归一化，进行傅里叶变换，按频谱峰值构造高斯陷波滤波器，"
        "再依次使用 5 x 5 中值滤波、3 x 3 均值滤波、高提升锐化和百分位灰度拉伸。"
        "主陷波中心设为 (0, ±71) 和 (±74, 0)，陷波半径 D0 取 10。"
    )
    add_body(doc, "高斯陷波滤波器定义为 H_k(u,v)=1-exp(-D_k(u,v)^2/(2D0^2))，总滤波器为各陷波器的乘积。")
    add_heading(doc, "3.2 图像复原算法", 2)
    add_body(
        doc,
        "复原算法使用退化模型 g(x,y)=h(x,y)*f(x,y)+n(x,y)。图1中取高斯 PSF 近似 h(x,y)，"
        "参数为 psf_size=13、psf_sigma=1.35，并使用维纳滤波公式 F_hat=H*/(|H|^2+K)G，K=0.004。"
        "复原后的亮度分量再经高提升锐化和百分位拉伸后替换回彩色图像。"
    )

    add_heading(doc, "4. 实验结果与分析", 1)
    add_heading(doc, "4.1 增强任务结果", 2)
    add_metrics_table(
        doc,
        ["阶段", "MSE", "RMSE", "SNR/dB", "PSNR/dB", "SSIM"],
        [
            ["处理前", "0.071191", "0.266816", "4.664", "11.476", "0.1039"],
            ["处理后", "0.006541", "0.080877", "15.032", "21.844", "0.6558"],
        ],
        [1560, 1560, 1560, 1560, 1560, 1560],
    )
    add_body(
        doc,
        "处理后 MSE 明显下降，SNR 和 PSNR 均提升约 10 dB，SSIM 从 0.1039 提升到 0.6558，"
        "说明周期噪声和随机噪声均得到有效抑制，图像结构信息恢复明显。"
    )
    add_figure(doc, "enhancement_comparison.png", "图1 图像增强处理流程与结果对比")
    add_figure(doc, "enhancement_spectrum_and_notch.png", "图2 周期噪声频谱峰值与高斯陷波滤波器")
    add_figure(doc, "enhancement_error_comparison.png", "图3 增强前后误差对比")

    add_heading(doc, "4.2 复原任务结果", 2)
    add_metrics_table(
        doc,
        ["阶段", "信息熵", "标准差", "平均梯度", "拉普拉斯方差"],
        [
            ["复原前", "7.320", "0.176", "0.03489", "0.01926"],
            ["复原后", "7.607", "0.228", "0.11841", "0.12463"],
        ],
        [1872, 1872, 1872, 1872, 1872],
    )
    add_body(
        doc,
        "图1复原后信息熵和标准差均有所提升，说明灰度层次和整体对比度增强。平均梯度和拉普拉斯方差大幅提升，"
        "说明边缘变化和高频细节明显增强，树林纹理和轮廓比复原前更清晰。"
    )
    add_figure(doc, "restoration_fig1_comparison.png", "图4 图1模糊树林图像复原结果")

    add_heading(doc, "4.3 图2备选结果", 2)
    add_metrics_table(
        doc,
        ["阶段", "信息熵", "标准差", "平均梯度", "拉普拉斯方差"],
        [
            ["复原前", "6.926", "0.163", "0.01148", "0.00100"],
            ["复原后", "7.525", "0.201", "0.03442", "0.01115"],
        ],
        [1872, 1872, 1872, 1872, 1872],
    )
    add_body(
        doc,
        "若学号尾数为奇数，可选择图2低照度瀑布图像。脚本对图2使用 Retinex 光照校正、gamma 变换和高提升锐化。"
        "从无参考指标看，处理后亮度层次、对比度和细节清晰度均有提升。"
    )
    add_figure(doc, "restoration_fig2_comparison.png", "图5 图2低照度瀑布图像备选复原结果")

    add_heading(doc, "5. 关键 MATLAB 实现", 1)
    add_body(doc, "完整程序见 01-topics/image-enhancement-restoration-course-design/matlab/run_course_design3.m。核心流程如下。")
    add_code_block(
        doc,
        """
[freqFiltered, notchFilter] = gaussian_notch_reject(noisy, notchOffsets, notchRadius);
medianFiltered = median_filter2(freqFiltered, 5);
meanFiltered = mean_filter2(medianFiltered, 3);
sharpened = high_boost(meanFiltered, 0.15);
enhanced = percentile_stretch(sharpened, 0.5, 99.5);

psf = gaussian_kernel2(13, 1.35);
deconvY = wiener_deconvolution(y, psf, 0.004);
detail = deconvY - gaussian_blur2(deconvY, 1.0);
restoredY = clamp01(deconvY + 0.28 * detail);
restoredY = percentile_stretch(restoredY, 1.0, 99.0);
""",
    )

    add_heading(doc, "6. 结论", 1)
    add_body(
        doc,
        "本文完成了混合噪声图像增强和未知降质图像复原两个任务。增强任务中，频域高斯陷波能够有效抑制周期条纹，"
        "空间域中值滤波和均值滤波进一步降低随机噪声，高提升锐化和灰度拉伸改善了细节与对比度。"
        "从 MSE、SNR、PSNR 和 SSIM 指标看，处理结果明显优于退化图像。"
    )
    add_body(
        doc,
        "复原任务中，本文针对图1模糊树林图像建立近似高斯模糊模型，并使用维纳滤波进行复原。"
        "由于没有清晰参考图，本文采用信息熵、标准差、平均梯度和拉普拉斯方差作为无参考评价指标。"
        "实验结果表明，复原后图像细节和边缘清晰度均得到改善。"
    )
    add_body(
        doc,
        "本实验仍存在一定限制：增强任务中的陷波中心依赖当前图像频谱分析，复原任务中的退化函数为近似模型。"
        "后续可进一步研究自适应频谱峰检测、盲去卷积和更稳定的无参考图像质量评价方法。"
    )

    doc.save(DOCX_PATH)
    return DOCX_PATH


if __name__ == "__main__":
    path = build_docx()
    print(path)

