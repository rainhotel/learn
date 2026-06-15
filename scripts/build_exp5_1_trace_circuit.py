from __future__ import annotations

import shutil
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(r"D:\moniC\project\learn")
SRC = ROOT / "hustzc" / "7.单总线CPU" / "单总线实验资料包(愚人节版)" / "MipsOnBusCpu-3-exp5-1.circ"
OUT = ROOT / "hustzc" / "7.单总线CPU" / "单总线实验资料包(愚人节版)" / "MipsOnBusCpu-3-exp5-1-trace.circ"
CPU_CIRCUIT = "◆单总线CPU(3级时序)"


def comp(name: str, lib: str, loc: tuple[int, int], attrs: dict[str, str]) -> ET.Element:
    node = ET.Element("comp", {"lib": lib, "loc": f"({loc[0]},{loc[1]})", "name": name})
    for key, value in attrs.items():
        ET.SubElement(node, "a", {"name": key, "val": value})
    return node


def wire(p1: tuple[int, int], p2: tuple[int, int]) -> ET.Element:
    return ET.Element("wire", {"from": f"({p1[0]},{p1[1]})", "to": f"({p2[0]},{p2[1]})"})


def tunnel(loc: tuple[int, int], label: str, *, width: int, facing: str) -> ET.Element:
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


def output_pin(loc: tuple[int, int], label: str, width: int) -> ET.Element:
    return comp(
        "Pin",
        "0",
        loc,
        {
            "facing": "west",
            "output": "true",
            "width": str(width),
            "tristate": "true",
            "pull": "none",
            "label": label,
            "labelloc": "east",
            "labelfont": "Dialog plain 12",
            "labelcolor": "#000000",
        },
    )


def splitter(loc: tuple[int, int], *, facing: str, incoming: int, bit_values: list[str]) -> ET.Element:
    attrs = {
        "facing": facing,
        "fanout": str(len(bit_values)),
        "incoming": str(incoming),
        "appear": "center",
    }
    for idx, value in enumerate(bit_values):
        attrs[f"bit{idx}"] = value
    return comp("Splitter", "0", loc, attrs)


def add_debug_output(
    circuit: ET.Element,
    *,
    source_point: tuple[int, int],
    source_label: str,
    width: int,
    tunnel_label: str,
    pin_loc: tuple[int, int],
    pin_label: str,
) -> None:
    source_tunnel_loc = (source_point[0] + 20, source_point[1])
    circuit.append(tunnel(source_tunnel_loc, tunnel_label, width=width, facing="east"))
    circuit.append(wire(source_point, source_tunnel_loc))

    pin_tunnel_loc = (pin_loc[0] - 30, pin_loc[1])
    circuit.append(tunnel(pin_tunnel_loc, tunnel_label, width=width, facing="west"))
    circuit.append(output_pin(pin_loc, pin_label, width))
    circuit.append(wire(pin_tunnel_loc, pin_loc))


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SRC, OUT)

    tree = ET.parse(OUT)
    root = tree.getroot()
    root.find("main").set("name", CPU_CIRCUIT)

    cpu = None
    for circuit in root.findall("circuit"):
        if circuit.attrib.get("name") == CPU_CIRCUIT:
            cpu = circuit
            break
    if cpu is None:
        raise ValueError(f"Missing circuit: {CPU_CIRCUIT}")

    debug_specs = [
        ((530, 140), "PC", 32, "DBG_PC", (1340, 120), "pc"),
        ((740, 210), "IR", 32, "DBG_IR", (1340, 160), "ir"),
        ((930, 210), "IMM32", 32, "DBG_IMM32", (1340, 200), "imm32"),
        ((170, 280), "AR", 32, "DBG_AR", (1340, 240), "ar"),
        ((510, 280), "DR", 32, "DBG_DR", (1340, 280), "dr"),
        ((280, 500), "X", 32, "DBG_X", (1340, 320), "x"),
        ((530, 550), "Z", 32, "DBG_Z", (1340, 360), "z"),
        ((590, 670), "BUS", 32, "DBG_BUS", (1340, 400), "bus"),
        ((980, 100), "CTRL", 22, "DBG_CTRL", (1340, 440), "ctrl"),
    ]
    for source_point, _source_label, width, tunnel_label, pin_loc, pin_label in debug_specs:
        add_debug_output(
            cpu,
            source_point=source_point,
            source_label=_source_label,
            width=width,
            tunnel_label=tunnel_label,
            pin_loc=pin_loc,
            pin_label=pin_label,
        )

    # In headless mode the reset net is otherwise left floating, which makes PC undefined.
    cpu.append(comp("Constant", "0", (1160, 560), {"facing": "east", "width": "1", "value": "0x0"}))
    cpu.append(tunnel((1190, 560), "RST", width=1, facing="west"))
    cpu.append(wire((1160, 560), (1190, 560)))

    # Add a simple tick counter so headless simulation can stop automatically once bit 6 goes high.
    counter_loc = (1180, 980)
    cpu.append(
        comp(
            "Counter",
            "4",
            counter_loc,
            {
                "width": "8",
                "max": "0xff",
                "ongoal": "wrap",
                "trigger": "rising",
                "behavior": "old",
                "label": "",
                "labelfont": "Dialog plain 12",
                "labelcolor": "#000000",
            },
        )
    )
    cpu.append(tunnel((counter_loc[0] - 20, counter_loc[1] + 50), "CLK", width=1, facing="north"))
    cpu.append(wire((counter_loc[0] - 20, counter_loc[1] + 20), (counter_loc[0] - 20, counter_loc[1] + 50)))

    split_loc = (1240, 980)
    cpu.append(splitter(split_loc, facing="east", incoming=8, bit_values=["6"]))
    cpu.append(wire((counter_loc[0] + 30, counter_loc[1]), split_loc))
    cpu.append(output_pin((1340, 520), "halt", 1))
    cpu.append(wire((split_loc[0] + 20, split_loc[1]), (1310, 980)))
    cpu.append(wire((1310, 980), (1310, 520)))
    cpu.append(wire((1310, 520), (1340, 520)))

    tree.write(OUT, encoding="utf-8", xml_declaration=True)
    print(OUT)


if __name__ == "__main__":
    main()
