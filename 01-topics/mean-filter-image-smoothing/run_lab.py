from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Iterable

import numpy as np
from numpy.lib.stride_tricks import sliding_window_view
from PIL import Image, ImageDraw, ImageFont, ImageOps


TOPIC_DIR = Path(__file__).resolve().parent
IMAGE_DIR = TOPIC_DIR / "images"
OUTPUT_DIR = TOPIC_DIR / "output"
FIGURE_DIR = OUTPUT_DIR / "figures"
METRICS_PATH = OUTPUT_DIR / "metrics.json"

RNG = np.random.default_rng(20260426)


def ensure_dirs() -> None:
    FIGURE_DIR.mkdir(parents=True, exist_ok=True)


def load_gray_image(path: Path) -> np.ndarray:
    with Image.open(path) as image:
        return np.asarray(image.convert("L"), dtype=np.float64) / 255.0


def save_gray_image(path: Path, image: np.ndarray) -> None:
    clipped = np.clip(image, 0.0, 1.0)
    Image.fromarray(np.round(clipped * 255).astype(np.uint8), mode="L").save(path)


def arr_to_pil(image: np.ndarray) -> Image.Image:
    clipped = np.clip(image, 0.0, 1.0)
    return Image.fromarray(np.round(clipped * 255).astype(np.uint8), mode="L").convert("RGB")


def wrap_text(text: str, width: int) -> list[str]:
    words = text.split()
    if not words:
        return [""]
    lines: list[str] = []
    current = words[0]
    for word in words[1:]:
        trial = f"{current} {word}"
        if len(trial) <= width:
            current = trial
        else:
            lines.append(current)
            current = word
    lines.append(current)
    return lines


def make_labeled_tile(
    image: Image.Image,
    label: str,
    cell_width: int = 240,
    image_height: int = 180,
) -> Image.Image:
    font = ImageFont.load_default()
    lines = wrap_text(label, 24)
    title_height = 18 * len(lines) + 10
    tile = Image.new("RGB", (cell_width, image_height + title_height + 12), "white")
    padded = ImageOps.contain(image, (cell_width - 18, image_height - 18))
    x = (cell_width - padded.width) // 2
    y = title_height + (image_height - padded.height) // 2
    tile.paste(padded, (x, y))
    tile = ImageOps.expand(tile, border=1, fill="#B8BCC2")
    draw = ImageDraw.Draw(tile)
    text_y = 6
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=font)
        text_x = (tile.width - (bbox[2] - bbox[0])) // 2
        draw.text((text_x, text_y), line, fill="black", font=font)
        text_y += 14
    return tile


def make_panel(
    title: str,
    items: Iterable[tuple[str, Image.Image]],
    columns: int,
    out_path: Path,
    subtitle: str | None = None,
) -> None:
    item_list = list(items)
    if not item_list:
        raise ValueError("No panel items were provided.")

    tiles = [make_labeled_tile(image, label) for label, image in item_list]
    rows = math.ceil(len(tiles) / columns)
    tile_w = tiles[0].width
    tile_h = tiles[0].height
    gutter = 14
    title_height = 46 if subtitle else 30
    canvas_w = columns * tile_w + (columns + 1) * gutter
    canvas_h = rows * tile_h + (rows + 1) * gutter + title_height
    canvas = Image.new("RGB", (canvas_w, canvas_h), "white")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()

    draw.text((gutter, 8), title, fill="black", font=font)
    if subtitle:
        draw.text((gutter, 24), subtitle, fill="#444444", font=font)

    for index, tile in enumerate(tiles):
        row = index // columns
        col = index % columns
        x = gutter + col * (tile_w + gutter)
        y = title_height + gutter + row * (tile_h + gutter)
        canvas.paste(tile, (x, y))

    canvas.save(out_path)


def convolve(image: np.ndarray, kernel: np.ndarray) -> np.ndarray:
    kh, kw = kernel.shape
    pad_h = kh // 2
    pad_w = kw // 2
    padded = np.pad(image, ((pad_h, pad_h), (pad_w, pad_w)), mode="edge")
    windows = sliding_window_view(padded, (kh, kw))
    return np.tensordot(windows, kernel, axes=((2, 3), (0, 1)))


def arithmetic_mean_filter(image: np.ndarray, size: int) -> np.ndarray:
    kernel = np.full((size, size), 1.0 / (size * size), dtype=np.float64)
    return convolve(image, kernel)


def pascal_row(size: int) -> np.ndarray:
    row = [1]
    for _ in range(size - 1):
        row = [1] + [row[i] + row[i + 1] for i in range(len(row) - 1)] + [1]
    return np.asarray(row, dtype=np.float64)


def weighted_mean_kernel(size: int) -> np.ndarray:
    row = pascal_row(size)
    kernel = np.outer(row, row)
    return kernel / kernel.sum()


def weighted_mean_filter(image: np.ndarray, size: int) -> np.ndarray:
    return convolve(image, weighted_mean_kernel(size))


def gaussian_kernel(size: int, sigma: float) -> np.ndarray:
    radius = size // 2
    axis = np.arange(-radius, radius + 1, dtype=np.float64)
    xx, yy = np.meshgrid(axis, axis)
    kernel = np.exp(-(xx**2 + yy**2) / (2.0 * sigma**2))
    kernel /= kernel.sum()
    return kernel


def gaussian_filter(image: np.ndarray, size: int, sigma: float) -> np.ndarray:
    return convolve(image, gaussian_kernel(size, sigma))


def geometric_mean_filter(image: np.ndarray, size: int) -> np.ndarray:
    eps = 1e-6
    pad = size // 2
    padded = np.pad(image, ((pad, pad), (pad, pad)), mode="edge")
    windows = sliding_window_view(padded, (size, size))
    return np.exp(np.mean(np.log(windows + eps), axis=(2, 3)))


def harmonic_mean_filter(image: np.ndarray, size: int) -> np.ndarray:
    eps = 1e-6
    pad = size // 2
    padded = np.pad(image, ((pad, pad), (pad, pad)), mode="edge")
    windows = sliding_window_view(padded, (size, size))
    return (size * size) / np.sum(1.0 / (windows + eps), axis=(2, 3))


def contra_harmonic_mean_filter(image: np.ndarray, size: int, q: float) -> np.ndarray:
    eps = 1e-6
    pad = size // 2
    padded = np.pad(image, ((pad, pad), (pad, pad)), mode="edge")
    windows = sliding_window_view(padded, (size, size))
    numerator = np.sum((windows + eps) ** (q + 1.0), axis=(2, 3))
    denominator = np.sum((windows + eps) ** q, axis=(2, 3))
    return numerator / (denominator + eps)


def bilateral_filter(
    image: np.ndarray,
    size: int = 5,
    sigma_space: float = 2.0,
    sigma_range: float = 0.1,
) -> np.ndarray:
    radius = size // 2
    axis = np.arange(-radius, radius + 1, dtype=np.float64)
    xx, yy = np.meshgrid(axis, axis)
    spatial = np.exp(-(xx**2 + yy**2) / (2.0 * sigma_space**2))

    padded = np.pad(image, ((radius, radius), (radius, radius)), mode="edge")
    windows = sliding_window_view(padded, (size, size))
    center = image[:, :, None, None]
    range_weights = np.exp(-((windows - center) ** 2) / (2.0 * sigma_range**2))
    weights = spatial[None, None, :, :] * range_weights
    return np.sum(weights * windows, axis=(2, 3)) / np.sum(weights, axis=(2, 3))


def add_gaussian_noise(image: np.ndarray, variance: float) -> np.ndarray:
    sigma = math.sqrt(variance)
    noisy = image + RNG.normal(0.0, sigma, image.shape)
    return np.clip(noisy, 0.0, 1.0)


def mse(reference: np.ndarray, image: np.ndarray) -> float:
    return float(np.mean((reference - image) ** 2))


def psnr(reference: np.ndarray, image: np.ndarray) -> float:
    error = mse(reference, image)
    if error <= 1e-12:
        return 99.0
    return float(10.0 * math.log10(1.0 / error))


def kernel_surface_image(kernel: np.ndarray, title: str) -> Image.Image:
    width, height = 360, 220
    canvas = Image.new("RGB", (width, height), "white")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    draw.text((8, 6), title, fill="black", font=font)

    tile_w = 28
    tile_h = 14
    height_scale = 110.0 / kernel.max()
    origin_x = 180
    origin_y = 160

    def iso(x: float, y: float, z: float) -> tuple[int, int]:
        sx = origin_x + int((x - y) * tile_w)
        sy = origin_y + int((x + y) * tile_h / 2 - z)
        return sx, sy

    norm = kernel / kernel.max()
    cells: list[tuple[int, int, float]] = []
    for row in range(kernel.shape[0]):
        for col in range(kernel.shape[1]):
            cells.append((row, col, norm[row, col]))
    cells.sort(key=lambda item: item[0] + item[1])

    for row, col, value in cells:
        z = value * height_scale
        top = [iso(col, row, z), iso(col + 1, row, z), iso(col + 1, row + 1, z), iso(col, row + 1, z)]
        left = [iso(col, row + 1, 0), iso(col, row + 1, z), iso(col, row, z), iso(col, row, 0)]
        right = [iso(col + 1, row, 0), iso(col + 1, row, z), iso(col + 1, row + 1, z), iso(col + 1, row + 1, 0)]

        shade = int(120 + 100 * value)
        top_color = (shade, shade, 255)
        left_color = (max(0, shade - 40), max(0, shade - 40), 200)
        right_color = (max(0, shade - 20), max(0, shade - 20), 225)

        draw.polygon(left, fill=left_color, outline="#5A5A7A")
        draw.polygon(right, fill=right_color, outline="#5A5A7A")
        draw.polygon(top, fill=top_color, outline="#4A4A66")

        value_text = f"{kernel[row, col]:.3f}"
        bbox = draw.textbbox((0, 0), value_text, font=font)
        tx = (top[0][0] + top[2][0] - (bbox[2] - bbox[0])) // 2
        ty = (top[0][1] + top[2][1] - (bbox[3] - bbox[1])) // 2
        draw.text((tx, ty), value_text, fill="black", font=font)

    return canvas


def heatmap_image(kernel: np.ndarray, title: str) -> Image.Image:
    size = 260
    canvas = Image.new("RGB", (size, size + 24), "white")
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    draw.text((8, 6), title, fill="black", font=font)

    grid_top = 28
    grid_size = size - 24
    cell = grid_size // kernel.shape[0]
    kernel_norm = kernel / kernel.max()
    for row in range(kernel.shape[0]):
        for col in range(kernel.shape[1]):
            value = kernel[row, col]
            shade = int(255 - 180 * kernel_norm[row, col])
            color = (shade, shade, 255)
            x0 = col * cell + 8
            y0 = row * cell + grid_top
            x1 = x0 + cell
            y1 = y0 + cell
            draw.rectangle((x0, y0, x1, y1), fill=color, outline="#666688")
            text = f"{value:.3f}"
            bbox = draw.textbbox((0, 0), text, font=font)
            tx = x0 + (cell - (bbox[2] - bbox[0])) // 2
            ty = y0 + (cell - (bbox[3] - bbox[1])) // 2
            draw.text((tx, ty), text, fill="black", font=font)
    return canvas


def save_task_figure(
    filename: str,
    title: str,
    images: Iterable[tuple[str, np.ndarray | Image.Image]],
    columns: int,
    subtitle: str | None = None,
) -> None:
    panel_items: list[tuple[str, Image.Image]] = []
    for label, image in images:
        panel_items.append((label, image if isinstance(image, Image.Image) else arr_to_pil(image)))
    make_panel(title, panel_items, columns, FIGURE_DIR / filename, subtitle=subtitle)


def run() -> None:
    ensure_dirs()

    fig0507a = load_gray_image(IMAGE_DIR / "Fig0507(a)(ckt-board-orig).tif")
    fig0507b = load_gray_image(IMAGE_DIR / "Fig0507(b)(ckt-board-gauss-var-400).tif")
    fig0508a = load_gray_image(IMAGE_DIR / "Fig0508(a)(circuit-board-pepper-prob-pt1).tif")
    fig0508b = load_gray_image(IMAGE_DIR / "Fig0508(b)(circuit-board-salt-prob-pt1).tif")
    fig0512b = load_gray_image(IMAGE_DIR / "Fig0512(b)(ckt-uniform-plus-saltpepr-prob-pt1).tif")

    metrics: dict[str, object] = {
        "task1": {},
        "task2": {},
        "task3": {},
        "task4": {},
        "task5": {},
        "task6": {},
    }

    same_sigma = 1.0
    size_list = [3, 5, 7]
    sigma_list = [0.6, 1.0, 1.6]
    fixed_size = 5

    task1_size_images = [("Noisy Fig0507(b)", fig0507b)]
    task1_sigma_images = [("Noisy Fig0507(b)", fig0507b)]
    task1_kernel_items: list[tuple[str, Image.Image]] = []
    task1_heatmap_items: list[tuple[str, Image.Image]] = []
    task1_metrics: dict[str, dict[str, float]] = {}

    for size in size_list:
        kernel = gaussian_kernel(size, same_sigma)
        filtered = gaussian_filter(fig0507b, size, same_sigma)
        key = f"size_{size}_sigma_{same_sigma:.1f}"
        task1_metrics[key] = {
            "psnr_vs_orig": round(psnr(fig0507a, filtered), 4),
            "mse_vs_orig": round(mse(fig0507a, filtered), 6),
        }
        task1_size_images.append((f"{size}x{size}, sigma=1.0\nPSNR={psnr(fig0507a, filtered):.2f} dB", filtered))
        task1_kernel_items.append((f"{size}x{size}, sigma=1.0", kernel_surface_image(kernel, f"{size}x{size}, sigma=1.0")))
        task1_heatmap_items.append((f"{size}x{size}, sigma=1.0", heatmap_image(kernel, f"{size}x{size} heatmap")))

    for sigma in sigma_list:
        kernel = gaussian_kernel(fixed_size, sigma)
        filtered = gaussian_filter(fig0507b, fixed_size, sigma)
        key = f"size_{fixed_size}_sigma_{sigma:.1f}"
        task1_metrics[key] = {
            "psnr_vs_orig": round(psnr(fig0507a, filtered), 4),
            "mse_vs_orig": round(mse(fig0507a, filtered), 6),
        }
        task1_sigma_images.append((f"5x5, sigma={sigma:.1f}\nPSNR={psnr(fig0507a, filtered):.2f} dB", filtered))

    metrics["task1"] = task1_metrics

    make_panel(
        "Task 1A - Gaussian kernel surface views",
        task1_kernel_items,
        columns=3,
        out_path=FIGURE_DIR / "task1_kernel_surface.png",
        subtitle="Different template sizes with the same sigma = 1.0",
    )
    make_panel(
        "Task 1B - Gaussian kernel heatmaps",
        task1_heatmap_items,
        columns=3,
        out_path=FIGURE_DIR / "task1_kernel_heatmap.png",
        subtitle="Coefficient distributions for the same sigma = 1.0",
    )
    save_task_figure(
        "task1_filter_size_compare.png",
        "Task 1C - Gaussian filtering with different template sizes",
        task1_size_images,
        columns=4,
        subtitle="Input image: Fig0507(b), reference for PSNR: Fig0507(a)",
    )
    save_task_figure(
        "task1_filter_sigma_compare.png",
        "Task 1D - Gaussian filtering with different sigma values",
        task1_sigma_images,
        columns=4,
        subtitle="Template size fixed at 5x5",
    )

    task2_gaussian = gaussian_filter(fig0507b, 5, 1.0)
    task2_bilateral = bilateral_filter(fig0507b, size=5, sigma_space=2.0, sigma_range=0.10)
    metrics["task2"] = {
        "gaussian_psnr_vs_orig": round(psnr(fig0507a, task2_gaussian), 4),
        "bilateral_psnr_vs_orig": round(psnr(fig0507a, task2_bilateral), 4),
    }
    save_task_figure(
        "task2_bilateral_vs_gaussian.png",
        "Task 2 - Bilateral filtering versus Gaussian filtering",
        [
            ("Clean Fig0507(a)", fig0507a),
            ("Noisy Fig0507(b)", fig0507b),
            (f"Gaussian 5x5, sigma=1.0\nPSNR={psnr(fig0507a, task2_gaussian):.2f} dB", task2_gaussian),
            (f"Bilateral 5x5\nPSNR={psnr(fig0507a, task2_bilateral):.2f} dB", task2_bilateral),
        ],
        columns=4,
        subtitle="Bilateral filtering should reduce noise while preserving board edges better",
    )

    task3_noise_vars = [0.001, 0.005, 0.01]
    task3_fixed_size = 5
    task3_size_list = [3, 5, 7]
    task3_metrics: dict[str, object] = {}

    for variance in task3_noise_vars:
        noisy = add_gaussian_noise(fig0507a, variance)
        arithmetic = arithmetic_mean_filter(noisy, task3_fixed_size)
        weighted = weighted_mean_filter(noisy, task3_fixed_size)
        gaussian = gaussian_filter(noisy, task3_fixed_size, 1.0)
        key = f"variance_{variance:.3f}"
        task3_metrics[key] = {
            "noisy_psnr": round(psnr(fig0507a, noisy), 4),
            "arithmetic_psnr": round(psnr(fig0507a, arithmetic), 4),
            "weighted_psnr": round(psnr(fig0507a, weighted), 4),
            "gaussian_psnr": round(psnr(fig0507a, gaussian), 4),
        }
        save_task_figure(
            f"task3_filter_compare_var_{str(variance).replace('.', '_')}.png",
            f"Task 3A - Filter comparison at Gaussian noise variance {variance:.3f}",
            [
                ("Clean Fig0507(a)", fig0507a),
                (f"Noisy image\nPSNR={psnr(fig0507a, noisy):.2f} dB", noisy),
                (f"Arithmetic mean 5x5\nPSNR={psnr(fig0507a, arithmetic):.2f} dB", arithmetic),
                (f"Weighted mean 5x5\nPSNR={psnr(fig0507a, weighted):.2f} dB", weighted),
                (f"Gaussian mean 5x5\nPSNR={psnr(fig0507a, gaussian):.2f} dB", gaussian),
            ],
            columns=5,
            subtitle="The same template size is used to compare three mean-type filters",
        )

    task3_noisy_mid = add_gaussian_noise(fig0507a, 0.005)
    for filter_name in ("arithmetic", "weighted", "gaussian"):
        images: list[tuple[str, np.ndarray]] = [("Clean Fig0507(a)", fig0507a), ("Noisy var=0.005", task3_noisy_mid)]
        per_filter_metrics: dict[str, float] = {}
        for size in task3_size_list:
            if filter_name == "arithmetic":
                filtered = arithmetic_mean_filter(task3_noisy_mid, size)
            elif filter_name == "weighted":
                filtered = weighted_mean_filter(task3_noisy_mid, size)
            else:
                filtered = gaussian_filter(task3_noisy_mid, size, 1.0)
            score = psnr(fig0507a, filtered)
            per_filter_metrics[f"size_{size}"] = round(score, 4)
            images.append((f"{filter_name.title()} {size}x{size}\nPSNR={score:.2f} dB", filtered))
        task3_metrics[f"{filter_name}_size_compare"] = per_filter_metrics
        save_task_figure(
            f"task3_{filter_name}_size_compare.png",
            f"Task 3B - {filter_name.title()} mean filter with different template sizes",
            images,
            columns=5,
            subtitle="Noise variance is fixed at 0.005 to isolate the template-size effect",
        )
    metrics["task3"] = task3_metrics

    task4_arithmetic = arithmetic_mean_filter(fig0507b, 5)
    task4_geometric = geometric_mean_filter(fig0507b, 5)
    metrics["task4"] = {
        "arithmetic_psnr_vs_orig": round(psnr(fig0507a, task4_arithmetic), 4),
        "geometric_psnr_vs_orig": round(psnr(fig0507a, task4_geometric), 4),
    }
    save_task_figure(
        "task4_arithmetic_vs_geometric.png",
        "Task 4 - Arithmetic mean versus geometric mean filtering",
        [
            ("Clean Fig0507(a)", fig0507a),
            ("Noisy Fig0507(b)", fig0507b),
            (f"Arithmetic mean 5x5\nPSNR={psnr(fig0507a, task4_arithmetic):.2f} dB", task4_arithmetic),
            (f"Geometric mean 5x5\nPSNR={psnr(fig0507a, task4_geometric):.2f} dB", task4_geometric),
        ],
        columns=4,
        subtitle="Geometric mean often preserves fine structure slightly better than arithmetic mean",
    )

    q_values_pepper = [0.5, 1.5, 3.0]
    q_values_salt = [-0.5, -1.5, -3.0]
    task5_metrics: dict[str, object] = {"pepper": {}, "salt": {}}

    pepper_images: list[tuple[str, np.ndarray]] = [("Reference Fig0507(a)", fig0507a), ("Pepper-noise image", fig0508a)]
    for q in q_values_pepper:
        filtered = contra_harmonic_mean_filter(fig0508a, 3, q)
        score = psnr(fig0507a, filtered)
        task5_metrics["pepper"][f"Q_{q}"] = round(score, 4)
        pepper_images.append((f"Q={q:.1f}\nPSNR={score:.2f} dB", filtered))

    salt_images: list[tuple[str, np.ndarray]] = [("Reference Fig0507(a)", fig0507a), ("Salt-noise image", fig0508b)]
    for q in q_values_salt:
        filtered = contra_harmonic_mean_filter(fig0508b, 3, q)
        score = psnr(fig0507a, filtered)
        task5_metrics["salt"][f"Q_{q}"] = round(score, 4)
        salt_images.append((f"Q={q:.1f}\nPSNR={score:.2f} dB", filtered))

    metrics["task5"] = task5_metrics
    save_task_figure(
        "task5_pepper_q_compare.png",
        "Task 5A - Contra-harmonic mean filtering on pepper noise",
        pepper_images,
        columns=5,
        subtitle="Positive Q values should suppress black pepper points more effectively",
    )
    save_task_figure(
        "task5_salt_q_compare.png",
        "Task 5B - Contra-harmonic mean filtering on salt noise",
        salt_images,
        columns=5,
        subtitle="Negative Q values should suppress white salt points more effectively",
    )

    task6_arithmetic = arithmetic_mean_filter(fig0512b, 3)
    task6_geometric = geometric_mean_filter(fig0512b, 3)
    task6_harmonic = harmonic_mean_filter(fig0512b, 3)
    task6_contra = contra_harmonic_mean_filter(fig0512b, 3, 1.5)
    metrics["task6"] = {
        "contra_harmonic_q": 1.5,
        "image_note": "Fig0512(b) contains mixed noise, so contra-harmonic filtering cannot remove both salt and pepper artifacts simultaneously.",
    }
    save_task_figure(
        "task6_mixed_noise_compare.png",
        "Task 6 - Mean-filter comparison on Fig0512(b)",
        [
            ("Noisy Fig0512(b)", fig0512b),
            ("Arithmetic mean 3x3", task6_arithmetic),
            ("Geometric mean 3x3", task6_geometric),
            ("Harmonic mean 3x3", task6_harmonic),
            ("Contra-harmonic 3x3, Q=1.5", task6_contra),
        ],
        columns=5,
        subtitle="Mixed salt-pepper plus uniform noise exposes the strengths and weaknesses of each mean filter",
    )

    save_gray_image(FIGURE_DIR / "task2_bilateral_result_only.png", task2_bilateral)
    save_gray_image(FIGURE_DIR / "task4_geometric_result_only.png", task4_geometric)

    with METRICS_PATH.open("w", encoding="utf-8") as handle:
        json.dump(metrics, handle, ensure_ascii=False, indent=2)

    print(f"Generated figures in: {FIGURE_DIR}")
    print(f"Saved metrics to: {METRICS_PATH}")


if __name__ == "__main__":
    run()
