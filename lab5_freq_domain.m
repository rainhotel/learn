%% 实验5：频域图像增强
% 数字图像处理实习
clear; close all; clc;

% 图像路径 & 输出目录
img_path = 'D:\moniC\Fig0333(a)(test_pattern_blurring_orig).tif';
out_dir = 'D:\moniC\project\learn\lab5_output';
if ~exist(out_dir, 'dir')
    mkdir(out_dir);
end

%% ========================================================================
% 任务1：生成图像 + FFT + 旋转比较
% ========================================================================
fprintf('===== 任务1：生成图像与FFT分析 =====\n');

% 1.1 生成128x128均匀分布图像 (均值=0, 方差=255)
% 均匀分布 U(a,b): 均值=(a+b)/2=0, 方差=(b-a)^2/12=255
% => b=-a, (2b)^2/12=255 => b=sqrt(765)≈27.66
d = sqrt(3060);          % (b-a) = sqrt(255*12) = sqrt(3060)
a = -d/2; b = d/2;
f1 = a + (b - a) * rand(128, 128);
fprintf('f1 实际均值: %.4f, 实际方差: %.4f\n', mean(f1(:)), var(f1(:)));

% 计算FFT并中心化
F1 = fft2(f1);
F1_shifted = fftshift(F1);
F1_mag = abs(F1_shifted);
F1_log = log(1 + F1_mag);

% 1.2 将f1顺时针旋转45度得到f2
f2 = imrotate(f1, -45, 'bilinear', 'crop');
F2 = fft2(f2);
F2_shifted = fftshift(F2);
F2_mag = abs(F2_shifted);
F2_log = log(1 + F2_mag);

% 显示结果
figure('Name', '任务1：图像生成与FFT分析', 'Position', [100, 100, 900, 400]);
subplot(1,4,1); imshow(f1, []); title('原图 f1 (均匀分布)');
subplot(1,4,2); imshow(F1_log, []); title('FFT(f1) 幅度谱');
subplot(1,4,3); imshow(f2, []); title('f2 (顺时针旋转45°)');
subplot(1,4,4); imshow(F2_log, []); title('FFT(f2) 幅度谱');
sgtitle('任务1：图像旋转对频谱的影响 —— 频谱同步旋转');

saveas(gcf, fullfile(out_dir, 'task1_fft_rotation.png'));
fprintf('结论：图像旋转45°后，其频谱也同步旋转45°，体现了傅里叶变换的旋转性质。\n\n');

%% ========================================================================
% 任务2：频域低通滤波 (Butterworth & 高斯)
% ========================================================================
fprintf('===== 任务2：频域低通滤波 =====\n');

img = imread(img_path);
if size(img, 3) == 3
    img = rgb2gray(img);
end
img = im2double(img);
[M, N] = size(img);

% 中心化的距离矩阵
u = (0:M-1) - floor(M/2);
v = (0:N-1) - floor(N/2);
[V, U] = meshgrid(v, u);
D = sqrt(U.^2 + V.^2);

% FFT
F = fftshift(fft2(img));

% 滤波半径
radii = [5, 15, 30, 80, 230];
n_order = 2;  % Butterworth阶数

figure('Name', '任务2：频域低通滤波', 'Position', [50, 50, 1400, 800]);

for idx = 1:5
    D0 = radii(idx);

    % Butterworth低通滤波
    H_blpf = 1 ./ (1 + (D ./ D0).^(2 * n_order));
    G_blpf = F .* H_blpf;
    g_blpf = real(ifft2(ifftshift(G_blpf)));

    % 高斯低通滤波
    H_glpf = exp(-(D.^2) ./ (2 * D0^2));
    G_glpf = F .* H_glpf;
    g_glpf = real(ifft2(ifftshift(G_glpf)));

    % 显示
    subplot(5, 3, 3*idx - 2);
    imshow(g_blpf); title(sprintf('Butterworth D_0=%d', D0));
    subplot(5, 3, 3*idx - 1);
    imshow(g_glpf); title(sprintf('高斯 D_0=%d', D0));
    subplot(5, 3, 3*idx);
    imshow(H_blpf, []);
    title(sprintf('滤波器 D_0=%d', D0));
end
sgtitle('任务2：不同半径的Butterworth与高斯低通滤波对比');
saveas(gcf, fullfile(out_dir, 'task2_lowpass.png'));

fprintf('结论：截止频率D0越小，图像越模糊（更多高频被滤除）；\n');
fprintf('      D0=230时基本保留所有频率分量，图像接近原图。\n\n');

%% ========================================================================
% 任务3：添加高斯噪声 + 低通滤波去噪
% ========================================================================
fprintf('===== 任务3：高斯噪声去除 =====\n');

% 添加不同密度的高斯噪声
noise_variances = [0.001, 0.005, 0.01];
figure('Name', '任务3：高斯噪声与低通滤波去噪', 'Position', [50, 50, 1200, 800]);

for nidx = 1:3
    noise_var = noise_variances(nidx);
    img_noisy = img + sqrt(noise_var) * randn(M, N);
    img_noisy = max(0, min(1, img_noisy));

    % 频谱分析
    F_noisy = fftshift(fft2(img_noisy));
    F_noisy_log = log(1 + abs(F_noisy));

    % 选择截止频率（噪声越大，截止频率越小，滤波越强）
    D0_noise = 60 - (nidx - 1) * 20;

    % Butterworth低通滤波
    H_lpf = 1 ./ (1 + (D ./ D0_noise).^(2 * n_order));
    G_denoised = F_noisy .* H_lpf;
    g_denoised = real(ifft2(ifftshift(G_denoised)));

    subplot(3, 4, 4*nidx - 3);
    imshow(img_noisy); title(sprintf('加噪图 \\sigma^2=%.3f', noise_var));
    subplot(3, 4, 4*nidx - 2);
    imshow(F_noisy_log, []); title('噪声图像频谱');
    subplot(3, 4, 4*nidx - 1);
    imshow(g_denoised); title(sprintf('BLPF去噪 D_0=%d', D0_noise));
    subplot(3, 4, 4*nidx);
    imshow(H_lpf, []); title('Butterworth滤波器');
end
sgtitle('任务3：高斯噪声添加与低通滤波去噪');

saveas(gcf, fullfile(out_dir, 'task3_gaussian_noise.png'));
fprintf('结论：高斯噪声在频域表现为全频带的白噪声；\n');
fprintf('      低通滤波可以有效去除高频噪声，但也会损失图像细节。\n\n');

%% ========================================================================
% 任务4：频域高通滤波 (Butterworth & 高斯)
% ========================================================================
fprintf('===== 任务4：频域高通滤波 =====\n');

D0_hp = 15;

% Butterworth高通滤波
H_bhpf = 1 ./ (1 + (D0_hp ./ (D + eps)).^(2 * n_order));
G_bhpf = F .* H_bhpf;
g_bhpf = real(ifft2(ifftshift(G_bhpf)));

% 高斯高通滤波
H_ghpf = 1 - exp(-(D.^2) ./ (2 * D0_hp^2));
G_ghpf = F .* H_ghpf;
g_ghpf = real(ifft2(ifftshift(G_ghpf)));

figure('Name', '任务4：频域高通滤波', 'Position', [100, 100, 1000, 500]);
subplot(2,3,1); imshow(img); title('原图');
subplot(2,3,2); imshow(g_bhpf); title('Butterworth HPF (D_0=15)');
subplot(2,3,3); imshow(g_ghpf); title('高斯 HPF (D_0=15)');
subplot(2,3,4); imshow(log(1+abs(F)), []); title('原图频谱');
subplot(2,3,5); imshow(H_bhpf, []); title('Butterworth HPF');
subplot(2,3,6); imshow(H_ghpf, []); title('高斯 HPF');
sgtitle('任务4：Butterworth与高斯高通滤波对比');

fprintf('结论：高通滤波保留边缘和细节，但丢失了低频背景信息；\n');
fprintf('      Butterworth HPF边缘更清晰，高斯HPF过渡更平滑。\n\n');
saveas(gcf, fullfile(out_dir, 'task4_highpass.png'));

%% ========================================================================
% 任务5：高频增强与高频强调滤波
% ========================================================================
fprintf('===== 任务5：高频增强与高频强调滤波 =====\n');

% 高频强调滤波: H_hfe = a + b * H_hp
% a控制低频分量保留, b控制高频分量增强
param_sets = [
    0.5, 1.0;   % 高频增强1
    0.5, 2.0;   % 高频增强2
    1.0, 1.5;   % 高频强调1
    0.3, 3.0;   % 高频强调2
];

figure('Name', '任务5：高频增强与高频强调滤波', 'Position', [50, 50, 1200, 800]);

for pidx = 1:4
    a_val = param_sets(pidx, 1);
    b_val = param_sets(pidx, 2);

    % 使用高斯高通作为基础
    H_hp = 1 - exp(-(D.^2) ./ (2 * D0_hp^2));
    H_hfe = a_val + b_val * H_hp;
    G_hfe = F .* H_hfe;
    g_hfe = real(ifft2(ifftshift(G_hfe)));

    % 直方图均衡化增强显示效果
    g_hfe_clipped = max(0, min(1, g_hfe));
    g_hfe_eq = histeq(g_hfe_clipped);

    subplot(2, 5, pidx);
    imshow(g_hfe); title(sprintf('a=%.1f, b=%.1f', a_val, b_val));
    subplot(2, 5, pidx + 5);
    imshow(g_hfe_eq); title(sprintf('a=%.1f,b=%.1f(均衡化)', a_val, b_val));
end
subplot(2,5,5); imshow(img); title('原图');
subplot(2,5,10); imshow(histeq(img)); title('原图(均衡化)');
sgtitle('任务5：不同参数的高频强调滤波');

fprintf('结论：参数a控制背景保留程度，b控制边缘增强程度；\n');
fprintf('      b > 1时高频被强调，图像边缘更突出。\n\n');
saveas(gcf, fullfile(out_dir, 'task5_hfe.png'));

%% ========================================================================
% 任务6：周期性噪声去除
% ========================================================================
fprintf('===== 任务6：周期性噪声去除 =====\n');

% 添加周期性噪声（正弦噪声）
img_periodic = img;
[M, N] = size(img);
[x, y] = meshgrid(0:N-1, 0:M-1);

% 添加多个频率的正弦噪声（在频谱中产生亮点）
freq_u = [50, 80];   % 水平频率
freq_v = [60, 100];  % 垂直频率

for k = 1:length(freq_u)
    img_periodic = img_periodic + 0.05 * sin(2 * pi * freq_u(k) * x / N);
    img_periodic = img_periodic + 0.05 * sin(2 * pi * freq_v(k) * y / M);
end
img_periodic = max(0, min(1, img_periodic));

% 频谱分析
F_periodic = fftshift(fft2(img_periodic));
F_periodic_log = log(1 + abs(F_periodic));

% 构造带阻滤波器（Butterworth notch filter）
% 在噪声频率位置放置陷波滤波器
H_notch = ones(M, N);
notch_radius = 8;  % 陷波半径

% 对每个噪声频率点及其对称点设置陷波
% 水平正弦噪声产生峰值在 (M/2, N/2 ± freq_u)
% 垂直正弦噪声产生峰值在 (M/2 ± freq_v, N/2)
noise_centers_u = [M/2, M/2, M/2+freq_v(1), M/2+freq_v(2), M/2-freq_v(1), M/2-freq_v(2), M/2, M/2];
noise_centers_v = [N/2+freq_u(1), N/2-freq_u(1), N/2, N/2, N/2, N/2, N/2+freq_u(2), N/2-freq_u(2)];

for k = 1:length(noise_centers_u)
    Dk = sqrt((U - noise_centers_u(k)).^2 + (V - noise_centers_v(k)).^2);
    Hk = 1 ./ (1 + (notch_radius ./ (Dk + eps)).^(2 * n_order));
    H_notch = H_notch .* Hk;
end

% 应用陷波滤波器
G_notch = F_periodic .* H_notch;
g_notch = real(ifft2(ifftshift(G_notch)));

figure('Name', '任务6：周期性噪声去除', 'Position', [100, 100, 1200, 400]);
subplot(2,3,1); imshow(img); title('原图');
subplot(2,3,2); imshow(img_periodic); title('添加周期性噪声');
subplot(2,3,3); imshow(g_notch); title('陷波滤波去噪');
subplot(2,3,4); imshow(log(1+abs(F)), []); title('原图频谱');
subplot(2,3,5); imshow(F_periodic_log, []); title('噪声图像频谱 （注意亮点）');
subplot(2,3,6); imshow(H_notch, []); title('陷波滤波器');
sgtitle('任务6：周期性噪声的频谱分析与陷波滤波去除');

fprintf('结论：周期性噪声在频谱中表现为亮点（冲激），\n');
fprintf('      通过陷波滤波器可以精确去除特定频率的噪声而不影响其他频率。\n\n');
saveas(gcf, fullfile(out_dir, 'task6_periodic_noise.png'));

fprintf('===== 全部实验任务完成！ =====\n');
