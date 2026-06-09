function run_lab6_image_restoration()
% 实验6：图像复原自动化脚本
% 运行内容：
% 1. 运动模糊模拟
% 2. 大气湍流模糊模拟
% 3. 大气湍流图像的逆滤波和维纳滤波复原
% 4. 运动模糊带噪图像的逆滤波和维纳滤波复原

clearvars;
close all;
clc;

set(0, 'DefaultFigureVisible', 'off');

root_dir = 'D:\moniC\project\learn';
input_dir = fullfile(root_dir, '01-topics', 'image-restoration-lab6', 'input-images');
output_dir = fullfile(root_dir, '03-outputs', 'image-restoration-lab6');

if ~exist(output_dir, 'dir')
    mkdir(output_dir);
end

paths.original_motion = fullfile(input_dir, 'Fig0526(a)(original_DIP).tif');
paths.original_turb = fullfile(input_dir, 'Fig0525(a)(aerial_view_no_turb).tif');
paths.turb_blurred = fullfile(input_dir, 'Fig0525(b)(aerial_view_turb_c_0pt0025).tif');
paths.motion_noisy_high = fullfile(input_dir, 'Fig0529(a)(noisiest_var_pt1).tif');
paths.motion_noisy_mid = fullfile(input_dir, 'Fig0529(d)(medium_noise_var_pt01).tif');
paths.motion_noisy_low = fullfile(input_dir, 'Fig0529(g)(least_noise_var_10minus37).tif');

required = struct2cell(paths);
for idx = 1:numel(required)
    if ~exist(required{idx}, 'file')
        error('Missing input image: %s', required{idx});
    end
end

motion_original = load_gray_image(paths.original_motion);
turb_original = load_gray_image(paths.original_turb);
turb_degraded = load_gray_image(paths.turb_blurred);

motion_noisy = struct();
motion_noisy.noisiest = load_gray_image(paths.motion_noisy_high);
motion_noisy.medium = load_gray_image(paths.motion_noisy_mid);
motion_noisy.least = load_gray_image(paths.motion_noisy_low);

summary = struct();
summary.generated_at = char(datetime('now', 'Format', 'yyyy-MM-dd HH:mm:ss'));
summary.assumptions = struct('turbulence_k_for_restoration', 0.0025);

summary.task1 = task1_motion_blur_demo(motion_original, output_dir);
summary.task2 = task2_turbulence_demo(turb_original, output_dir);
summary.task3 = task3_restore_turbulence(turb_original, turb_degraded, output_dir);
summary.task4 = task4_restore_motion_noise(motion_original, motion_noisy, output_dir);

summary_file = fullfile(output_dir, 'summary.json');
fid = fopen(summary_file, 'w');
if fid < 0
    error('Failed to write summary file: %s', summary_file);
end
fwrite(fid, jsonencode(summary, 'PrettyPrint', true), 'char');
fclose(fid);

write_metrics_csv(summary, fullfile(output_dir, 'metrics.csv'));
write_notes_txt(summary, fullfile(output_dir, 'report-notes.txt'));

fprintf('All tasks finished.\n');
fprintf('Outputs saved to: %s\n', output_dir);
fprintf('Summary saved to: %s\n', summary_file);
end

function result = task1_motion_blur_demo(img, output_dir)
fprintf('Running task 1: motion blur simulation...\n');

param_sets = [
    0.05, 0.05, 1.0;
    0.10, 0.10, 1.0;
    0.15, 0.05, 1.0;
    0.10, 0.20, 1.5
];

labels = {
    'a=0.05, b=0.05, T=1.0'
    'a=0.10, b=0.10, T=1.0'
    'a=0.15, b=0.05, T=1.0'
    'a=0.10, b=0.20, T=1.5'
};

fig = figure('Position', [100, 100, 1300, 700]);
show_image_subplot(2, 3, 1, img, '原图');

result = struct();
result.parameter_sets = param_sets;
result.image_file = 'task1_motion_blur_demo.png';

for idx = 1:size(param_sets, 1)
    a = param_sets(idx, 1);
    b = param_sets(idx, 2);
    T = param_sets(idx, 3);
    H = motion_psf_freq(size(img, 1), size(img, 2), a, b, T);
    blurred = apply_frequency_degradation(img, H);
    slot = idx + 1;
    show_image_subplot(2, 3, slot, blurred, labels{idx});

    result.samples(idx).a = a; %#ok<AGROW>
    result.samples(idx).b = b; %#ok<AGROW>
    result.samples(idx).T = T; %#ok<AGROW>
    result.samples(idx).mean_intensity = mean(blurred(:)); %#ok<AGROW>
    result.samples(idx).std_intensity = std(blurred(:)); %#ok<AGROW>

    out_path = fullfile(output_dir, sprintf('task1_motion_blur_sample_%d.png', idx));
    imwrite(to_uint8(blurred), out_path);
end

sgtitle('任务1：不同参数下的运动模糊模拟');
saveas(fig, fullfile(output_dir, result.image_file));
close(fig);
end

function result = task2_turbulence_demo(img, output_dir)
fprintf('Running task 2: turbulence blur simulation...\n');

k_values = [0.0005, 0.0025, 0.0050, 0.0100, 0.0500];
fig = figure('Position', [100, 100, 1300, 700]);
show_image_subplot(2, 3, 1, img, '原图');

result = struct();
result.k_values = k_values;
result.image_file = 'task2_turbulence_demo.png';

for idx = 1:numel(k_values)
    k = k_values(idx);
    H = turbulence_psf_freq(size(img, 1), size(img, 2), k);
    blurred = apply_frequency_degradation(img, H);
    show_image_subplot(2, 3, idx + 1, blurred, sprintf('k=%.4f', k));

    result.samples(idx).k = k; %#ok<AGROW>
    result.samples(idx).mean_intensity = mean(blurred(:)); %#ok<AGROW>
    result.samples(idx).std_intensity = std(blurred(:)); %#ok<AGROW>

    out_path = fullfile(output_dir, sprintf('task2_turbulence_sample_%d.png', idx));
    imwrite(to_uint8(blurred), out_path);
end

sgtitle('任务2：不同参数下的大气湍流模糊模拟');
saveas(fig, fullfile(output_dir, result.image_file));
close(fig);
end

function result = task3_restore_turbulence(original_img, degraded_img, output_dir)
fprintf('Running task 3: turbulence restoration...\n');

k = 0.0025;
H = turbulence_psf_freq(size(original_img, 1), size(original_img, 2), k);

inverse_cutoffs = [40, 60, 80, 100, 140, 180, 220];
inverse_thresholds = [1e-3, 5e-3, 1e-2];
best_inverse = struct('psnr', -inf);

for cutoff = inverse_cutoffs
    for threshold = inverse_thresholds
        restored = inverse_restore(degraded_img, H, cutoff, threshold);
        score = calc_psnr(original_img, restored);
        if score > best_inverse.psnr
            best_inverse.psnr = score;
            best_inverse.cutoff = cutoff;
            best_inverse.threshold = threshold;
            best_inverse.image = restored;
            best_inverse.mse = calc_mse(original_img, restored);
        end
    end
end

wiener_cutoffs = [60, 80, 100, 140, 180, 220];
wiener_ks = [1e-8, 3e-8, 1e-7, 3e-7, 1e-6, 3e-6, 1e-5, 3e-5, 1e-4];
best_wiener = struct('psnr', -inf);

for cutoff = wiener_cutoffs
    for K = wiener_ks
        restored = wiener_restore(degraded_img, H, K, cutoff);
        score = calc_psnr(original_img, restored);
        if score > best_wiener.psnr
            best_wiener.psnr = score;
            best_wiener.cutoff = cutoff;
            best_wiener.K = K;
            best_wiener.image = restored;
            best_wiener.mse = calc_mse(original_img, restored);
        end
    end
end

fig = figure('Position', [100, 100, 1400, 700]);
show_image_subplot(2, 3, 1, original_img, '原图');
show_image_subplot(2, 3, 2, degraded_img, '湍流模糊图');
show_image_subplot(2, 3, 3, best_inverse.image, sprintf('逆滤波 PSNR=%.2f dB', best_inverse.psnr));
show_image_subplot(2, 3, 4, abs(original_img - degraded_img), '原图与模糊图差值');
show_image_subplot(2, 3, 5, abs(original_img - best_inverse.image), '原图与逆滤波差值');
show_image_subplot(2, 3, 6, best_wiener.image, sprintf('维纳滤波 PSNR=%.2f dB', best_wiener.psnr));
sgtitle('任务3：大气湍流模糊图像复原对比');
saveas(fig, fullfile(output_dir, 'task3_turbulence_restoration.png'));
close(fig);

fig2 = figure('Position', [100, 100, 1200, 450]);
show_image_subplot(1, 3, 1, degraded_img, '待复原图像');
show_image_subplot(1, 3, 2, best_inverse.image, sprintf('逆滤波 cutoff=%d', best_inverse.cutoff));
show_image_subplot(1, 3, 3, best_wiener.image, sprintf('维纳滤波 K=%.0e', best_wiener.K));
sgtitle('任务3：最佳复原结果');
saveas(fig2, fullfile(output_dir, 'task3_turbulence_best_results.png'));
close(fig2);

imwrite(to_uint8(best_inverse.image), fullfile(output_dir, 'task3_inverse_restored.png'));
imwrite(to_uint8(best_wiener.image), fullfile(output_dir, 'task3_wiener_restored.png'));

result = struct();
result.k = k;
result.image_file = 'task3_turbulence_restoration.png';
result.best_inverse = rmfield(best_inverse, 'image');
result.best_wiener = rmfield(best_wiener, 'image');
end

function result = task4_restore_motion_noise(original_img, noisy_struct, output_dir)
fprintf('Running task 4: motion blur noisy image restoration...\n');

estimated = estimate_motion_params(original_img, noisy_struct.least);
H = estimated.H;

names = fieldnames(noisy_struct);
result = struct();
result.image_file = 'task4_motion_noise_restoration.png';
result.estimated_degradation = rmfield(estimated, {'preview_image', 'H'});

fig0 = figure('Position', [100, 100, 1200, 450]);
show_image_subplot(1, 3, 1, original_img, '原图');
show_image_subplot(1, 3, 2, noisy_struct.least, '低噪声运动模糊图');
show_image_subplot(1, 3, 3, estimated.preview_image, ...
    sprintf('估计PSF len=%d, theta=%d', estimated.len, estimated.theta));
sgtitle('任务4：运动模糊参数估计预览');
saveas(fig0, fullfile(output_dir, 'task4_motion_parameter_estimation.png'));
close(fig0);

fig = figure('Position', [100, 100, 1500, 950]);

inverse_cutoffs = [40, 60, 80, 100, 140, 180];
inverse_thresholds = [1e-2, 2e-2, 5e-2, 1e-1];
wiener_cutoffs = [60, 80, 100, 140, 180, 220];
wiener_ks = [1e-6, 3e-6, 1e-5, 3e-5, 1e-4, 3e-4, 1e-3, 3e-3];

for idx = 1:numel(names)
    key = names{idx};
    degraded = noisy_struct.(key);

    best_inverse = struct('psnr', -inf);
    for cutoff = inverse_cutoffs
        for threshold = inverse_thresholds
            restored = inverse_restore(degraded, H, cutoff, threshold);
            score = calc_psnr(original_img, restored);
            if score > best_inverse.psnr
                best_inverse.psnr = score;
                best_inverse.cutoff = cutoff;
                best_inverse.threshold = threshold;
                best_inverse.image = restored;
                best_inverse.mse = calc_mse(original_img, restored);
            end
        end
    end

    best_wiener = struct('psnr', -inf);
    for cutoff = wiener_cutoffs
        for K = wiener_ks
            restored = wiener_restore(degraded, H, K, cutoff);
            score = calc_psnr(original_img, restored);
            if score > best_wiener.psnr
                best_wiener.psnr = score;
                best_wiener.cutoff = cutoff;
                best_wiener.K = K;
                best_wiener.image = restored;
                best_wiener.mse = calc_mse(original_img, restored);
            end
        end
    end

    show_image_subplot(3, 4, (idx - 1) * 4 + 1, original_img, '原图');
    show_image_subplot(3, 4, (idx - 1) * 4 + 2, degraded, sprintf('%s 噪声图', key));
    show_image_subplot(3, 4, (idx - 1) * 4 + 3, best_inverse.image, ...
        sprintf('逆滤波 %.2f dB', best_inverse.psnr));
    show_image_subplot(3, 4, (idx - 1) * 4 + 4, best_wiener.image, ...
        sprintf('维纳滤波 %.2f dB', best_wiener.psnr));

    result.(key).best_inverse = rmfield(best_inverse, 'image');
    result.(key).best_wiener = rmfield(best_wiener, 'image');

    imwrite(to_uint8(best_inverse.image), fullfile(output_dir, sprintf('task4_%s_inverse_restored.png', key)));
    imwrite(to_uint8(best_wiener.image), fullfile(output_dir, sprintf('task4_%s_wiener_restored.png', key)));
end

sgtitle('任务4：运动模糊带噪图像复原对比');
saveas(fig, fullfile(output_dir, result.image_file));
close(fig);
end

function img = load_gray_image(path)
img = imread(path);
if ndims(img) == 3
    img = rgb2gray(img);
end
img = im2double(img);
end

function H = motion_psf_freq(M, N, a, b, T)
[U, V] = centered_frequency_grid(M, N);
uv = (U * a + V * b) * T;
den = pi * uv;
H = exp(-1i * pi * uv) .* sin(pi * uv) ./ (den + eps);
H(abs(uv) < 1e-12) = 1;
end

function H = turbulence_psf_freq(M, N, k)
[U, V] = pixel_frequency_grid(M, N);
H = exp(-k .* ((U .^ 2 + V .^ 2) .^ (5 / 6)));
end

function degraded = apply_frequency_degradation(img, H)
F = fftshift(fft2(img));
G = H .* F;
degraded = real(ifft2(ifftshift(G)));
degraded = clamp01(degraded);
end

function restored = inverse_restore(img, H, cutoff, threshold)
[M, N] = size(img);
[U, V] = centered_frequency_grid(M, N);
D = sqrt(U .^ 2 + V .^ 2);

F = fftshift(fft2(img));
mask = (abs(H) >= threshold) & (D <= cutoff);
invH = zeros(size(H));
invH(mask) = 1 ./ H(mask);
G = F .* invH;
restored = real(ifft2(ifftshift(G)));
restored = clamp01(restored);
end

function restored = wiener_restore(img, H, K, cutoff)
[M, N] = size(img);
[U, V] = centered_frequency_grid(M, N);
D = sqrt(U .^ 2 + V .^ 2);

F = fftshift(fft2(img));
W = conj(H) ./ (abs(H) .^ 2 + K);
W(D > cutoff) = 0;
G = F .* W;
restored = real(ifft2(ifftshift(G)));
restored = clamp01(restored);
end

function [U, V] = centered_frequency_grid(M, N)
u = ((0:M-1) - floor(M / 2)) / M;
v = ((0:N-1) - floor(N / 2)) / N;
[V, U] = meshgrid(v, u);
end

function [U, V] = pixel_frequency_grid(M, N)
u = (0:M-1) - floor(M / 2);
v = (0:N-1) - floor(N / 2);
[V, U] = meshgrid(v, u);
end

function mse = calc_mse(a, b)
diff = a - b;
mse = mean(diff(:) .^ 2);
end

function value = calc_psnr(reference, target)
mse = calc_mse(reference, target);
if mse <= 1e-12
    value = inf;
else
    value = 10 * log10(1 / mse);
end
end

function out = clamp01(img)
out = min(max(img, 0), 1);
end

function out = to_uint8(img)
out = uint8(round(clamp01(img) * 255));
end

function show_image_subplot(rows, cols, index, img, title_text)
subplot(rows, cols, index);
imagesc(img);
colormap(gray(256));
axis image off;
title(title_text, 'Interpreter', 'none', 'FontSize', 10);
end

function estimate = estimate_motion_params(original_img, least_noisy_img)
length_values = [10, 15, 20, 25, 30, 35, 40, 45];
theta_values = [30, 45, 60, 120, 135, 150];
cutoffs = [80, 120, 160, 220];
Ks = [1e-6, 1e-5, 1e-4, 1e-3];

estimate = struct('psnr', -inf);
for len = length_values
    for theta = theta_values
            psf = fspecial('motion', len, theta);
            H = fftshift(psf2otf(psf, size(original_img)));
            for cutoff = cutoffs
                for K = Ks
                    restored = wiener_restore(least_noisy_img, H, K, cutoff);
                    score = calc_psnr(original_img, restored);
                    if score > estimate.psnr
                        estimate.psnr = score;
                        estimate.len = len;
                        estimate.theta = theta;
                        estimate.cutoff = cutoff;
                        estimate.K = K;
                        estimate.preview_image = restored;
                        estimate.mse = calc_mse(original_img, restored);
                        estimate.H = H;
                    end
                end
            end
    end
end
end

function write_metrics_csv(summary, path)
fid = fopen(path, 'w');
if fid < 0
    error('Failed to write metrics csv: %s', path);
end

fprintf(fid, 'task,variant,method,psnr,mse,cutoff,extra\n');
fprintf(fid, 'task3,turbulence,inverse,%.6f,%.6f,%d,threshold=%.6g\n', ...
    summary.task3.best_inverse.psnr, summary.task3.best_inverse.mse, ...
    summary.task3.best_inverse.cutoff, summary.task3.best_inverse.threshold);
fprintf(fid, 'task3,turbulence,wiener,%.6f,%.6f,%d,K=%.6g\n', ...
    summary.task3.best_wiener.psnr, summary.task3.best_wiener.mse, ...
    summary.task3.best_wiener.cutoff, summary.task3.best_wiener.K);

variants = {'noisiest', 'medium', 'least'};
if isfield(summary.task4, 'estimated_degradation')
    fprintf(fid, 'task4,estimated,params,%.6f,%.6f,%d,len=%d;theta=%d;K=%.6g\n', ...
        summary.task4.estimated_degradation.psnr, summary.task4.estimated_degradation.mse, ...
        summary.task4.estimated_degradation.cutoff, summary.task4.estimated_degradation.len, ...
        summary.task4.estimated_degradation.theta, ...
        summary.task4.estimated_degradation.K);
end
for idx = 1:numel(variants)
    key = variants{idx};
    inverse = summary.task4.(key).best_inverse;
    wiener = summary.task4.(key).best_wiener;
    fprintf(fid, 'task4,%s,inverse,%.6f,%.6f,%d,threshold=%.6g\n', ...
        key, inverse.psnr, inverse.mse, inverse.cutoff, inverse.threshold);
    fprintf(fid, 'task4,%s,wiener,%.6f,%.6f,%d,K=%.6g\n', ...
        key, wiener.psnr, wiener.mse, wiener.cutoff, wiener.K);
end

fclose(fid);
end

function write_notes_txt(summary, path)
fid = fopen(path, 'w');
if fid < 0
    error('Failed to write notes file: %s', path);
end

fprintf(fid, '实验6 图像复原自动分析摘要\n');
fprintf(fid, '生成时间: %s\n\n', summary.generated_at);

fprintf(fid, '任务1 运动模糊模拟\n');
fprintf(fid, '- 已输出多组(a, b, T)参数下的模糊结果，用于对比方向和模糊程度。\n\n');

fprintf(fid, '任务2 大气湍流模糊模拟\n');
fprintf(fid, '- 已输出多组k参数下的湍流模糊结果，k越大高频衰减越明显。\n\n');

fprintf(fid, '任务3 大气湍流模糊图像复原\n');
fprintf(fid, '- 逆滤波最佳: PSNR=%.3f dB, cutoff=%d, threshold=%.6g\n', ...
    summary.task3.best_inverse.psnr, summary.task3.best_inverse.cutoff, ...
    summary.task3.best_inverse.threshold);
fprintf(fid, '- 维纳滤波最佳: PSNR=%.3f dB, cutoff=%d, K=%.6g\n\n', ...
    summary.task3.best_wiener.psnr, summary.task3.best_wiener.cutoff, ...
    summary.task3.best_wiener.K);

fprintf(fid, '任务4 运动模糊参数估计\n');
fprintf(fid, '- 依据低噪声图像搜索得到 len=%d, theta=%d, cutoff=%d, K=%.6g, 预览PSNR=%.3f dB\n\n', ...
    summary.task4.estimated_degradation.len, summary.task4.estimated_degradation.theta, ...
    summary.task4.estimated_degradation.cutoff, ...
    summary.task4.estimated_degradation.K, summary.task4.estimated_degradation.psnr);

variants = {'noisiest', 'medium', 'least'};
labels = {'高噪声', '中噪声', '低噪声'};
for idx = 1:numel(variants)
    key = variants{idx};
    inverse = summary.task4.(key).best_inverse;
    wiener = summary.task4.(key).best_wiener;
    fprintf(fid, '任务4 %s运动模糊带噪图像复原\n', labels{idx});
    fprintf(fid, '- 逆滤波最佳: PSNR=%.3f dB, cutoff=%d, threshold=%.6g\n', ...
        inverse.psnr, inverse.cutoff, inverse.threshold);
    fprintf(fid, '- 维纳滤波最佳: PSNR=%.3f dB, cutoff=%d, K=%.6g\n\n', ...
        wiener.psnr, wiener.cutoff, wiener.K);
end

fprintf(fid, '总体结论\n');
fprintf(fid, '- 含噪情况下，维纳滤波通常比逆滤波更稳定。\n');
fprintf(fid, '- 噪声越强，逆滤波越容易放大噪声，维纳滤波的优势越明显。\n');
fprintf(fid, '- 逆滤波在噪声较弱、退化模型较准确时仍能获得较好锐化效果。\n');

fclose(fid);
end
