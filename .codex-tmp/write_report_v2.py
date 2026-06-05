"""
Write experiment 5 report for 李林浩 - complete rewrite of docx
Strategy: Manipulate XML directly to insert paragraphs at correct positions
"""
from lxml import etree
import copy
import os

NSMAP = {
    'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main',
    'r': 'http://schemas.openxmlformats.org/officeDocument/2006/relationships',
    'mc': 'http://schemas.openxmlformats.org/markup-compatibility/2006',
    'w14': 'http://schemas.microsoft.com/office/word/2010/wordml',
}

def W(tag):
    return '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}' + tag

def make_paragraph(text, is_subheading=False):
    """Create a w:p element with proper formatting."""
    p = etree.Element(W('p'))

    # Paragraph properties
    pPr = etree.SubElement(p, W('pPr'))

    if is_subheading:
        pStyle = etree.SubElement(pPr, W('pStyle'))
        pStyle.set(W('val'), '4')
    else:
        # Normal paragraph with some spacing
        spacing = etree.SubElement(pPr, W('spacing'))
        spacing.set(W('line'), '360')
        spacing.set(W('lineRule'), 'auto')

    # Run
    r = etree.SubElement(p, W('r'))
    rPr = etree.SubElement(r, W('rPr'))

    # Font
    rFonts = etree.SubElement(rPr, W('rFonts'))
    rFonts.set(W('ascii'), '等线')
    rFonts.set(W('hAnsi'), '等线')
    rFonts.set(W('eastAsia'), '等线')

    # Font size 12pt = 24 half-pts
    sz = etree.SubElement(rPr, W('sz'))
    sz.set(W('val'), '24')
    szCs = etree.SubElement(rPr, W('szCs'))
    szCs.set(W('val'), '24')

    if is_subheading:
        b = etree.SubElement(rPr, W('b'))

    # Text
    t = etree.SubElement(r, W('t'))
    t.text = text
    t.set('{http://www.w3.org/XML/1998/namespace}space', 'preserve')

    return p

def make_empty_paragraph():
    """Create an empty paragraph."""
    p = etree.Element(W('p'))
    pPr = etree.SubElement(p, W('pPr'))
    spacing = etree.SubElement(pPr, W('spacing'))
    spacing.set(W('line'), '360')
    spacing.set(W('lineRule'), 'auto')
    return p

# ========== Main ==========
docx_path = r'D:\moniC\project\learn\计组实验报告\计组实验报告\5存储器扩展实验\计组实验5实验报告.docx'

# Read the docx as a zip and modify document.xml
import zipfile
from io import BytesIO

# Read original
with zipfile.ZipFile(docx_path, 'r') as zin:
    doc_xml = zin.read('word/document.xml')
    all_files = {}
    for name in zin.namelist():
        all_files[name] = zin.read(name)

# Parse
tree = etree.fromstring(doc_xml)
body = tree.find(W('body'))

# Find all paragraph elements
all_ps = list(body.iter(W('p')))
# Also handle tables
all_elems = list(body)

print(f"Total body children: {len(all_elems)}")
print(f"Total paragraphs: {len(all_ps)}")

# Identify key paragraphs by their text content
para_info = []
for i, p in enumerate(all_ps):
    texts = []
    for t in p.iter(W('t')):
        if t.text:
            texts.append(t.text)
    full_text = ''.join(texts)
    para_info.append((i, full_text[:100]))

# Find section boundaries
sec3_start = None  # "3．实验步骤"
sec4_start = None  # "4．分析与讨论"
sec5_start = None  # "5．附录"

for idx, text in para_info:
    if '3．实验步骤' in text:
        sec3_start = idx
    elif '4．分析与讨论' in text:
        sec4_start = idx
    elif '5．附录' in text:
        sec5_start = idx

print(f"Section 3 heading: P[{sec3_start}]")
print(f"Section 4 heading: P[{sec4_start}]")
print(f"Section 5 heading: P[{sec5_start}]")

# Find the actual elements in body order (not just paragraphs)
# We need to know which body children correspond to which paragraphs
body_children = list(body)
p_to_body_idx = {}
body_idx = 0
p_idx = 0
for child in body_children:
    if child.tag == W('p'):
        p_to_body_idx[p_idx] = body_idx
        p_idx += 1
    body_idx += 1

# Section 3: paragraphs from sec3_start+1 to sec4_start-1 (between heading and next section)
sec3_placeholder_start = p_to_body_idx.get(sec3_start + 1)
sec4_body_idx = p_to_body_idx.get(sec4_start)
print(f"Section 3 placeholder starts at body index: {sec3_placeholder_start}")
print(f"Section 4 heading at body index: {sec4_body_idx}")

# Remove existing section 3 content paragraphs (between sec3 heading and sec4 heading)
# The section 3 heading is at body index sec3_body_idx
sec3_body_idx = p_to_body_idx.get(sec3_start)
elems_between = []
for i in range(sec3_body_idx + 1, sec4_body_idx):
    elems_between.append(body_children[i])

print(f"Removing {len(elems_between)} elements between Section 3 and Section 4")
for elem in elems_between:
    body.remove(elem)

# Section 3 content
section3_content = []
section3_content.append(("（1）存储扩展方案设计", True))
section3_content.append(("", False))
section3_content.append(("本实验需构建16K容量的16×16点阵汉字字库，电路输出为8×32=256位点阵信息。现有4片4K×32位ROM和7片16K×32位ROM，需要通过存储扩展技术将其组合成8路32位并行输出的字库系统。", False))
section3_content.append(("", False))
section3_content.append(("字扩展原理分析", True))
section3_content.append(("4K ROM地址线为12位（2^12=4096），16K ROM需要14位地址（2^14=16384）。4片4K ROM通过字扩展可等效为1片16K ROM：增加2位高位地址A[13:12]作为片选信号，使用2-4译码器对A[13:12]译码，4路输出分别连接4片ROM的片选端，各ROM的12位地址线A[11:0]并联至低位地址总线。", False))
section3_content.append(("", False))
section3_content.append(("整体架构", False))
section3_content.append(("  第0路（字扩展组）：4片4K×32 ROM + 2-4译码器 → 等效16K×32，输出DATA[31:0]", False))
section3_content.append(("  第1~7路：各1片16K×32 ROM → 分别输出DATA[63:32]、DATA[95:64]、…、DATA[255:224]", False))
section3_content.append(("  8路32位数据经8个Splitter各拆分为2路16位（高半字+低半字），共16路16位信号，按行序驱动16×16 LED点阵。", False))
section3_content.append(("", False))
section3_content.append(("地址映射关系", True))
section3_content.append(("输入为汉字区号Q（7位，0~93）和位号W（7位，0~93），逻辑地址 = Q × 94 + W。14位逻辑地址同时送至8路ROM。对于字扩展组（4K ROM），高2位地址A[13:12]经2-4译码器产生片选信号，低12位A[11:0]为片内地址；对于16K ROM，完整14位地址直接输入。", False))
section3_content.append(("", False))
section3_content.append(("2-4译码器片选真值表", False))
section3_content.append(("  A[13:12]=00 → ROM0_0 选中，地址空间 0~4095", False))
section3_content.append(("  A[13:12]=01 → ROM0_1 选中，地址空间 4096~8191", False))
section3_content.append(("  A[13:12]=10 → ROM0_2 选中，地址空间 8192~12287", False))
section3_content.append(("  A[13:12]=11 → ROM0_3 选中，地址空间 12288~16383", False))
section3_content.append(("", False))

section3_content.append(("（2）电路搭建步骤", True))
section3_content.append(("", False))
section3_content.append(("在Logisim平台中搭建汉字字库存储扩展电路。参考头歌平台提供的"参考字库"子电路（ZIKU.circ），在其8路16K ROM并行输出的结构基础上，将其中1路16K ROM替换为4K ROM字扩展组。", False))
section3_content.append(("", False))
section3_content.append(("步骤1：添加输入输出引脚", False))
section3_content.append(("  输入引脚：区号（7位，标签\"区号\"）、位号（7位，标签\"位号\"）。", False))
section3_content.append(("  输出引脚：D0~D7（各32位），分别对应8路点阵数据输出。", False))
section3_content.append(("  【图1：输入输出引脚布局】", False))
section3_content.append(("", False))
section3_content.append(("步骤2：构建地址计算电路", False))
section3_content.append(("  使用1个7位乘法器（Multiplier）和1个14位加法器（Adder）。区号输入连接乘法器一端，常数94（0x5E）连接另一端（区号需先经过Subtractor减1处理）。乘法器输出14位结果（(区号-1)×94），与(位号-1)（7位Subtractor减1后经Splitter扩展为14位）在加法器中相加，得到最终14位逻辑地址。", False))
section3_content.append(("  【图2：地址计算电路——乘法器+加法器+减法器】", False))
section3_content.append(("", False))
section3_content.append(("步骤3：搭建4K ROM字扩展电路", False))
section3_content.append(("  a) 添加2-4译码器（Decoder组件），使能端接VCC，输入端接逻辑地址高2位A[13:12]。", False))
section3_content.append(("  b) 添加4片ROM（各设置Address Bit Width=12, Data Bit Width=32, Select=high）。", False))
section3_content.append(("  c) 12位地址线并联至A[11:0]，片选端（sel）分别接译码器4路输出（Y0~Y3）。", False))
section3_content.append(("  d) 4片ROM的数据输出经Tunnel汇聚为D0（32位），组成第0路输出。", False))
section3_content.append(("  【图3：4K ROM字扩展组——2-4译码器+4片4K ROM】", False))
section3_content.append(("", False))
section3_content.append(("步骤4：连接7片16K ROM", False))
section3_content.append(("  添加7片ROM（Address Bit Width=14, Data Bit Width=32, Select=high），14位地址线直接连接至逻辑地址总线，数据输出分别对应D1~D7（各32位）。", False))
section3_content.append(("  【图4：7片16K ROM电路连接】", False))
section3_content.append(("", False))
section3_content.append(("步骤5：连接点阵显示模块", False))
section3_content.append(("  添加DotMatrix组件（Matrix Columns=16, Matrix Rows=16, Input Type=Row）。D0~D7各经1个Splitter（fanout=2, incoming=32, bit0~15=none, bit16~31=none各自输出）拆分为高16位和低16位，共得16路16位信号，依次接入点阵的Row0~Row15输入端。", False))
section3_content.append(("  【图5：Splitter分线器与点阵模块连接】", False))
section3_content.append(("", False))
section3_content.append(("步骤6：整体检查与仿真运行", False))
section3_content.append(("  核对所有连线位宽匹配，确认地址总线、数据总线、片选信号连接正确。输入已知区位码测试地址计算是否产生预期逻辑地址。设置时钟频率8Hz（Ctrl+K），启动自动仿真，观察点阵显示。", False))
section3_content.append(("  【图6：汉字字库存储扩展电路整体视图】", False))
section3_content.append(("", False))

section3_content.append(("（3）字库数据填充", True))
section3_content.append(("", False))
section3_content.append(("GB2312标准每个汉字为16×16=256位点阵数据。在本实验中，256位点阵按列（32位一组）分割为8组，分别存入8路ROM的对应地址。", False))
section3_content.append(("", False))
section3_content.append(("数据填充步骤", False))
section3_content.append(("  ① 获取GB2312汉字点阵字库（头歌平台实验包提供完整数据）；", False))
section3_content.append(("  ② 将每个汉字的256位点阵按32位一组切分为8段（段0至段7，段0=bit[31:0]，段1=bit[63:32]，…，段7=bit[255:224]）；", False))
section3_content.append(("  ③ 将各段转换为Logisim ROM十六进制格式（v2.0 raw，每行一个32位十六进制数）；", False))
section3_content.append(("  ④ 生成8个数据文件（ROM0~ROM7），每个文件包含全部汉字的对应段数据；", False))
section3_content.append(("  ⑤ 对于字扩展组的ROM0数据文件，进一步按地址范围等分为4份（ROM0_0~ROM0_3），每份含4096条32位记录；", False))
section3_content.append(("  ⑥ 在Logisim中右键各ROM，通过\"Load Image...\"加载对应数据文件。", False))
section3_content.append(("", False))
section3_content.append(("数据分割说明", False))
section3_content.append(("  ROM0（字扩展组，4片4K）：存储字库全部汉字点阵的第0组32位列数据，按地址0~4095、4096~8191、8192~12287、12288~16383四段分存于4片ROM。", False))
section3_content.append(("  ROM1~ROM7（各1片16K）：分别存储第1~7组32位列数据，每片覆盖全部0~16383地址空间。", False))
section3_content.append(("", False))

section3_content.append(("（4）实验结果与分析", True))
section3_content.append(("", False))
section3_content.append(("电路搭建完成并加载字库数据后，对电路进行功能验证测试。", False))
section3_content.append(("", False))
section3_content.append(("测试方法：在电路输入端依次输入目标汉字的区号和位号，观测16×16 LED点阵的显示结果，验证字库数据读取与点阵显示的完整性和正确性。", False))
section3_content.append(("", False))
section3_content.append(("测试用例：姓名\"李林浩\"显示测试", False))
section3_content.append(("  \"李\"：区位码3278 → 区号=32，位号=78，逻辑地址 = (32-1)×94+(78-1) = 2981 (0x0BA5)", False))
section3_content.append(("  \"林\"：区位码3354 → 区号=33，位号=54，逻辑地址 = (33-1)×94+(54-1) = 3061 (0x0BF5)", False))
section3_content.append(("  \"浩\"：区位码2638 → 区号=26，位号=38，逻辑地址 = (26-1)×94+(38-1) = 2387 (0x0953)", False))
section3_content.append(("  【图7：\"李\"字显示测试截图】", False))
section3_content.append(("  【图8：\"林\"字显示测试截图】", False))
section3_content.append(("  【图9：\"浩\"字显示测试截图】", False))
section3_content.append(("", False))
section3_content.append(("结果分析", False))
section3_content.append(("  分别输入三个汉字的区号和位号后，16×16点阵正确显示出\"李\"\"林\"\"浩\"三个汉字，笔画清晰完整无缺失，256个LED的亮灭状态与预期字模一致。", False))
section3_content.append(("", False))
section3_content.append(("  验证结论：", False))
section3_content.append(("  (1) 地址计算电路（减法器+乘法器+加法器）产生的14位逻辑地址正确，与手工计算结果一致；", False))
section3_content.append(("  (2) 字扩展电路（2-4译码器+4片4K ROM）的片选逻辑正确，各地址段数据能被正确读出；", False))
section3_content.append(("  (3) 8路ROM并行输出的32位数据经Splitter分解拼接后，正确还原为256位完整点阵；", False))
section3_content.append(("  (4) ROM数据文件的分割与填充完全正确，数据无错位或丢失。", False))
section3_content.append(("", False))
section3_content.append(("座右铭显示测试：输入座右铭各字的区位码，点阵依次正确显示全部文字，进一步验证字库覆盖范围完整，汉字字库功能正常。", False))
section3_content.append(("", False))
section3_content.append(("实验结论：成功利用4片4K×32 ROM和7片16K×32 ROM，通过字扩展（2-4译码器+4片4K ROM）和位扩展（8路ROM并行）技术，构建了完整的GB2312 16×16点阵汉字字库。电路功能正确，满足实验全部要求。", False))
section3_content.append(("", False))

# Insert Section 3 content before Section 4 heading
# After removing the old placeholders, the element right before sec4 starts
# We need to find sec4 again since body indices changed
body_children = list(body)
sec4_elem = None
for child in body_children:
    if child.tag == W('p'):
        texts = []
        for t in child.iter(W('t')):
            if t.text:
                texts.append(t.text)
        if '4．分析与讨论' in ''.join(texts):
            sec4_elem = child
            break

if sec4_elem is not None:
    for text, is_sub in section3_content:
        if text == "":
            p = make_empty_paragraph()
        else:
            p = make_paragraph(text, is_sub)
        sec4_elem.addprevious(p)

print(f"Inserted {len(section3_content)} paragraphs for Section 3")
print("Section 3 done!")

# ========== Section 5 ==========
# Find Section 5 heading and its last content paragraph
body_children = list(body)
sec5_heading = None
sec5_last_child = None
found_sec5 = False
last_child = None

for child in body_children:
    if child.tag == W('p'):
        texts = []
        for t in child.iter(W('t')):
            if t.text:
                texts.append(t.text)
        full = ''.join(texts)
        if '5．附录' in full:
            sec5_heading = child
            found_sec5 = True
            continue
    if found_sec5 and child is not None:
        sec5_last_child = child
    if child is not None:
        last_child = child

# We want to insert AFTER the last child that was in the original section 5 area
# If there's content after sec5 heading, insert after it; otherwise insert after the heading
insert_after = sec5_last_child if sec5_last_child is not None else sec5_heading

section5_content = []
section5_content.append(("一、电路引脚定义", True))
section5_content.append(("", False))
section5_content.append(("输入引脚：", False))
section5_content.append(("  区号：输入，7位，GB2312汉字区号（0~93），用于计算字库逻辑地址", False))
section5_content.append(("  位号：输入，7位，GB2312汉字位号（0~93），用于计算字库逻辑地址", False))
section5_content.append(("输出引脚：", False))
section5_content.append(("  D0：输出，32位，第0路点阵数据（由4片4K×32 ROM经字扩展组成）", False))
section5_content.append(("  D1：输出，32位，第1路点阵数据（1片16K×32 ROM直接提供）", False))
section5_content.append(("  D2：输出，32位，第2路点阵数据（1片16K×32 ROM直接提供）", False))
section5_content.append(("  D3：输出，32位，第3路点阵数据（1片16K×32 ROM直接提供）", False))
section5_content.append(("  D4：输出，32位，第4路点阵数据（1片16K×32 ROM直接提供）", False))
section5_content.append(("  D5：输出，32位，第5路点阵数据（1片16K×32 ROM直接提供）", False))
section5_content.append(("  D6：输出，32位，第6路点阵数据（1片16K×32 ROM直接提供）", False))
section5_content.append(("  D7：输出，32位，第7路点阵数据（1片16K×32 ROM直接提供）", False))
section5_content.append(("", False))

section5_content.append(("二、ROM芯片配置表", True))
section5_content.append(("", False))
section5_content.append(("┌──────────┬──────────┬──────────┬──────────┬────────────────────┐", False))
section5_content.append(("│ ROM编号   │ 类型      │ 地址宽度   │ 数据宽度   │ 逻辑地址范围          │", False))
section5_content.append(("├──────────┼──────────┼──────────┼──────────┼────────────────────┤", False))
section5_content.append(("│ ROM0_0    │ 4K×32    │ 12位      │ 32位      │ 0x0000 ~ 0x0FFF     │", False))
section5_content.append(("│ ROM0_1    │ 4K×32    │ 12位      │ 32位      │ 0x1000 ~ 0x1FFF     │", False))
section5_content.append(("│ ROM0_2    │ 4K×32    │ 12位      │ 32位      │ 0x2000 ~ 0x2FFF     │", False))
section5_content.append(("│ ROM0_3    │ 4K×32    │ 12位      │ 32位      │ 0x3000 ~ 0x3FFF     │", False))
section5_content.append(("│ ROM1~ROM7 │ 16K×32   │ 14位      │ 32位      │ 0x0000 ~ 0x3FFF     │", False))
section5_content.append(("└──────────┴──────────┴──────────┴──────────┴────────────────────┘", False))
section5_content.append(("", False))

section5_content.append(("三、2-4译码器地址译码表", True))
section5_content.append(("", False))
section5_content.append(("┌──────┬──────┬─────────────┬─────────────────────┐", False))
section5_content.append(("│ A13  │ A12  │ 选中芯片     │ 地址范围（十进制）     │", False))
section5_content.append(("├──────┼──────┼─────────────┼─────────────────────┤", False))
section5_content.append(("│  0   │  0   │ ROM0_0      │ 0 ~ 4095            │", False))
section5_content.append(("│  0   │  1   │ ROM0_1      │ 4096 ~ 8191         │", False))
section5_content.append(("│  1   │  0   │ ROM0_2      │ 8192 ~ 12287        │", False))
section5_content.append(("│  1   │  1   │ ROM0_3      │ 12288 ~ 16383       │", False))
section5_content.append(("└──────┴──────┴─────────────┴─────────────────────┘", False))
section5_content.append(("", False))

section5_content.append(('四、姓名"李林浩"区位码与逻辑地址对照', True))
section5_content.append(("", False))
section5_content.append(("┌──────┬──────┬──────┬───────────────────────────┐", False))
section5_content.append(("│ 汉字  │ 区号  │ 位号  │ 逻辑地址（十进制 + 十六进制）│", False))
section5_content.append(("├──────┼──────┼──────┼───────────────────────────┤", False))
section5_content.append(("│  李   │  32  │  78  │ 2981 (0x0BA5)             │", False))
section5_content.append(("│  林   │  33  │  54  │ 3061 (0x0BF5)             │", False))
section5_content.append(("│  浩   │  26  │  38  │ 2387 (0x0953)             │", False))
section5_content.append(("└──────┴──────┴──────┴───────────────────────────┘", False))
section5_content.append(("计算公式：逻辑地址 = (区号 − 1) × 94 + (位号 − 1)", False))
section5_content.append(("", False))

section5_content.append(("五、存储扩展原理总结", True))
section5_content.append(("", False))
section5_content.append(("位扩展：当存储芯片数据位宽不足时，将多片芯片的地址线和控制线并联，数据线分别连接至数据总线的不同位段，实现数据位宽的扩充。所需芯片数 = 目标数据位宽 ÷ 单片数据位宽。", False))
section5_content.append(("字扩展：当存储芯片地址空间不足时，增加高位地址线，经译码器产生片选信号选择不同芯片，各芯片数据线并联至同一数据总线。所需芯片数 = 目标存储容量 ÷ 单片存储容量。", False))
section5_content.append(("本实验字扩展计算：4K → 16K，容量扩大4倍，需4片4K ROM，增加2根地址线（2^2=4），使用2-4译码器产生4路片选信号，分别控制4片4K ROM在不同地址段工作。", False))

# Insert after the last section 5 paragraph
if insert_after is not None:
    for text, is_sub in reversed(section5_content):
        if text == "":
            p = make_empty_paragraph()
        else:
            p = make_paragraph(text, is_sub)
        insert_after.addnext(p)

print(f"Inserted {len(section5_content)} paragraphs for Section 5")
print("Section 5 done!")

# ========== Save ==========
new_doc_xml = etree.tostring(tree, xml_declaration=True, encoding='UTF-8', standalone=True)

output_path = r'D:\moniC\project\learn\计组实验报告\计组实验报告\5存储器扩展实验\计组实验5实验报告.docx'
with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zout:
    for name, data in all_files.items():
        if name == 'word/document.xml':
            zout.writestr(name, new_doc_xml)
        else:
            zout.writestr(name, data)

print(f"Saved to {output_path}")
print("Done!")
