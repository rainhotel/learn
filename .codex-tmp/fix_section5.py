# -*- coding: utf-8 -*-
"""Fix Section 5 (appendix) content insertion"""
from lxml import etree
import zipfile

W = lambda t: '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}' + t

def make_p(text, bold=False):
    p = etree.Element(W('p'))
    pPr = etree.SubElement(p, W('pPr'))
    if bold:
        etree.SubElement(pPr, W('pStyle')).set(W('val'), '4')
    else:
        s = etree.SubElement(pPr, W('spacing'))
        s.set(W('line'), '360')
        s.set(W('lineRule'), 'auto')
    r = etree.SubElement(p, W('r'))
    rPr = etree.SubElement(r, W('rPr'))
    rf = etree.SubElement(rPr, W('rFonts'))
    for a in ('ascii', 'hAnsi', 'eastAsia'):
        rf.set(W(a), '等线')
    for tag in ('sz', 'szCs'):
        etree.SubElement(rPr, W(tag)).set(W('val'), '24')
    if bold:
        etree.SubElement(rPr, W('b'))
    t = etree.SubElement(r, W('t'))
    t.text = text
    t.set('{http://www.w3.org/XML/1998/namespace}space', 'preserve')
    return p

def make_empty():
    p = etree.Element(W('p'))
    pPr = etree.SubElement(p, W('pPr'))
    s = etree.SubElement(pPr, W('spacing'))
    s.set(W('line'), '360')
    s.set(W('lineRule'), 'auto')
    return p

docx_path = r'D:\moniC\project\learn\计组实验报告\计组实验报告\5存储器扩展实验\计组实验5实验报告.docx'
with zipfile.ZipFile(docx_path, 'r') as zin:
    doc_xml = zin.read('word/document.xml')
    all_files = {n: zin.read(n) for n in zin.namelist()}

tree = etree.fromstring(doc_xml)
body = tree.find(W('body'))

# Find all body children
body_children = list(body)

# Find section 5 heading element
sec5_heading = None
for child in body_children:
    if child.tag == W('p'):
        txt = ''.join(t.text or '' for t in child.iter(W('t')))
        if '5．附录' in txt:
            sec5_heading = child
            break

if sec5_heading is not None:
    print(f'Found Section 5 heading: {sec5_heading.tag}')
    # Find the element right after the heading
    found = False
    insert_after = sec5_heading
    for child in body_children:
        if child is sec5_heading:
            found = True
            continue
        if found and child.tag == W('p'):
            insert_after = child
    print(f'Insert after element: {insert_after.tag}')
else:
    print('ERROR: Section 5 heading not found')
    exit(1)

# Section 5 content
S5 = []
def b(text, bold=False):
    S5.append((text, bold))

b('一、电路引脚定义', True)
b('')
b('输入引脚：', False)
b('  区号：输入，7位，GB2312汉字区号（0~93），用于计算字库逻辑地址', False)
b('  位号：输入，7位，GB2312汉字位号（0~93），用于计算字库逻辑地址', False)
b('输出引脚：', False)
b('  D0：输出，32位，第0路点阵数据（由4片4K×32 ROM经字扩展组成）', False)
b('  D1：输出，32位，第1路点阵数据（1片16K×32 ROM直接提供）', False)
b('  D2：输出，32位，第2路点阵数据（1片16K×32 ROM直接提供）', False)
b('  D3：输出，32位，第3路点阵数据（1片16K×32 ROM直接提供）', False)
b('  D4：输出，32位，第4路点阵数据（1片16K×32 ROM直接提供）', False)
b('  D5：输出，32位，第5路点阵数据（1片16K×32 ROM直接提供）', False)
b('  D6：输出，32位，第6路点阵数据（1片16K×32 ROM直接提供）', False)
b('  D7：输出，32位，第7路点阵数据（1片16K×32 ROM直接提供）', False)
b('')
b('二、ROM芯片配置表', True)
b('')
b('┌──────────┬──────────┬──────────┬──────────┬────────────────────┐', False)
b('│ ROM编号   │ 类型      │ 地址宽度   │ 数据宽度   │ 逻辑地址范围          │', False)
b('├──────────┼──────────┼──────────┼──────────┼────────────────────┤', False)
b('│ ROM0_0    │ 4K×32    │ 12位      │ 32位      │ 0x0000 ~ 0x0FFF     │', False)
b('│ ROM0_1    │ 4K×32    │ 12位      │ 32位      │ 0x1000 ~ 0x1FFF     │', False)
b('│ ROM0_2    │ 4K×32    │ 12位      │ 32位      │ 0x2000 ~ 0x2FFF     │', False)
b('│ ROM0_3    │ 4K×32    │ 12位      │ 32位      │ 0x3000 ~ 0x3FFF     │', False)
b('│ ROM1~ROM7 │ 16K×32   │ 14位      │ 32位      │ 0x0000 ~ 0x3FFF     │', False)
b('└──────────┴──────────┴──────────┴──────────┴────────────────────┘', False)
b('')
b('三、2-4译码器地址译码表', True)
b('')
b('┌──────┬──────┬─────────────┬─────────────────────┐', False)
b('│ A13  │ A12  │ 选中芯片     │ 地址范围（十进制）    │', False)
b('├──────┼──────┼─────────────┼─────────────────────┤', False)
b('│  0   │  0   │ ROM0_0      │ 0 ~ 4095            │', False)
b('│  0   │  1   │ ROM0_1      │ 4096 ~ 8191         │', False)
b('│  1   │  0   │ ROM0_2      │ 8192 ~ 12287        │', False)
b('│  1   │  1   │ ROM0_3      │ 12288 ~ 16383       │', False)
b('└──────┴──────┴─────────────┴─────────────────────┘', False)
b('')
b('四、姓名"李林浩"区位码与逻辑地址对照', True)
b('')
b('┌──────┬──────┬──────┬───────────────────────────┐', False)
b('│ 汉字  │ 区号  │ 位号  │ 逻辑地址（十进制 + 十六进制）│', False)
b('├──────┼──────┼──────┼───────────────────────────┤', False)
b('│  李   │  32  │  78  │ 2981 (0x0BA5)             │', False)
b('│  林   │  33  │  54  │ 3061 (0x0BF5)             │', False)
b('│  浩   │  26  │  38  │ 2387 (0x0953)             │', False)
b('└──────┴──────┴──────┴───────────────────────────┘', False)
b('计算公式：逻辑地址 = (区号 - 1) × 94 + (位号 - 1)', False)
b('')
b('五、存储扩展原理总结', True)
b('')
b('位扩展：当存储芯片数据位宽不足时，将多片芯片的地址线和控制线并联，数据线分别连接至数据总线的不同位段，实现数据位宽的扩充。所需芯片数 = 目标数据位宽 / 单片数据位宽。', False)
b('字扩展：当存储芯片地址空间不足时，增加高位地址线，经译码器产生片选信号选择不同芯片，各芯片数据线并联至同一数据总线。所需芯片数 = 目标存储容量 / 单片存储容量。', False)
b('本实验字扩展计算：4K -> 16K，容量扩大4倍，需4片4K ROM，增加2根地址线（2^2=4），使用2-4译码器产生4路片选信号，分别控制4片4K ROM在不同地址段工作。', False)

# Insert after the last paragraph following section 5 heading
for text, bold in S5:
    p = make_p(text, bold) if text else make_empty()
    insert_after.addnext(p)
    insert_after = p

print(f'Inserted {len(S5)} paragraphs for Section 5')

# Verify
body_children = list(body)
sec5_count = 0
found_sec5 = False
for child in body_children:
    if child is sec5_heading:
        found_sec5 = True
        continue
    if found_sec5 and child.tag == W('p'):
        sec5_count += 1
print(f'Paragraphs after Section 5 heading: {sec5_count}')

# Save
new_doc = etree.tostring(tree, xml_declaration=True, encoding='UTF-8', standalone=True)
with zipfile.ZipFile(docx_path, 'w', zipfile.ZIP_DEFLATED) as zout:
    for name, data in all_files.items():
        if name == 'word/document.xml':
            zout.writestr(name, new_doc)
        else:
            zout.writestr(name, data)

print(f'Saved: {docx_path}')
print('Done!')
