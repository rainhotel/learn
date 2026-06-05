"""
Embed all ROM font data and test sequence into storage.circ
"""
from lxml import etree
import re
import os

ROM_DIR = r'D:\moniC\project\learn\计组实验报告\计组实验报告\5存储器扩展实验'

def parse_rom_file(filepath):
    """Parse a Logisim v2.0 raw ROM file into a list of integers."""
    with open(filepath, 'r') as f:
        content = f.read()

    values = []
    for line in content.split('\n'):
        line = line.strip()
        if not line or line.startswith('v2.0'):
            continue
        # Remove comments
        if '#' in line:
            line = line[:line.index('#')]
        for token in line.split():
            if not token:
                continue
            if '*' in token:
                count, val = token.split('*')
                count = int(count)
                val = int(val, 16)
                values.extend([val] * count)
            else:
                values.append(int(token, 16))
    return values

def format_rom_data(values):
    """Format a list of integers as Logisim ROM text (with run-length encoding for efficiency)."""
    if not values:
        return ''

    lines = []
    current_line = []

    i = 0
    while i < len(values):
        # Check for run of repeated values
        run_len = 1
        while i + run_len < len(values) and values[i + run_len] == values[i]:
            run_len += 1

        if run_len >= 8:
            # Use N*val notation
            item = f'{run_len}*{values[i]:x}'
        else:
            item = f'{values[i]:x}'

        current_line.append(item)

        # Start new line every ~8 items
        if len(current_line) >= 8:
            lines.append(' '.join(current_line))
            current_line = []

        i += run_len if run_len >= 8 else 1

    if current_line:
        lines.append(' '.join(current_line))

    return '\n'.join(lines)

# ====== Parse all ROM files ======
print("Parsing ROM files...")
rom_data = {}
for i in range(8):
    fpath = os.path.join(ROM_DIR, f'ROM{i}')
    rom_data[i] = parse_rom_file(fpath)
    print(f'  ROM{i}: {len(rom_data[i])} entries')

# ====== Prepare data for each ROM ======
# ROM0 split into 4 parts for 4K ROMs
rom0 = rom_data[0]
rom0_parts = []
for part in range(4):
    start = part * 4096
    end = start + 4096
    segment = []
    for j in range(start, end):
        if j < len(rom0):
            segment.append(rom0[j])
        else:
            segment.append(0)
    rom0_parts.append(segment)
    non_zero = sum(1 for v in segment if v != 0)
    print(f'  ROM0 part {part}: {len(segment)} entries, {non_zero} non-zero')

# ROM1-ROM7 go to 16K ROMs (pad to 16384)
rom_parts_16k = {}
for i in range(1, 8):
    data = rom_data[i]
    padded = data + [0] * (16384 - len(data))
    rom_parts_16k[i] = padded
    non_zero = sum(1 for v in padded if v != 0)
    print(f'  ROM{i} padded: {len(padded)} entries, {non_zero} non-zero')

# ====== Modify the circuit ======
print("\nModifying storage.circ...")
tree = etree.parse(os.path.join(ROM_DIR, 'storage.circ'))
root = tree.getroot()

# Track which 4K ROM we're on (for ROM0 assignments)
rom0_part_idx = 0
# Track which 16K ROM we're on (for ROM1-ROM7 assignments)
rom16k_idx = 1

for circuit in root.iter('circuit'):
    cname = circuit.get('name', '')

    for comp in circuit.iter('comp'):
        if comp.get('name') != 'ROM':
            continue

        loc = comp.get('loc', '')
        addr_w = data_w = '14'
        for a in comp.findall('a'):
            if a.get('name') == 'addrWidth':
                addr_w = a.get('val', '14')
            if a.get('name') == 'dataWidth':
                data_w = a.get('val', '32')

        # Determine which data to embed
        embed_data = None

        if '字库电路' in cname and '参考' not in cname and '测试' not in cname:
            # This is the main "汉字字库电路"
            if addr_w == '12':
                # 4K ROM - use next ROM0 part
                if rom0_part_idx < 4:
                    embed_data = rom0_parts[rom0_part_idx]
                    print(f'  Embed ROM0 part {rom0_part_idx} into ROM at {loc} (addrWidth={addr_w})')
                    rom0_part_idx += 1
            elif addr_w == '14':
                # 16K ROM - use ROM1-ROM7
                if rom16k_idx < 8:
                    embed_data = rom_parts_16k[rom16k_idx]
                    print(f'  Embed ROM{rom16k_idx} into ROM at {loc} (addrWidth={addr_w})')
                    rom16k_idx += 1

        elif '参考字库' in cname:
            # Reference design - all 16K ROMs, use ROM0-ROM7
            # ROM0 for first, ROM1-ROM7 for rest
            if addr_w == '14':
                ref_idx = rom16k_idx_ref if 'rom16k_idx_ref' in dir() else 0
                # Actually let me use a different counter
                pass  # Skip reference circuit for now, focus on main

        elif '测试' in cname:
            # Test circuit ROM
            if addr_w == '8' and data_w == '16':
                # Test sequence ROM - already embedded from previous fix
                # Don't overwrite - it has李林浩 data
                print(f'  Skipping test ROM at {loc} (already has 李林浩 data)')
                continue

        # Embed data if we have it
        if embed_data is not None:
            formatted = format_rom_data(embed_data)
            rom_text = f'addr/data: {addr_w} {data_w}\n{formatted}'

            # Replace contents element
            for a in list(comp.findall('a')):
                if a.get('name') == 'contents':
                    comp.remove(a)

            new_a = etree.SubElement(comp, 'a')
            new_a.set('name', 'contents')
            new_a.text = rom_text
            new_a.tail = '\n      '

# ====== Save ======
output_path = os.path.join(ROM_DIR, 'storage.circ')
tree.write(output_path, xml_declaration=True, encoding='UTF-8', standalone=True)

import os as _os
size_mb = _os.path.getsize(output_path) / (1024*1024)
print(f'\nSaved: {output_path} ({size_mb:.1f} MB)')
print('Done!')
