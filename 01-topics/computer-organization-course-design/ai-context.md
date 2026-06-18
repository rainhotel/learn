# AI Context

## Current Stage

- User is stuck in Logisim for the computer organization course design.
- The two selected tasks are:
  - Experiment 5-1: single-bus CPU, fixed-length 3-stage timing.
  - Experiment 5-3: single-bus CPU, modern timing / microprogrammed controller.

## Local Files

- Requirement doc: `D:\moniC\project\learn\ver2-课程设计文件\ver2-课程设计文件\计组课设-题目与要求设计任务书.docx`
- Cover template: `D:\moniC\project\learn\ver2-课程设计文件\ver2-课程设计文件\课程设计封面.docx`
- Main resource dir: `D:\moniC\project\learn\hustzc\7.单总线CPU\单总线实验资料包(愚人节版)`

## Key Findings

- `MipsOnBusCpu-3.circ` is a 3-stage timing template.
- `MipsOnBusCpu-3-exp5-1.circ` contains completed reference subcircuits for experiment 5-1.
- `MipsOnBusCpu-1.circ` has the modern timing microprogram structure but the control ROM is effectively empty.
- Microinstruction layout for experiment 5-3 is:
  `22 control bits + P0/P1/P2 + 5-bit next address`.

## Open Risks

- The proposed experiment 5-3 microprogram is derived from the local data path and workbook bit order. It should be tested in Logisim with the provided `sort-5.hex`.
- If branch behavior fails, inspect `IR(A)out` branch-target path and the `beq分支` mux input.

