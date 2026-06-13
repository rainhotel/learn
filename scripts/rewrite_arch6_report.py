from pathlib import Path

from docx import Document


SOURCE = Path(r"D:\moniC\project\learn\计组实验6实验报告.docx")
OUTPUT = Path(r"D:\moniC\project\learn\03-outputs\计组实验6实验报告_改写版.docx")


REPLACEMENTS = {
    "头歌实践平台课程：动手画CPU    https://www.educoder.net/paths/hvbz6g9i": "头歌实践平台课程：动手画 CPU    https://www.educoder.net/paths/hvbz6g9i",
    "实验包下载https://gitee.com/totalcontrol/hustzc/": "实验包下载地址：https://gitee.com/totalcontrol/hustzc/",
    "汉字机内码查询网站https://www.qqxiuzi.cn/bianma/zifuji.php": "汉字机内码查询网站：https://www.qqxiuzi.cn/bianma/zifuji.php",
    "了解 MIPS 寄存器文件基本概念，进一步熟悉多路选择器、译码器、解复用器等 Logisim 组件的使用，并利用相关组件构建 MIPS 寄存器文件。": "理解 MIPS 寄存器文件的基本原理，进一步熟悉多路选择器、译码器、解复用器等 Logisim 元件的使用方法，并能够利用这些组件完成简化 MIPS 寄存器文件的搭建。",
    "MIPS寄存器文件设计": "MIPS 寄存器文件设计",
    "利用 Logisim 平台构建一个简化的 MIPS 寄存器文件，内部包含4个32位寄存器，0号寄存器值恒为0，其具体引脚与功能描述如下表。": "使用 Logisim 平台设计一个简化的 MIPS 寄存器文件。该寄存器文件内部包含 4 个 32 位寄存器，其中 0 号寄存器的值始终保持为 0，具体引脚及功能要求如下表所示。",
    "当现有存储芯片的位数不足存储器所需数据位宽时需要位扩展，芯片存储容量达不到目标存储单元数量时需要字扩展；若同时位宽和容量都不满足，就需要同时进行字扩展和位扩展，通过多片存储芯片组合拼接，构建出满足系统要求的大容量、指定位宽的存储系统。": "当单片存储器的数据位宽小于系统要求时，就需要进行位扩展；当单片存储器能够提供的存储单元数量不足时，就需要进行字扩展。如果位宽和容量两个方面都达不到要求，那么就要同时采用字扩展和位扩展，通过多片芯片组合，最终构成满足系统容量和数据宽度要求的存储系统。",
    "寄存器常用引脚包含数据输入引脚、数据输出引脚、时钟脉冲CLK引脚、清零/复位引脚、置数/使能控制引脚、并行加载控制引脚等；寄存器引脚个数主要和存储的二进制位数、输入输出方式（并行/串行）、控制信号种类有关，位数越多数据输入输出引脚越多，功能控制越复杂则控制引脚也越多。": "寄存器的常用引脚一般包括数据输入端、数据输出端、时钟 CLK 端、清零或复位端、使能控制端以及并行加载等控制端。引脚数量主要取决于寄存器保存的数据位数、输入输出方式以及控制功能的复杂程度。通常位数越大，数据相关引脚越多；控制功能越丰富，所需的控制引脚也会相应增加。",
}


def main() -> None:
    doc = Document(str(SOURCE))
    for para in doc.paragraphs:
        text = para.text.strip()
        if text in REPLACEMENTS:
            para.text = REPLACEMENTS[text]
    doc.save(str(OUTPUT))
    print(OUTPUT)


if __name__ == "__main__":
    main()
