from __future__ import annotations

import shutil
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(r"D:\moniC\project\learn")
BASE = ROOT / "hustzc" / "7.单总线CPU" / "单总线实验资料包(RISC-V)（双十一版）" / "单总线实验资料包(RISC-V)（双十一版）"
SRC = BASE / "RiscvOnBusCpu-3.circ"
OUT = BASE / "RiscvOnBusCpu-3-clean.circ"


STATE_OUT_CIRCUIT = "◇时序发生器输出函数(定长指令周期)"
CONTROL_CIRCUIT = "◇硬布线控制器组合逻辑单元"
HARDWIRED_CONTROLLER = "◇硬布线控制器"


def comp(name: str, loc: tuple[int, int], attrs: dict[str, str], lib: str | None = None, text_attrs: dict[str, str] | None = None) -> ET.Element:
    node_attrs = {"loc": f"({loc[0]},{loc[1]})", "name": name}
    if lib is not None:
        node_attrs["lib"] = lib
    node = ET.Element("comp", node_attrs)
    for key, value in attrs.items():
        child = ET.SubElement(node, "a", {"name": key})
        if text_attrs and key in text_attrs:
            child.text = text_attrs[key]
        else:
            child.set("val", value)
    return node


def wire(p1: tuple[int, int], p2: tuple[int, int]) -> ET.Element:
    return ET.Element("wire", {"from": f"({p1[0]},{p1[1]})", "to": f"({p2[0]},{p2[1]})"})


def tunnel(loc: tuple[int, int], label: str, *, width: int = 1, facing: str = "east") -> ET.Element:
    return comp(
        "Tunnel",
        loc,
        {
            "facing": facing,
            "width": str(width),
            "label": label,
            "labelfont": "Dialog plain 12",
        },
        lib="0",
    )


def splitter(loc: tuple[int, int], facing: str, incoming: int, bit_values: list[str]) -> ET.Element:
    attrs = {
        "facing": facing,
        "fanout": str(len(bit_values)),
        "incoming": str(incoming),
        "appear": "center",
    }
    for idx, value in enumerate(bit_values):
        attrs[f"bit{idx}"] = value
    return comp("Splitter", loc, attrs, lib="0")


def constant(loc: tuple[int, int], value: str, *, width: int = 1) -> ET.Element:
    return comp("Constant", loc, {"facing": "east", "width": str(width), "value": value}, lib="0")


def pull_resistor(loc: tuple[int, int], pull: str, *, facing: str = "east") -> ET.Element:
    return comp("Pull Resistor", loc, {"facing": facing, "pull": pull}, lib="0")


def logic_gate(name: str, loc: tuple[int, int], facing: str = "east", inputs: int = 2) -> ET.Element:
    attrs = {
        "facing": facing,
        "width": "1",
        "size": "30",
        "inputs": str(inputs),
        "out": "01",
        "label": "",
        "labelfont": "Dialog plain 12",
        "labelcolor": "#000000",
    }
    for idx in range(inputs):
        attrs[f"negate{idx}"] = "false"
    return comp(name, loc, attrs, lib="1")


def rom(loc: tuple[int, int], addr_width: int, data_width: int, contents: list[int]) -> ET.Element:
    hex_width = max(1, (data_width + 3) // 4)
    rows: list[str] = []
    for start in range(0, len(contents), 8):
        chunk = contents[start : start + 8]
        rows.append(" ".join(f"{value:0{hex_width}x}" for value in chunk))
    body = f"addr/data: {addr_width} {data_width}\n" + "\n".join(rows) + "\n"
    return comp(
        "ROM",
        loc,
        {
            "addrWidth": str(addr_width),
            "dataWidth": str(data_width),
            "label": "",
            "labelfont": "Dialog plain 12",
            "labelcolor": "#000000",
            "contents": body,
            "Select": "low",
        },
        lib="4",
        text_attrs={"contents": body},
    )


def get_circuit(root: ET.Element, name: str) -> ET.Element:
    return next(c for c in root.findall("circuit") if c.attrib.get("name") == name)


def clear_non_pins(circuit: ET.Element) -> None:
    for node in list(circuit):
        if node.tag == "comp" and node.attrib.get("name") == "Pin":
            continue
        circuit.remove(node)


def pin_map(circuit: ET.Element) -> dict[str, tuple[int, int]]:
    mapping: dict[str, tuple[int, int]] = {}
    for node in circuit.findall("comp"):
        if node.attrib.get("name") != "Pin":
            continue
        attrs = {a.attrib["name"]: a.attrib.get("val", "") for a in node.findall("a")}
        mapping[attrs["label"]] = eval(node.attrib["loc"])
    return mapping


def attach_pin_tunnels(
    circuit: ET.Element,
    pins: dict[str, tuple[int, int]],
    input_labels: list[str],
    output_labels: list[str],
    prefix: str,
) -> None:
    for label in input_labels:
        x, y = pins[label]
        tloc = (x + 40, y)
        circuit.append(tunnel(tloc, f"{prefix}_{label}", facing="east"))
        circuit.append(wire((x, y), tloc))
    for label in output_labels:
        x, y = pins[label]
        tloc = (x - 40, y)
        circuit.append(tunnel(tloc, f"{prefix}_{label}", facing="west"))
        circuit.append(wire(tloc, (x, y)))


def rom_addr_pin(loc: tuple[int, int]) -> tuple[int, int]:
    return (loc[0] - 10, loc[1])


def rom_sel_pin(loc: tuple[int, int]) -> tuple[int, int]:
    return (loc[0] - 90, loc[1] + 40)


def east_branch_points(loc: tuple[int, int], fanout: int) -> list[tuple[int, int]]:
    x, y = loc
    start = -(fanout - 2) * 10
    return [(x + 20, y + start + idx * 10) for idx in range(fanout)]


def west_branch_points(loc: tuple[int, int], fanout: int) -> list[tuple[int, int]]:
    x, y = loc
    start = -(fanout - 2) * 10
    return [(x - 20, y + start + idx * 10) for idx in range(fanout)]


def build_state_outputs(circuit: ET.Element) -> None:
    clear_non_pins(circuit)
    pins = pin_map(circuit)
    inputs = ["S3", "S2", "S1", "S0"]
    outputs = ["Mif", "Mcal", "Mex", "T1", "T2", "T3", "T4"]
    attach_pin_tunnels(circuit, pins, inputs, outputs, "tg")

    contents = [0] * 16
    for state in range(12):
        if state < 4:
            phase = "Mif"
            step = state
        elif state < 8:
            phase = "Mcal"
            step = state - 4
        else:
            phase = "Mex"
            step = state - 8

        flags = {
            "Mif": 1 if phase == "Mif" else 0,
            "Mcal": 1 if phase == "Mcal" else 0,
            "Mex": 1 if phase == "Mex" else 0,
            "T1": 1 if step == 0 else 0,
            "T2": 1 if step == 1 else 0,
            "T3": 1 if step == 2 else 0,
            "T4": 1 if step == 3 else 0,
        }
        value = 0
        for bit, label in enumerate(reversed(outputs)):
            value |= flags[label] << bit
        contents[state] = value

    in_split = (90, 180)
    out_split = (120, 180)
    rom_loc = (110, 180)
    circuit.append(splitter(in_split, "east", 4, ["3", "2", "1", "0"]))
    circuit.append(splitter(out_split, "west", 7, ["6", "5", "4", "3", "2", "1", "0"]))
    circuit.append(rom(rom_loc, 4, 7, contents))
    circuit.append(constant((20, 220), "0x0"))
    circuit.append(wire((20, 220), rom_sel_pin(rom_loc)))
    circuit.append(wire((in_split[0], in_split[1]), (in_split[0], 210)))
    circuit.append(wire((in_split[0], 210), (rom_addr_pin(rom_loc)[0], 210)))
    circuit.append(wire((rom_addr_pin(rom_loc)[0], 210), rom_addr_pin(rom_loc)))
    circuit.append(wire(rom_loc, (140, 180)))
    circuit.append(wire((140, 180), (140, 210)))
    circuit.append(wire((140, 210), (out_split[0], 210)))
    circuit.append(wire((out_split[0], 210), (out_split[0], out_split[1])))

    for label, point in zip(inputs, east_branch_points(in_split, len(inputs))):
        src = (point[0] - 30, point[1])
        circuit.append(tunnel(src, f"tg_{label}", facing="west"))
        circuit.append(wire(src, point))

    for label, point in zip(outputs, west_branch_points(out_split, len(outputs))):
        dst = (point[0] + 30, point[1])
        circuit.append(tunnel(dst, f"tg_{label}", facing="east"))
        circuit.append(wire(point, dst))


CONTROL_INPUTS = ["SLT", "ADDI", "LW", "SW", "BEQ", "Mif", "Mcal", "Mex", "T1", "T2", "T3", "T4", "EQUAL"]
CONTROL_OUTPUTS = [
    "Pcout",
    "DRout",
    "Zout",
    "Rout",
    "IRIout",
    "IRSout",
    "IRBout",
    "DREout",
    "Pcin",
    "ARin",
    "DREin",
    "DRin",
    "Xin",
    "Rin",
    "IRin",
    "PSWin",
    "rs12",
    "Add",
    "Add4",
    "Slt",
    "READ",
    "WRITE",
]


def active_instruction(signals: dict[str, int]) -> str:
    for name in ("LW", "SW", "BEQ", "SLT", "ADDI"):
        if signals[name]:
            return name
    return "OTHER"


def phase_and_step(signals: dict[str, int]) -> tuple[str | None, int | None]:
    phase = next((name for name in ("Mif", "Mcal", "Mex") if signals[name]), None)
    step = next((idx for idx, name in enumerate(("T1", "T2", "T3", "T4"), start=1) if signals[name]), None)
    return phase, step


def control_word(signals: dict[str, int]) -> int:
    phase, step = phase_and_step(signals)
    instr = active_instruction(signals)
    active: set[str] = set()

    if phase == "Mif":
        if step == 1:
            active |= {"Pcout", "ARin", "Xin"}
        elif step == 2:
            active |= {"Add4"}
        elif step == 3:
            active |= {"Zout", "Pcin", "READ", "DREin"}
        elif step == 4:
            active |= {"DRout", "IRin"}
    elif phase == "Mcal":
        if instr in {"ADDI", "LW", "SW", "SLT"}:
            if step == 1:
                active |= {"Rout", "Xin"}
            elif step == 2:
                if instr == "SLT":
                    active |= {"Rout", "rs12", "Slt"}
                else:
                    active |= {"IRIout", "Add"}
            elif step == 3:
                if instr in {"ADDI", "SLT"}:
                    active |= {"Zout", "Rin"}
                else:
                    active |= {"Zout", "ARin"}
            elif step == 4:
                if instr == "LW":
                    active |= {"READ", "DREin"}
                elif instr == "SW":
                    active |= {"Rout", "rs12", "DRin"}
        elif instr == "BEQ":
            if step == 1:
                active |= {"Rout", "Xin"}
            elif step == 2:
                active |= {"Rout", "rs12"}
            elif step == 3:
                active |= {"Pcout", "Xin"}
            elif step == 4:
                active |= {"IRBout", "Add"}
    elif phase == "Mex":
        if step == 1:
            if instr == "LW":
                active |= {"DRout", "Rin"}
            elif instr == "SW":
                active |= {"DREout", "WRITE"}
            elif instr == "BEQ" and signals["EQUAL"]:
                active |= {"Zout", "Pcin"}

    value = 0
    for bit, label in enumerate(reversed(CONTROL_OUTPUTS)):
        value |= (1 if label in active else 0) << bit
    return value


def build_control_rom() -> list[int]:
    contents = [0] * (1 << len(CONTROL_INPUTS))
    for addr in range(len(contents)):
        signals = {}
        for idx, label in enumerate(CONTROL_INPUTS):
            bit = len(CONTROL_INPUTS) - 1 - idx
            signals[label] = (addr >> bit) & 1
        contents[addr] = control_word(signals)
    return contents


def build_control_logic(circuit: ET.Element) -> None:
    clear_non_pins(circuit)
    pins = pin_map(circuit)

    for label in CONTROL_INPUTS:
        x, y = pins[label]
        tloc = (x + 40, y)
        circuit.append(tunnel(tloc, f"ctl_raw_{label}", facing="east"))
        circuit.append(wire((x, y), tloc))
    for label in CONTROL_OUTPUTS:
        x, y = pins[label]
        tloc = (x - 40, y)
        circuit.append(tunnel(tloc, f"ctl_{label}", facing="west"))
        circuit.append(wire(tloc, (x, y)))

    phase_mem_or = (110, 330)
    safe_slt_and = (110, 30)
    safe_addi_and = (110, 80)
    safe_lw_and = (110, 130)
    safe_sw_and = (110, 180)
    safe_beq_and = (110, 230)
    safe_eq_beq_and = (180, 630)
    safe_equal_and = (260, 630)

    circuit.append(logic_gate("OR Gate", phase_mem_or))
    circuit.append(tunnel((70, 320), "ctl_raw_Mcal", facing="east"))
    circuit.append(tunnel((70, 340), "ctl_raw_Mex", facing="east"))
    circuit.append(wire((70, 320), (80, 320)))
    circuit.append(wire((70, 340), (80, 340)))
    circuit.append(tunnel((140, 330), "ctl_phase_mem", facing="west"))
    circuit.append(wire(phase_mem_or, (140, 330)))

    for raw_label, raw_y, gate_loc, other_label, other_y, safe_label in [
        ("SLT", 20, safe_slt_and, "Mcal", 40, "ctl_safe_SLT"),
        ("ADDI", 70, safe_addi_and, "Mcal", 90, "ctl_safe_ADDI"),
        ("LW", 120, safe_lw_and, "phase_mem", 140, "ctl_safe_LW"),
        ("SW", 170, safe_sw_and, "phase_mem", 190, "ctl_safe_SW"),
        ("BEQ", 220, safe_beq_and, "phase_mem", 240, "ctl_safe_BEQ"),
    ]:
        circuit.append(logic_gate("AND Gate", gate_loc))
        circuit.append(tunnel((70, raw_y), f"ctl_raw_{raw_label}", facing="east"))
        circuit.append(wire((70, raw_y), (80, raw_y)))
        other_label_name = f"ctl_raw_{other_label}" if other_label == "Mcal" else "ctl_phase_mem"
        circuit.append(tunnel((70, other_y), other_label_name, facing="east"))
        circuit.append(wire((70, other_y), (80, other_y)))
        circuit.append(tunnel((140, gate_loc[1]), safe_label, facing="west"))
        circuit.append(wire(gate_loc, (140, gate_loc[1])))

    circuit.append(logic_gate("AND Gate", safe_eq_beq_and))
    circuit.append(tunnel((130, 620), "ctl_raw_EQUAL", facing="east"))
    circuit.append(tunnel((130, 640), "ctl_safe_BEQ", facing="east"))
    circuit.append(wire((130, 620), (150, 620)))
    circuit.append(wire((130, 640), (150, 640)))
    circuit.append(tunnel((210, 630), "ctl_eq_beq", facing="west"))
    circuit.append(wire(safe_eq_beq_and, (210, 630)))

    circuit.append(logic_gate("AND Gate", safe_equal_and))
    circuit.append(tunnel((220, 620), "ctl_eq_beq", facing="east"))
    circuit.append(tunnel((220, 640), "ctl_raw_Mex", facing="east"))
    circuit.append(wire((220, 620), (230, 620)))
    circuit.append(wire((220, 640), (230, 640)))
    circuit.append(tunnel((290, 630), "ctl_safe_EQUAL", facing="west"))
    circuit.append(wire(safe_equal_and, (290, 630)))

    input_sources = {
        "SLT": "ctl_safe_SLT",
        "ADDI": "ctl_safe_ADDI",
        "LW": "ctl_safe_LW",
        "SW": "ctl_safe_SW",
        "BEQ": "ctl_safe_BEQ",
        "Mif": "ctl_raw_Mif",
        "Mcal": "ctl_raw_Mcal",
        "Mex": "ctl_raw_Mex",
        "T1": "ctl_raw_T1",
        "T2": "ctl_raw_T2",
        "T3": "ctl_raw_T3",
        "T4": "ctl_raw_T4",
        "EQUAL": "ctl_safe_EQUAL",
    }

    in_split = (120, 140)
    out_split = (300, 530)
    rom_loc = (190, 140)
    circuit.append(splitter(in_split, "east", len(CONTROL_INPUTS), [str(i) for i in range(len(CONTROL_INPUTS) - 1, -1, -1)]))
    circuit.append(splitter(out_split, "west", len(CONTROL_OUTPUTS), [str(i) for i in range(len(CONTROL_OUTPUTS) - 1, -1, -1)]))
    circuit.append(rom(rom_loc, len(CONTROL_INPUTS), len(CONTROL_OUTPUTS), build_control_rom()))
    circuit.append(constant((60, 180), "0x0"))
    circuit.append(wire((60, 180), rom_sel_pin(rom_loc)))
    circuit.append(wire((in_split[0], in_split[1]), (in_split[0], 170)))
    circuit.append(wire((in_split[0], 170), (rom_addr_pin(rom_loc)[0], 170)))
    circuit.append(wire((rom_addr_pin(rom_loc)[0], 170), rom_addr_pin(rom_loc)))
    circuit.append(wire(rom_loc, (240, 140)))
    circuit.append(wire((240, 140), (240, 550)))
    circuit.append(wire((240, 550), (out_split[0], 550)))
    circuit.append(wire((out_split[0], 550), (out_split[0], out_split[1])))

    for label, point in zip(CONTROL_INPUTS, east_branch_points(in_split, len(CONTROL_INPUTS))):
        src = (point[0] - 30, point[1])
        circuit.append(tunnel(src, input_sources[label], facing="west"))
        circuit.append(wire(src, point))

    for label, point in zip(CONTROL_OUTPUTS, west_branch_points(out_split, len(CONTROL_OUTPUTS))):
        dst = (point[0] + 30, point[1])
        circuit.append(tunnel(dst, f"ctl_{label}", facing="east"))
        circuit.append(wire(point, dst))


def replace_state_register_with_counter(circuit: ET.Element) -> None:
    for node in circuit.findall("comp"):
        if node.attrib.get("name") != "Register":
            continue
        attrs = {a.attrib.get("name"): a.attrib.get("val") for a in node.findall("a")}
        if attrs.get("width") != "4":
            continue
        node.attrib["name"] = "Counter"
        node.attrib["lib"] = "4"
        for child in list(node):
            node.remove(child)
        for key, value in {
            "width": "4",
            "max": "0xb",
            "ongoal": "wrap",
            "trigger": "falling",
            "behavior": "old",
            "label": "",
            "labelfont": "Dialog plain 12",
            "labelcolor": "#000000",
        }.items():
            ET.SubElement(node, "a", {"name": key, "val": value})
        return
    raise ValueError("4-bit state register not found")


def wire_fixed_timing_controller(circuit: ET.Element) -> None:
    circuit.append(wire((200, 410), (390, 410)))
    circuit.append(pull_resistor((200, 450), "0"))
    circuit.append(pull_resistor((200, 490), "0"))
    circuit.append(tunnel((170, 400), "CLK", facing="west"))
    circuit.append(wire((170, 400), (200, 400)))


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SRC, OUT)

    tree = ET.parse(OUT)
    root = tree.getroot()

    build_state_outputs(get_circuit(root, STATE_OUT_CIRCUIT))
    build_control_logic(get_circuit(root, CONTROL_CIRCUIT))
    replace_state_register_with_counter(get_circuit(root, HARDWIRED_CONTROLLER))
    wire_fixed_timing_controller(get_circuit(root, HARDWIRED_CONTROLLER))

    tree.write(OUT, encoding="utf-8", xml_declaration=True)
    print(OUT)


if __name__ == "__main__":
    main()
