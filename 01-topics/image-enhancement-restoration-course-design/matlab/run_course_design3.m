function run_course_design3()
% 图像增强与复原算法综合应用课程设计
%
% 运行方式：
%   在 MATLAB 中打开本文件所在目录，执行 run_course_design3
%
% 输出目录：
%   D:\moniC\project\learn\03-outputs\image-enhancement-restoration-course-design

clearvars;
close all;
clc;

set(0, 'DefaultFigureVisible', 'off');

scriptDir = fileparts(mfilename('fullpath'));
topicDir = fileparts(scriptDir);
rootDir = fileparts(fileparts(topicDir));
inputDir = fullfile(topicDir, 'input-images');
outputDir = fullfile(rootDir, '03-outputs', 'image-enhancement-restoration-course-design');

if ~exist(outputDir, 'dir')
    mkdir(outputDir);
end

fprintf('Course design 3 automation started.\n');
fprintf('Input dir : %s\n', inputDir);
fprintf('Output dir: %s\n\n', outputDir);

dogDistorted = load_gray_image(fullfile(inputDir, 'dogDistorted.bmp'));
dogOriginal = load_gray_image(fullfile(inputDir, 'dogOriginal.bmp'));
enhancement = run_enhancement_task(dogDistorted, dogOriginal, outputDir);

forestBlur = load_rgb_image(fullfile(inputDir, 'restoration_fig1_forest_blur.jpg'));
waterfallDark = load_rgb_image(fullfile(inputDir, 'restoration_fig2_waterfall_dark.jpg'));
restorationFig1 = run_forest_restoration_task(forestBlur, outputDir);
restorationFig2 = run_waterfall_restoration_task(waterfallDark, outputDir);

summary = struct();
summary.generated_at = char(datetime('now', 'Format', 'yyyy-MM-dd HH:mm:ss'));
summary.assumption = '默认按学号尾数为偶数：增强任务使用 dog 图组，复原任务正文选择图1；脚本同时输出图2备选结果。';
summary.enhancement = enhancement;
summary.restoration_fig1 = restorationFig1;
summary.restoration_fig2 = restorationFig2;

write_json(summary, fullfile(outputDir, 'summary.json'));
write_metrics_csv(summary, fullfile(outputDir, 'metrics.csv'));
write_report_notes(summary, fullfile(outputDir, 'report-notes.txt'));

fprintf('\nFinished.\n');
fprintf('Key output files:\n');
fprintf('- %s\n', fullfile(outputDir, 'enhancement_comparison.png'));
fprintf('- %s\n', fullfile(outputDir, 'restoration_fig1_comparison.png'));
fprintf('- %s\n', fullfile(outputDir, 'metrics.csv'));
fprintf('- %s\n', fullfile(outputDir, 'report-notes.txt'));
end

function result = run_enhancement_task(noisy, reference, outputDir)
fprintf('Running enhancement task...\n');

metricsBefore = reference_metrics(reference, noisy);

% 频谱诊断显示周期噪声主峰主要位于中心左右约 71 像素、上下约 74 像素。
% 先用高斯陷波滤波器抑制周期噪声，再用空间域滤波削弱随机噪声。
notchOffsets = [
     0, -71
     0,  71
   -74,   0
    74,   0
];
notchRadius = 10;

[freqFiltered, notchFilter] = gaussian_notch_reject(noisy, notchOffsets, notchRadius);
medianFiltered = median_filter2(freqFiltered, 5);
meanFiltered = mean_filter2(medianFiltered, 3);
sharpened = high_boost(meanFiltered, 0.15);
enhanced = percentile_stretch(sharpened, 0.5, 99.5);
enhanced = clamp01(enhanced);

metricsAfter = reference_metrics(reference, enhanced);

imwrite(to_uint8(noisy), fullfile(outputDir, 'enhancement_01_distorted.png'));
imwrite(to_uint8(freqFiltered), fullfile(outputDir, 'enhancement_02_notch_filtered.png'));
imwrite(to_uint8(medianFiltered), fullfile(outputDir, 'enhancement_03_median_filtered.png'));
imwrite(to_uint8(enhanced), fullfile(outputDir, 'enhancement_04_final.png'));

save_enhancement_figures(reference, noisy, freqFiltered, medianFiltered, enhanced, ...
    notchFilter, metricsBefore, metricsAfter, outputDir);

result = struct();
result.method = 'Gaussian notch reject filter + 5x5 median filter + 3x3 mean filter + high-boost + percentile stretch';
result.notch_offsets = notchOffsets;
result.notch_radius = notchRadius;
result.metrics_before = metricsBefore;
result.metrics_after = metricsAfter;
result.output_final = 'enhancement_04_final.png';
result.output_comparison = 'enhancement_comparison.png';
result.output_spectrum = 'enhancement_spectrum_and_notch.png';

fprintf('Enhancement MSE: %.6f -> %.6f\n', metricsBefore.mse, metricsAfter.mse);
fprintf('Enhancement SNR: %.3f dB -> %.3f dB\n', metricsBefore.snr, metricsAfter.snr);
fprintf('Enhancement SSIM: %.4f -> %.4f\n\n', metricsBefore.ssim, metricsAfter.ssim);
end

function result = run_forest_restoration_task(rgb, outputDir)
fprintf('Running restoration task for fig1 forest image...\n');

y = rgb_to_luma(rgb);
metricsBefore = no_reference_metrics(y);

% 图1主要退化表现为模糊。这里采用近似高斯点扩散函数的维纳复原，
% 再用高提升锐化和百分位拉伸改善边缘与整体层次。
psfSize = 13;
psfSigma = 1.35;
wienerK = 0.004;
psf = gaussian_kernel2(psfSize, psfSigma);
deconvY = wiener_deconvolution(y, psf, wienerK);
deconvY = clamp01(0.72 * deconvY + 0.28 * y);
detail = deconvY - gaussian_blur2(deconvY, 1.0);
restoredY = clamp01(deconvY + 0.28 * detail);
restoredY = percentile_stretch(restoredY, 1.0, 99.0);

restoredRgb = replace_luma(rgb, y, restoredY);
metricsAfter = no_reference_metrics(restoredY);

imwrite(to_uint8(rgb), fullfile(outputDir, 'restoration_fig1_input.png'));
imwrite(to_uint8(restoredRgb), fullfile(outputDir, 'restoration_fig1_restored.png'));
save_restoration_figure(rgb, restoredRgb, metricsBefore, metricsAfter, ...
    '图1 模糊树林图像复原', fullfile(outputDir, 'restoration_fig1_comparison.png'));

result = struct();
result.method = 'Approximate Gaussian PSF Wiener restoration + high-boost sharpening + percentile stretch';
result.psf_size = psfSize;
result.psf_sigma = psfSigma;
result.wiener_K = wienerK;
result.metrics_before = metricsBefore;
result.metrics_after = metricsAfter;
result.output_final = 'restoration_fig1_restored.png';
result.output_comparison = 'restoration_fig1_comparison.png';

fprintf('Fig1 average gradient: %.6f -> %.6f\n', metricsBefore.average_gradient, metricsAfter.average_gradient);
fprintf('Fig1 Laplacian variance: %.6f -> %.6f\n\n', metricsBefore.laplacian_variance, metricsAfter.laplacian_variance);
end

function result = run_waterfall_restoration_task(rgb, outputDir)
fprintf('Running restoration task for fig2 waterfall image...\n');

y = rgb_to_luma(rgb);
metricsBefore = no_reference_metrics(y);

% 图2主要退化表现为光照不足。使用单尺度 Retinex 估计照度并校正，
% 再结合 gamma 变换，避免暗部提升后整体发灰。
illumination = gaussian_blur2(y, 18.0);
retinex = log(y + 1e-4) - log(illumination + 1e-4);
retinex = percentile_stretch(retinex, 1.0, 99.0);
gammaCorrected = y .^ 0.62;
restoredY = clamp01(0.62 * retinex + 0.38 * gammaCorrected);
restoredY = high_boost(restoredY, 0.18);
restoredY = percentile_stretch(restoredY, 0.5, 99.5);

restoredRgb = replace_luma(rgb, y, restoredY);
metricsAfter = no_reference_metrics(restoredY);

imwrite(to_uint8(rgb), fullfile(outputDir, 'restoration_fig2_input.png'));
imwrite(to_uint8(restoredRgb), fullfile(outputDir, 'restoration_fig2_restored.png'));
save_restoration_figure(rgb, restoredRgb, metricsBefore, metricsAfter, ...
    '图2 低照度瀑布图像复原', fullfile(outputDir, 'restoration_fig2_comparison.png'));

result = struct();
result.method = 'Single-scale Retinex illumination correction + gamma correction + high-boost sharpening';
result.retinex_sigma = 18.0;
result.gamma = 0.62;
result.metrics_before = metricsBefore;
result.metrics_after = metricsAfter;
result.output_final = 'restoration_fig2_restored.png';
result.output_comparison = 'restoration_fig2_comparison.png';

fprintf('Fig2 entropy: %.6f -> %.6f\n', metricsBefore.entropy, metricsAfter.entropy);
fprintf('Fig2 average gradient: %.6f -> %.6f\n\n', metricsBefore.average_gradient, metricsAfter.average_gradient);
end

function save_enhancement_figures(reference, noisy, freqFiltered, medianFiltered, enhanced, notchFilter, metricsBefore, metricsAfter, outputDir)
fig = figure('Position', [100, 100, 1400, 760]);
show_gray_subplot(2, 3, 1, reference, '清晰参考图');
show_gray_subplot(2, 3, 2, noisy, sprintf('退化图 MSE=%.4f', metricsBefore.mse));
show_gray_subplot(2, 3, 3, spectrum_image(noisy), '退化图频谱');
show_gray_subplot(2, 3, 4, freqFiltered, '频域陷波后');
show_gray_subplot(2, 3, 5, medianFiltered, '空间域滤波后');
show_gray_subplot(2, 3, 6, enhanced, sprintf('最终结果 MSE=%.4f', metricsAfter.mse));
sgtitle('图像增强：空间域与频率域结合去噪');
saveas(fig, fullfile(outputDir, 'enhancement_comparison.png'));
close(fig);

fig2 = figure('Position', [100, 100, 1200, 420]);
show_gray_subplot(1, 3, 1, spectrum_image(noisy), '退化图频谱');
show_gray_subplot(1, 3, 2, notchFilter, '高斯陷波滤波器');
show_gray_subplot(1, 3, 3, spectrum_image(freqFiltered), '滤波后频谱');
sgtitle('周期噪声频谱峰值抑制');
saveas(fig2, fullfile(outputDir, 'enhancement_spectrum_and_notch.png'));
close(fig2);

fig3 = figure('Position', [100, 100, 1100, 420]);
show_gray_subplot(1, 3, 1, abs(reference - noisy), '处理前绝对误差');
show_gray_subplot(1, 3, 2, abs(reference - enhanced), '处理后绝对误差');
show_gray_subplot(1, 3, 3, enhanced, sprintf('SSIM %.4f -> %.4f', metricsBefore.ssim, metricsAfter.ssim));
sgtitle('增强前后误差对比');
saveas(fig3, fullfile(outputDir, 'enhancement_error_comparison.png'));
close(fig3);
end

function save_restoration_figure(inputRgb, restoredRgb, metricsBefore, metricsAfter, figTitle, outPath)
fig = figure('Position', [100, 100, 1200, 520]);
subplot(1, 3, 1);
imshow(inputRgb);
title('退化图像', 'FontSize', 10);

subplot(1, 3, 2);
imshow(restoredRgb);
title('复原图像', 'FontSize', 10);

subplot(1, 3, 3);
axis off;
text(0.02, 0.88, sprintf('Entropy: %.3f -> %.3f', metricsBefore.entropy, metricsAfter.entropy), 'FontSize', 11);
text(0.02, 0.72, sprintf('Std: %.3f -> %.3f', metricsBefore.std, metricsAfter.std), 'FontSize', 11);
text(0.02, 0.56, sprintf('AvgGradient: %.5f -> %.5f', metricsBefore.average_gradient, metricsAfter.average_gradient), 'FontSize', 11);
text(0.02, 0.40, sprintf('LapVar: %.5f -> %.5f', metricsBefore.laplacian_variance, metricsAfter.laplacian_variance), 'FontSize', 11);
title('无参考指标', 'FontSize', 10);

sgtitle(figTitle);
saveas(fig, outPath);
close(fig);
end

function img = load_gray_image(path)
[x, map] = imread(path);
if ~isempty(map)
    rgb = indexed_to_rgb(x, map);
    img = rgb_to_luma(rgb);
elseif ndims(x) == 3
    img = rgb_to_luma(to_double_image(x));
else
    img = to_double_image(x);
end
img = clamp01(img);
end

function img = load_rgb_image(path)
[x, map] = imread(path);
if ~isempty(map)
    img = indexed_to_rgb(x, map);
elseif ndims(x) == 3
    img = to_double_image(x);
else
    gray = to_double_image(x);
    img = repmat(gray, [1, 1, 3]);
end
img = clamp01(img);
end

function rgb = indexed_to_rgb(x, map)
if isinteger(x)
    idx = double(x) + 1;
else
    idx = double(x);
end
idx = round(idx);
idx = max(1, min(size(map, 1), idx));
rgb = zeros([size(x), 3]);
for c = 1:3
    channel = map(:, c);
    rgb(:, :, c) = channel(idx);
end
end

function img = to_double_image(x)
if isa(x, 'uint8')
    img = double(x) / 255;
elseif isa(x, 'uint16')
    img = double(x) / 65535;
elseif isa(x, 'int16')
    img = (double(x) - double(intmin('int16'))) / double(intmax('int16') - intmin('int16'));
else
    img = double(x);
    if max(img(:)) > 1
        img = img / 255;
    end
end
end

function gray = rgb_to_luma(rgb)
gray = 0.299 * rgb(:, :, 1) + 0.587 * rgb(:, :, 2) + 0.114 * rgb(:, :, 3);
end

function rgbOut = replace_luma(rgb, oldY, newY)
scale = (newY + 1e-4) ./ (oldY + 1e-4);
rgbOut = zeros(size(rgb));
for c = 1:3
    rgbOut(:, :, c) = rgb(:, :, c) .* scale;
end
rgbOut = clamp01(rgbOut);
end

function [out, H] = gaussian_notch_reject(img, offsets, D0)
[m, n] = size(img);
[x, y] = meshgrid(1:n, 1:m);
cx = floor(n / 2) + 1;
cy = floor(m / 2) + 1;
H = ones(m, n);

for k = 1:size(offsets, 1)
    dy = offsets(k, 1);
    dx = offsets(k, 2);
    d2 = (y - (cy + dy)).^2 + (x - (cx + dx)).^2;
    H = H .* (1 - exp(-d2 / (2 * D0^2)));
end

F = fftshift(fft2(img));
G = F .* H;
out = real(ifft2(ifftshift(G)));
out = clamp01(out);
end

function out = wiener_deconvolution(img, psf, K)
H = psf2otf_custom(psf, size(img));
G = fft2(img);
W = conj(H) ./ (abs(H).^2 + K);
out = real(ifft2(G .* W));
out = clamp01(out);
end

function otf = psf2otf_custom(psf, outSize)
pad = zeros(outSize);
psfSize = size(psf);
pad(1:psfSize(1), 1:psfSize(2)) = psf;
shift = -floor(psfSize / 2);
pad = circshift(pad, shift);
otf = fft2(pad);
end

function out = median_filter2(img, k)
pad = floor(k / 2);
padded = pad_replicate(img, pad);
[m, n] = size(img);
out = zeros(m, n);
for r = 1:m
    for c = 1:n
        block = padded(r:r + k - 1, c:c + k - 1);
        out(r, c) = median(block(:));
    end
end
end

function out = mean_filter2(img, k)
kernel = ones(k, k) / (k * k);
out = conv2_pad_replicate(img, kernel);
end

function out = high_boost(img, amount)
smooth = mean_filter2(img, 3);
out = clamp01(img + amount * (img - smooth));
end

function out = gaussian_blur2(img, sigma)
radius = max(1, ceil(3 * sigma));
x = -radius:radius;
k = exp(-(x.^2) / (2 * sigma^2));
k = k / sum(k);
tmp = conv2_pad_replicate(img, k);
out = conv2_pad_replicate(tmp, k');
end

function k = gaussian_kernel2(sz, sigma)
if mod(sz, 2) == 0
    sz = sz + 1;
end
r = floor(sz / 2);
[x, y] = meshgrid(-r:r, -r:r);
k = exp(-(x.^2 + y.^2) / (2 * sigma^2));
k = k / sum(k(:));
end

function out = conv2_pad_replicate(img, kernel)
[kh, kw] = size(kernel);
padH = floor(kh / 2);
padW = floor(kw / 2);
padded = pad_replicate(img, [padH, padW]);
out = conv2(padded, kernel, 'valid');
end

function padded = pad_replicate(img, pad)
if numel(pad) == 1
    padH = pad;
    padW = pad;
else
    padH = pad(1);
    padW = pad(2);
end

[m, n] = size(img);
padded = zeros(m + 2 * padH, n + 2 * padW);
padded(padH + 1:padH + m, padW + 1:padW + n) = img;
padded(1:padH, padW + 1:padW + n) = repmat(img(1, :), padH, 1);
padded(padH + m + 1:end, padW + 1:padW + n) = repmat(img(end, :), padH, 1);
padded(:, 1:padW) = repmat(padded(:, padW + 1), 1, padW);
padded(:, padW + n + 1:end) = repmat(padded(:, padW + n), 1, padW);
end

function out = percentile_stretch(img, lowPct, highPct)
vals = sort(img(:));
numVals = numel(vals);
lowIdx = max(1, round(numVals * lowPct / 100));
highIdx = min(numVals, round(numVals * highPct / 100));
lo = vals(lowIdx);
hi = vals(highIdx);
if hi <= lo
    out = clamp01(img);
else
    out = clamp01((img - lo) / (hi - lo));
end
end

function metrics = reference_metrics(reference, target)
diff = reference - target;
mseValue = mean(diff(:).^2);
signalPower = sum(reference(:).^2);
noisePower = sum(diff(:).^2);

metrics = struct();
metrics.mse = mseValue;
metrics.rmse = sqrt(mseValue);
if noisePower <= 1e-12
    metrics.snr = Inf;
else
    metrics.snr = 10 * log10(signalPower / noisePower);
end
if mseValue <= 1e-12
    metrics.psnr = Inf;
else
    metrics.psnr = 10 * log10(1 / mseValue);
end
metrics.ssim = ssim_index(reference, target);
end

function value = ssim_index(x, y)
window = gaussian_kernel2(11, 1.5);
C1 = (0.01)^2;
C2 = (0.03)^2;

muX = conv2_pad_replicate(x, window);
muY = conv2_pad_replicate(y, window);
muX2 = muX.^2;
muY2 = muY.^2;
muXY = muX .* muY;

sigmaX2 = conv2_pad_replicate(x.^2, window) - muX2;
sigmaY2 = conv2_pad_replicate(y.^2, window) - muY2;
sigmaXY = conv2_pad_replicate(x .* y, window) - muXY;

ssimMap = ((2 * muXY + C1) .* (2 * sigmaXY + C2)) ./ ...
    ((muX2 + muY2 + C1) .* (sigmaX2 + sigmaY2 + C2));
value = mean(ssimMap(:));
end

function metrics = no_reference_metrics(gray)
gray = clamp01(gray);
metrics = struct();
metrics.mean = mean(gray(:));
metrics.std = std(gray(:));
metrics.entropy = image_entropy(gray);
metrics.average_gradient = average_gradient(gray);
metrics.laplacian_variance = laplacian_variance(gray);
end

function value = image_entropy(gray)
bins = floor(clamp01(gray(:)) * 255) + 1;
counts = accumarray(bins, 1, [256, 1]);
p = counts / sum(counts);
p = p(p > 0);
value = -sum(p .* log2(p));
end

function value = average_gradient(gray)
gx = diff(gray, 1, 2);
gy = diff(gray, 1, 1);
g = sqrt((gx(1:end - 1, :).^2 + gy(:, 1:end - 1).^2) / 2);
value = mean(g(:));
end

function value = laplacian_variance(gray)
kernel = [0, 1, 0; 1, -4, 1; 0, 1, 0];
lap = conv2_pad_replicate(gray, kernel);
value = var(lap(:));
end

function img = spectrum_image(gray)
F = fftshift(fft2(gray - mean(gray(:))));
mag = log(1 + abs(F));
out = percentile_stretch(mag, 5.0, 99.7);
img = clamp01(out);
end

function show_gray_subplot(rows, cols, idx, img, titleText)
subplot(rows, cols, idx);
imagesc(img);
axis image off;
colormap(gray(256));
title(titleText, 'Interpreter', 'none', 'FontSize', 10);
end

function out = clamp01(img)
out = min(max(img, 0), 1);
end

function out = to_uint8(img)
out = uint8(round(clamp01(img) * 255));
end

function write_json(data, path)
try
    text = jsonencode(data, 'PrettyPrint', true);
catch
    text = jsonencode(data);
end
fid = fopen(path, 'w');
if fid < 0
    error('Cannot write JSON: %s', path);
end
fwrite(fid, text, 'char');
fclose(fid);
end

function write_metrics_csv(summary, path)
fid = fopen(path, 'w');
if fid < 0
    error('Cannot write metrics CSV: %s', path);
end

fprintf(fid, 'task,variant,stage,mse,rmse,snr,psnr,ssim,entropy,std,average_gradient,laplacian_variance\n');

b = summary.enhancement.metrics_before;
a = summary.enhancement.metrics_after;
fprintf(fid, 'enhancement,dog,before,%.8f,%.8f,%.8f,%.8f,%.8f,,,,\n', b.mse, b.rmse, b.snr, b.psnr, b.ssim);
fprintf(fid, 'enhancement,dog,after,%.8f,%.8f,%.8f,%.8f,%.8f,,,,\n', a.mse, a.rmse, a.snr, a.psnr, a.ssim);

b = summary.restoration_fig1.metrics_before;
a = summary.restoration_fig1.metrics_after;
fprintf(fid, 'restoration,fig1_forest,before,,,,,,%.8f,%.8f,%.8f,%.8f\n', b.entropy, b.std, b.average_gradient, b.laplacian_variance);
fprintf(fid, 'restoration,fig1_forest,after,,,,,,%.8f,%.8f,%.8f,%.8f\n', a.entropy, a.std, a.average_gradient, a.laplacian_variance);

b = summary.restoration_fig2.metrics_before;
a = summary.restoration_fig2.metrics_after;
fprintf(fid, 'restoration,fig2_waterfall,before,,,,,,%.8f,%.8f,%.8f,%.8f\n', b.entropy, b.std, b.average_gradient, b.laplacian_variance);
fprintf(fid, 'restoration,fig2_waterfall,after,,,,,,%.8f,%.8f,%.8f,%.8f\n', a.entropy, a.std, a.average_gradient, a.laplacian_variance);

fclose(fid);
end

function write_report_notes(summary, path)
fid = fopen(path, 'w');
if fid < 0
    error('Cannot write report notes: %s', path);
end

e0 = summary.enhancement.metrics_before;
e1 = summary.enhancement.metrics_after;
r10 = summary.restoration_fig1.metrics_before;
r11 = summary.restoration_fig1.metrics_after;
r20 = summary.restoration_fig2.metrics_before;
r21 = summary.restoration_fig2.metrics_after;

fprintf(fid, '课程设计3：图像增强与复原算法综合应用结果摘要\n');
fprintf(fid, '生成时间：%s\n\n', summary.generated_at);

fprintf(fid, '一、图像增强任务\n');
fprintf(fid, '算法：%s\n', summary.enhancement.method);
fprintf(fid, '陷波中心偏移：(0, ±71), (±74, 0)，半径 D0=%d。\n', summary.enhancement.notch_radius);
fprintf(fid, 'MSE: %.6f -> %.6f\n', e0.mse, e1.mse);
fprintf(fid, 'SNR: %.3f dB -> %.3f dB\n', e0.snr, e1.snr);
fprintf(fid, 'PSNR: %.3f dB -> %.3f dB\n', e0.psnr, e1.psnr);
fprintf(fid, 'SSIM: %.4f -> %.4f\n\n', e0.ssim, e1.ssim);

fprintf(fid, '二、图像复原任务-图1（偶数学号默认选择）\n');
fprintf(fid, '算法：%s\n', summary.restoration_fig1.method);
fprintf(fid, '无参考指标：Entropy %.3f -> %.3f；Std %.3f -> %.3f；Average Gradient %.5f -> %.5f；Laplacian Variance %.5f -> %.5f。\n\n', ...
    r10.entropy, r11.entropy, r10.std, r11.std, r10.average_gradient, r11.average_gradient, r10.laplacian_variance, r11.laplacian_variance);

fprintf(fid, '三、图像复原任务-图2（备选）\n');
fprintf(fid, '算法：%s\n', summary.restoration_fig2.method);
fprintf(fid, '无参考指标：Entropy %.3f -> %.3f；Std %.3f -> %.3f；Average Gradient %.5f -> %.5f；Laplacian Variance %.5f -> %.5f。\n\n', ...
    r20.entropy, r21.entropy, r20.std, r21.std, r20.average_gradient, r21.average_gradient, r20.laplacian_variance, r21.laplacian_variance);

fprintf(fid, '结论：增强任务中，频域陷波有效抑制周期噪声，空间域滤波进一步降低随机噪声，MSE明显下降，SNR与SSIM明显提高。复原任务中，图1使用维纳复原提升边缘清晰度，图2使用Retinex和gamma校正改善低照度。由于复原任务没有清晰参考图，报告中采用信息熵、标准差、平均梯度和拉普拉斯方差作为无参考质量评价指标。\n');

fclose(fid);
end
