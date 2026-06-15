from __future__ import annotations

import shutil
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(r"D:\moniC\project\learn")
SRC = ROOT / "hustzc" / "7.单总线CPU" / "单总线实验资料包(愚人节版)" / "MipsOnBusCpu-3.circ"
OUT = ROOT / "hustzc" / "7.单总线CPU" / "单总线实验资料包(愚人节版)" / "MipsOnBusCpu-3-exp5-1.circ"


STATE_CIRCUIT = "◇时序发生器状态机(定长指令周期)"
STATE_OUT_CIRCUIT = "◇时序发生器输出函数(定长指令周期)"
CONTROL_CIRCUIT = "◇硬布线控制器组合逻辑单元"
HARDWIRED_CONTROLLER = "◇硬布线控制器"


def comp(name: str, lib: str, loc: tuple[int, int], attrs: dict[str, str], text: str | None = None) -> ET.Element:
    node = ET.Element("comp", {"lib": lib, "loc": f"({loc[0]},{loc[1]})", "name": name})
    for key, value in attrs.items():
        child = ET.SubElement(node, "a", {"name": key})
        if text is not None and key == "contents":
            child.text = value
        else:
            child.set("val", value)
    return node


def wire(p1: tuple[int, int], p2: tuple[int, int]) -> ET.Element:
    return ET.Element("wire", {"from": f"({p1[0]},{p1[1]})", "to": f"({p2[0]},{p2[1]})"})


def tunnel(loc: tuple[int, int], label: str, facing: str = "east", width: int = 1) -> ET.Element:
    return comp(
        "Tunnel",
        "0",
        loc,
        {
            "facing": facing,
            "width": str(width),
            "label": label,
            "labelfont": "Dialog plain 12",
        },
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
    return comp("Splitter", "0", loc, attrs)


def rom(loc: tuple[int, int], addr_width: int, data_width: int, contents: list[int]) -> ET.Element:
    hex_width = max(1, (data_width + 3) // 4)
    rows: list[str] = []
    for start in range(0, len(contents), 8):
        chunk = contents[start : start + 8]
        rows.append(" ".join(f"{value:0{hex_width}x}" for value in chunk))
    body = f"addr/data: {addr_width} {data_width}\n" + "\n".join(rows) + "\n"
    return comp(
        "ROM",
        "4",
        loc,
        {
            "addrWidth": str(addr_width),
            "dataWidth": str(data_width),
            "label": "",
            "labelfont": "Dialog plain 12",
            "labelcolor": "#000000",
            "contents": body,
            # In this Logisim build the ROM select pin is low-active by default.
            # The original handout used an invalid "(none)" value, so we model
            # the intended always-enabled behavior with Select=low and sel=0.
            "Select": "low",
        },
        text=body,
    )


def logic_gate(name: str, loc: tuple[int, int], facing: str = "east", inputs: int = 2) -> ET.Element:
    return comp(
        name,
        "1",
        loc,
        {
            "facing": facing,
            "width": "1",
            "size": "30",
            "inputs": str(inputs),
            "out": "01",
            "label": "",
            "labelfont": "Dialog plain 12",
            "labelcolor": "#000000",
            "negate0": "false",
            "negate1": "false",
        },
    )


def rom_addr_pin(loc: tuple[int, int]) -> tuple[int, int]:
    x, y = loc
    # Match the base ROM geometry used by the handout circuits: the address
    # input sits immediately on the west edge of the component, not far away.
    return (x - 10, y)


def rom_sel_pin(loc: tuple[int, int]) -> tuple[int, int]:
    x, y = loc
    return (x - 90, y + 40)


def east_branch_points(loc: tuple[int, int], fanout: int) -> list[tuple[int, int]]:
    x, y = loc
    start = -(fanout - 2) * 10
    return [(x + 20, y + start + idx * 10) for idx in range(fanout)]


def west_branch_points(loc: tuple[int, int], fanout: int) -> list[tuple[int, int]]:
    x, y = loc
    start = -(fanout - 2) * 10
    return [(x - 20, y + start + idx * 10) for idx in range(fanout)]


def get_circuit(root: ET.Element, name: str) -> ET.Element:
    for circuit in root.findall("circuit"):
        if circuit.attrib.get("name") == name:
            return circuit
    raise ValueError(f"Missing circuit: {name}")


def pin_map(circuit: ET.Element) -> dict[str, tuple[int, int]]:
    mapping: dict[str, tuple[int, int]] = {}
    for node in circuit.findall("comp"):
        if node.attrib.get("name") != "Pin":
            continue
        attrs = {a.attrib["name"]: a.attrib.get("val", "") for a in node.findall("a")}
        mapping[attrs["label"]] = eval(node.attrib["loc"])
    return mapping


def clear_non_pins(circuit: ET.Element) -> None:
    for node in list(circuit):
        if node.tag == "comp" and node.attrib.get("name") == "Pin":
            continue
        circuit.remove(node)


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


def build_state_machine(circuit: ET.Element) -> None:
    clear_non_pins(circuit)
    pins = pin_map(circuit)
    inputs = ["S3", "S2", "S1", "S0"]
    outputs = ["N3", "N2", "N1", "N0"]
    attach_pin_tunnels(circuit, pins, inputs, outputs, "fsm")

    in_split = (150, 130)
    out_split = (270, 130)
    rom_loc = (200, 130)
    circuit.append(splitter(in_split, "east", 4, ["3", "2", "1", "0"]))
    circuit.append(splitter(out_split, "west", 4, ["3", "2", "1", "0"]))
    circuit.append(rom(rom_loc, 4, 4, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 0, 0, 0, 0]))
    circuit.append(comp("Constant", "0", (70, 170), {"facing": "east", "width": "1", "value": "0x0"}))
    circuit.append(wire((70, 170), rom_sel_pin(rom_loc)))
    circuit.append(wire((in_split[0], in_split[1]), (in_split[0], 160)))
    circuit.append(wire((in_split[0], 160), (rom_addr_pin(rom_loc)[0], 160)))
    circuit.append(wire((rom_addr_pin(rom_loc)[0], 160), rom_addr_pin(rom_loc)))
    circuit.append(wire(rom_loc, (230, 130)))
    circuit.append(wire((230, 130), (230, 160)))
    circuit.append(wire((230, 160), (out_split[0], 160)))
    circuit.append(wire((out_split[0], 160), (out_split[0], out_split[1])))

    for label, point in zip(inputs, east_branch_points(in_split, len(inputs))):
        src = (point[0] - 30, point[1])
        circuit.append(tunnel(src, f"fsm_{label}", facing="west"))
        circuit.append(wire(src, point))

    for label, point in zip(outputs, west_branch_points(out_split, len(outputs))):
        dst = (point[0] + 30, point[1])
        circuit.append(tunnel(dst, f"fsm_{label}", facing="east"))
        circuit.append(wire(point, dst))


def build_state_outputs(circuit: ET.Element) -> None:
    clear_non_pins(circuit)
    pins = pin_map(circuit)
    inputs = ["S3", "S2", "S1", "S0"]
    outputs = ["Mif", "Mcal", "Mex", "T1", "T2", "T3", "T4"]
    attach_pin_tunnels(circuit, pins, inputs, outputs, "tg")

    contents = [0] * 16
    for state in range(12):
        if state < 4:
            phase_bits = (1, 0, 0)
            t_index = state
        elif state < 8:
            phase_bits = (0, 1, 0)
            t_index = state - 4
        else:
            phase_bits = (0, 0, 1)
            t_index = state - 8

        flags = {
            "Mif": phase_bits[0],
            "Mcal": phase_bits[1],
            "Mex": phase_bits[2],
            "T1": 1 if t_index == 0 else 0,
            "T2": 1 if t_index == 1 else 0,
            "T3": 1 if t_index == 2 else 0,
            "T4": 1 if t_index == 3 else 0,
        }
        value = 0
        for bit, label in enumerate(reversed(outputs)):
            value |= flags[label] << bit
        contents[state] = value

    in_split = (140, 180)
    out_split = (280, 180)
    rom_loc = (190, 180)
    circuit.append(splitter(in_split, "east", 4, ["3", "2", "1", "0"]))
    circuit.append(splitter(out_split, "west", 7, ["6", "5", "4", "3", "2", "1", "0"]))
    circuit.append(rom(rom_loc, 4, 7, contents))
    circuit.append(comp("Constant", "0", (60, 220), {"facing": "east", "width": "1", "value": "0x0"}))
    circuit.append(wire((60, 220), rom_sel_pin(rom_loc)))
    circuit.append(wire((in_split[0], in_split[1]), (in_split[0], 210)))
    circuit.append(wire((in_split[0], 210), (rom_addr_pin(rom_loc)[0], 210)))
    circuit.append(wire((rom_addr_pin(rom_loc)[0], 210), rom_addr_pin(rom_loc)))
    circuit.append(wire(rom_loc, (220, 180)))
    circuit.append(wire((220, 180), (220, 210)))
    circuit.append(wire((220, 210), (out_split[0], 210)))
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
    "DRout",
    "Pcout",
    "Rout",
    "Zout",
    "IRaout",
    "IRiout",
    "Pcin",
    "DREout",
    "DREin",
    "ARin",
    "Xin",
    "DRin",
    "IRin",
    "Rin",
    "RegTgt",
    "PSWin",
    "Add",
    "RegDst",
    "Slt",
    "Add4",
    "WRITE",
    "READ",
]


def active_instruction(signals: dict[str, int]) -> str:
    for name in ("LW", "SW", "BEQ", "SLT", "ADDI"):
        if signals[name]:
            return name
    return "ADD"


def phase_and_step(signals: dict[str, int]) -> tuple[str | None, int | None]:
    phase = None
    for name in ("Mif", "Mcal", "Mex"):
        if signals[name]:
            phase = name
            break
    step = None
    for idx, name in enumerate(("T1", "T2", "T3", "T4"), start=1):
        if signals[name]:
            step = idx
            break
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
        if instr in {"ADD", "SLT", "ADDI", "LW", "SW"}:
            if step == 1:
                active |= {"Rout", "Xin"}
            elif step == 2:
                if instr == "ADD":
                    active |= {"Rout", "Add", "RegTgt"}
                elif instr == "SLT":
                    active |= {"Rout", "Slt", "RegTgt"}
                else:
                    active |= {"IRiout", "Add"}
            elif step == 3:
                if instr == "ADD":
                    active |= {"Zout", "Rin", "RegDst"}
                elif instr == "SLT":
                    active |= {"Zout", "Rin", "RegDst"}
                elif instr == "ADDI":
                    active |= {"Zout", "Rin"}
                else:
                    active |= {"Zout", "ARin"}
            elif step == 4:
                if instr == "LW":
                    active |= {"READ", "DREin"}
                elif instr == "SW":
                    active |= {"Rout", "RegTgt", "DRin"}
        elif instr == "BEQ":
            if step == 1:
                active |= {"Pcout", "Xin"}
            elif step == 2:
                active |= {"IRaout", "Add"}
            elif step == 3 and signals["EQUAL"]:
                active |= {"Zout", "Pcin"}
    elif phase == "Mex":
        if step == 1:
            if instr == "LW":
                active |= {"DRout", "Rin"}
            elif instr == "SW":
                active |= {"DREout", "WRITE"}

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

    # A plain address ROM is X-sensitive: during fetch the instruction decoder
    # outputs and EQUAL can still be undefined, which turns the whole control
    # word into X and makes the RAM WE line spuriously active.
    # We gate those irrelevant inputs down to 0 before they reach the ROM.
    phase_mem_or = (130, 330)
    safe_slt_and = (130, 30)
    safe_addi_and = (130, 80)
    safe_lw_and = (130, 130)
    safe_sw_and = (130, 180)
    safe_beq_and = (130, 230)
    safe_eq_beq_and = (210, 630)
    safe_equal_and = (290, 630)

    circuit.append(logic_gate("OR Gate", phase_mem_or))
    circuit.append(tunnel((90, 320), "ctl_raw_Mcal", facing="east"))
    circuit.append(tunnel((90, 340), "ctl_raw_Mex", facing="east"))
    circuit.append(wire((90, 320), (100, 320)))
    circuit.append(wire((90, 340), (100, 340)))
    circuit.append(tunnel((160, 330), "ctl_phase_mem", facing="west"))
    circuit.append(wire(phase_mem_or, (160, 330)))

    for raw_label, raw_y, gate_loc, other_label, other_y, safe_label in [
        ("SLT", 20, safe_slt_and, "Mcal", 40, "ctl_safe_SLT"),
        ("ADDI", 70, safe_addi_and, "Mcal", 90, "ctl_safe_ADDI"),
        ("LW", 120, safe_lw_and, "phase_mem", 140, "ctl_safe_LW"),
        ("SW", 170, safe_sw_and, "phase_mem", 190, "ctl_safe_SW"),
        ("BEQ", 220, safe_beq_and, "Mcal", 240, "ctl_safe_BEQ"),
    ]:
        circuit.append(logic_gate("AND Gate", gate_loc))
        circuit.append(tunnel((90, raw_y), f"ctl_raw_{raw_label}", facing="east"))
        circuit.append(wire((90, raw_y), (100, raw_y)))
        circuit.append(tunnel((90, other_y), f"ctl_raw_{other_label}" if other_label == "Mcal" else "ctl_phase_mem", facing="east"))
        circuit.append(wire((90, other_y), (100, other_y)))
        circuit.append(tunnel((160, gate_loc[1]), safe_label, facing="west"))
        circuit.append(wire(gate_loc, (160, gate_loc[1])))

    circuit.append(logic_gate("AND Gate", safe_eq_beq_and))
    circuit.append(tunnel((150, 620), "ctl_raw_EQUAL", facing="east"))
    circuit.append(tunnel((150, 640), "ctl_safe_BEQ", facing="east"))
    circuit.append(wire((150, 620), (180, 620)))
    circuit.append(wire((150, 640), (180, 640)))
    circuit.append(tunnel((240, 630), "ctl_eq_beq", facing="west"))
    circuit.append(wire(safe_eq_beq_and, (240, 630)))

    circuit.append(logic_gate("AND Gate", safe_equal_and))
    circuit.append(tunnel((250, 620), "ctl_eq_beq", facing="east"))
    circuit.append(tunnel((250, 640), "ctl_raw_T3", facing="east"))
    circuit.append(wire((250, 620), (260, 620)))
    circuit.append(wire((250, 640), (260, 640)))
    circuit.append(tunnel((320, 630), "ctl_safe_EQUAL", facing="west"))
    circuit.append(wire(safe_equal_and, (320, 630)))

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

    in_split = (260, 140)
    out_split = (440, 240)
    rom_loc = (330, 140)
    circuit.append(splitter(in_split, "east", len(CONTROL_INPUTS), [str(i) for i in range(len(CONTROL_INPUTS) - 1, -1, -1)]))
    circuit.append(splitter(out_split, "west", len(CONTROL_OUTPUTS), [str(i) for i in range(len(CONTROL_OUTPUTS) - 1, -1, -1)]))
    circuit.append(rom(rom_loc, len(CONTROL_INPUTS), len(CONTROL_OUTPUTS), build_control_rom()))
    circuit.append(comp("Constant", "0", (200, 180), {"facing": "east", "width": "1", "value": "0x0"}))
    circuit.append(wire((200, 180), rom_sel_pin(rom_loc)))
    circuit.append(wire((in_split[0], in_split[1]), (in_split[0], 170)))
    circuit.append(wire((in_split[0], 170), (rom_addr_pin(rom_loc)[0], 170)))
    circuit.append(wire((rom_addr_pin(rom_loc)[0], 170), rom_addr_pin(rom_loc)))
    circuit.append(wire(rom_loc, (380, 140)))
    circuit.append(wire((380, 140), (380, 260)))
    circuit.append(wire((380, 260), (out_split[0], 260)))
    circuit.append(wire((out_split[0], 260), (out_split[0], out_split[1])))

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
            "trigger": "rising",
            "behavior": "old",
            "label": "",
            "labelfont": "Dialog plain 12",
            "labelcolor": "#000000",
        }.items():
            ET.SubElement(node, "a", {"name": key, "val": value})
        return

    raise ValueError("4-bit state register not found in hardwired controller")


def wire_fixed_timing_controller(circuit: ET.Element) -> None:
    # Counter Q -> current-state bus -> timing-state splitter + timing-output splitter
    circuit.append(tunnel((260, 410), "fixed_state_bus", facing="west", width=4))
    circuit.append(wire((230, 410), (260, 410)))

    circuit.append(tunnel((220, 330), "fixed_state_bus", facing="east", width=4))
    circuit.append(wire((220, 330), (250, 330)))

    circuit.append(tunnel((350, 410), "fixed_state_bus", facing="east", width=4))
    circuit.append(wire((350, 410), (380, 410)))

    # Reuse the existing CLK tunnel label so the mod-12 counter advances.
    circuit.append(tunnel((180, 460), "CLK", facing="north"))
    circuit.append(wire((180, 430), (180, 460)))


def wire_state_register(circuit: ET.Element) -> None:
    # The source handout leaves the 4-bit timing state register effectively floating.
    # That makes the first control word undefined, so RAM[0] gets clobbered on the
    # first clock. Wire it as the fixed-length timing controller intends:
    # Q -> current-state bus and output decoder, next-state bus -> D, and keep the
    # register permanently selected/enabled with CLR/PRE held inactive.
    circuit.append(wire((200, 410), (230, 410)))
    circuit.append(wire((230, 410), (390, 410)))
    circuit.append(wire((230, 410), (230, 330)))
    circuit.append(wire((230, 330), (250, 330)))

    circuit.append(wire((170, 410), (150, 410)))
    circuit.append(wire((150, 410), (150, 330)))
    circuit.append(wire((150, 330), (380, 330)))

    circuit.append(tunnel((180, 460), "CLK", facing="north"))
    circuit.append(wire((180, 430), (180, 460)))

    circuit.append(comp("Constant", "0", (130, 400), {"facing": "east", "width": "1", "value": "0x1"}))
    circuit.append(wire((130, 400), (170, 400)))
    circuit.append(comp("Constant", "0", (130, 420), {"facing": "east", "width": "1", "value": "0x1"}))
    circuit.append(wire((130, 420), (170, 420)))

    circuit.append(comp("Constant", "0", (150, 390), {"facing": "east", "width": "1", "value": "0x0"}))
    circuit.append(wire((150, 390), (190, 390)))
    circuit.append(comp("Constant", "0", (150, 430), {"facing": "east", "width": "1", "value": "0x0"}))
    circuit.append(wire((150, 430), (190, 430)))


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SRC, OUT)

    tree = ET.parse(OUT)
    root = tree.getroot()

    build_state_machine(get_circuit(root, STATE_CIRCUIT))
    build_state_outputs(get_circuit(root, STATE_OUT_CIRCUIT))
    build_control_logic(get_circuit(root, CONTROL_CIRCUIT))
    wire_state_register(get_circuit(root, HARDWIRED_CONTROLLER))

    tree.write(OUT, encoding="utf-8", xml_declaration=True)
    print(OUT)


if __name__ == "__main__":
    main()
