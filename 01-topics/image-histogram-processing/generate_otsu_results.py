from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
OUTPUT_DIR = ROOT / "03-outputs" / "image-histogram-processing"

IMAGES = [
    {
        "filename": "Fig1040(a)(large_septagon_gaussian_noise_mean_0_std_50_added).tif",
        "label": "Fig1040(a)",
        "description": "bright object with gaussian noise",
    },
    {
        "filename": "Fig1046(a)(septagon_noisy_shaded).tif",
        "label": "Fig1046(a)",
        "description": "septagon with shading and noise",
    },
]


def otsu_threshold(image: np.ndarray) -> int:
    hist = np.bincount(image.ravel(), minlength=256).astype(np.float64)
    prob = hist / hist.sum()
    omega = np.cumsum(prob)
    mu = np.cumsum(prob * np.arange(256))
    mu_total = mu[-1]
    denominator = omega * (1.0 - omega)
    sigma_between = (mu_total * omega - mu) ** 2 / np.maximum(denominator, 1e-12)
    sigma_between[(omega <= 0) | (omega >= 1)] = -1
    return int(np.argmax(sigma_between))


def make_binary(image: np.ndarray, threshold: int) -> np.ndarray:
    bright_mask = image > threshold
    dark_mask = ~bright_mask

    bright_mean = float(image[bright_mask].mean()) if bright_mask.any() else 0.0
    dark_mean = float(image[dark_mask].mean()) if dark_mask.any() else 0.0

    # Show the brighter class in white so the segmented object stays intuitive.
    foreground = bright_mask if bright_mean >= dark_mean else dark_mask
    return foreground.astype(np.uint8) * 255


def save_panel(image_path: Path, label: str, description: str) -> dict:
    image = np.array(Image.open(image_path).convert("L"))
    threshold = otsu_threshold(image)
    binary = make_binary(image, threshold)
    hist = np.bincount(image.ravel(), minlength=256)

    fig, axes = plt.subplots(1, 3, figsize=(14, 4.6))
    axes[0].imshow(image, cmap="gray", vmin=0, vmax=255)
    axes[0].set_title(f"{label} original")
    axes[0].axis("off")

    axes[1].plot(np.arange(256), hist, color="black", linewidth=1)
    axes[1].axvline(threshold, color="crimson", linestyle="--", linewidth=1.5)
    axes[1].set_title(f"histogram\nOtsu threshold = {threshold} ({threshold / 255:.4f})")
    axes[1].set_xlabel("gray level")
    axes[1].set_ylabel("pixel count")

    axes[2].imshow(binary, cmap="gray", vmin=0, vmax=255)
    axes[2].set_title(f"{label} segmented")
    axes[2].axis("off")

    fig.suptitle(f"{label} - {description}", fontsize=14)
    fig.tight_layout()

    output_path = OUTPUT_DIR / f"{image_path.stem}_otsu_panel.png"
    fig.savefig(output_path, dpi=180, bbox_inches="tight")
    plt.close(fig)

    binary_path = OUTPUT_DIR / f"{image_path.stem}_otsu_binary.png"
    Image.fromarray(binary).save(binary_path)

    return {
        "label": label,
        "filename": image_path.name,
        "threshold": threshold,
        "threshold_normalized": threshold / 255,
        "output_panel": output_path.name,
        "output_binary": binary_path.name,
    }


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    records = []

    for item in IMAGES:
        image_path = ROOT / item["filename"]
        records.append(save_panel(image_path, item["label"], item["description"]))

    lines = [
        "# Otsu 阈值分割结果",
        "",
        "## 输出文件",
        "",
    ]

    for record in records:
        lines.extend(
            [
                f"### {record['label']}",
                "",
                f"- 原始图像：`{record['filename']}`",
                f"- Otsu 阈值：`{record['threshold']}`",
                f"- 归一化阈值：`{record['threshold_normalized']:.4f}`",
                f"- 三联对比图：`{record['output_panel']}`",
                f"- 二值结果图：`{record['output_binary']}`",
                "",
            ]
        )

    (OUTPUT_DIR / "README.md").write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    main()
