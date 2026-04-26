%% 图像直方图处理实验
% 使用说明：
% 1. 把实验图像放到 Matlab 当前工作目录，或修改 imageDir。
% 2. 如果 Fig0338(a)(blurry_moon) / Fig0809(a) 的扩展名不同，请在下方文件名处修改。
% 3. 先运行实验 1 和 2，按直方图微调 manualThreshold。

clear;
close all;
clc;

imageDir = '.';

manualConfigs = [
    struct('file', 'eight.tif', 'displayName', 'eight.tif', 'manualThreshold', 0.50)
    struct('file', 'rice.png', 'displayName', 'rice.png', 'manualThreshold', 0.35)
    struct('file', 'Fig0338(a)(blurry_moon).tif', 'displayName', 'Fig0338(a)(blurry_moon)', 'manualThreshold', 0.45)
];

%% 实验 1：人工阈值二值化
for k = 1:numel(manualConfigs)
    cfg = manualConfigs(k);
    I = read_gray_image(fullfile(imageDir, cfg.file));
    BW = imbinarize(I, cfg.manualThreshold);

    show_binarization_result(I, BW, ...
        sprintf('实验1 - %s', cfg.displayName), ...
        sprintf('人工阈值 = %.3f', cfg.manualThreshold));
end

%% 实验 2：graythresh / Otsu 自动阈值
otsuRows = cell(numel(manualConfigs), 3);

for k = 1:numel(manualConfigs)
    cfg = manualConfigs(k);
    I = read_gray_image(fullfile(imageDir, cfg.file));
    otsuThreshold = graythresh(I);
    BW = imbinarize(I, otsuThreshold);

    show_binarization_result(I, BW, ...
        sprintf('实验2 - %s', cfg.displayName), ...
        sprintf('Otsu阈值 = %.3f', otsuThreshold));

    otsuRows(k, :) = {cfg.displayName, cfg.manualThreshold, otsuThreshold};
end

otsuTable = cell2table(otsuRows, ...
    'VariableNames', {'Image', 'ManualThreshold', 'OtsuThreshold'});
disp('人工阈值与 Otsu 阈值对比：');
disp(otsuTable);

%% 实验 3：手写直方图均衡化（不能使用 histeq）
I_pout = read_gray_image(fullfile(imageDir, 'pout.tif'));
[J_eq, map, cdfValues] = my_histeq_gray(I_pout); %#ok<NASGU>

figure('Name', '实验3 - 手写直方图均衡化', 'Color', 'w');
tiledlayout(2, 2, 'TileSpacing', 'compact', 'Padding', 'compact');

nexttile;
imshow(I_pout);
title('原图 pout.tif');

nexttile;
imhist(I_pout);
title('原图直方图');

nexttile;
imshow(J_eq);
title('手写均衡化结果');

nexttile;
imhist(J_eq);
title('均衡化后直方图');

%% 实验 4：直方图规定化
I_ref = read_gray_image(fullfile(imageDir, 'Fig0809(a).tif'));
sourceUint8 = im2uint8(I_pout);
refUint8 = im2uint8(I_ref);
targetHist = imhist(refUint8, 256);
J_spec = histeq(sourceUint8, targetHist);

figure('Name', '实验4 - 直方图规定化', 'Color', 'w');
tiledlayout(2, 3, 'TileSpacing', 'compact', 'Padding', 'compact');

nexttile;
imshow(sourceUint8);
title('源图像 pout.tif');

nexttile;
imhist(sourceUint8);
title('源图像直方图');

nexttile;
bar(targetHist);
title('目标直方图 Fig0809(a)');
xlim([0 255]);

nexttile;
imshow(refUint8);
title('参考图像 Fig0809(a)');

nexttile;
imshow(J_spec);
title('规定化结果');

nexttile;
imhist(J_spec);
title('规定化后直方图');

%% 实验 5：自适应直方图均衡化
claheInput = sourceUint8;
J_clahe = adapthisteq(claheInput, 'ClipLimit', 0.01, 'NumTiles', [8 8]);

figure('Name', '实验5 - 自适应直方图均衡化', 'Color', 'w');
tiledlayout(2, 2, 'TileSpacing', 'compact', 'Padding', 'compact');

nexttile;
imshow(claheInput);
title('原图 pout.tif');

nexttile;
imhist(claheInput);
title('原图直方图');

nexttile;
imshow(J_clahe);
title('CLAHE 结果');

nexttile;
imhist(J_clahe);
title('CLAHE 后直方图');

%% 本文件局部函数
function I = read_gray_image(filePath)
    [~, name, ext] = fileparts(filePath);
    fallbackName = [name ext];

    if exist(filePath, 'file') == 2
        raw = imread(filePath);
    elseif exist(fallbackName, 'file') == 2
        raw = imread(fallbackName);
    else
        error('找不到图像文件：%s', filePath);
    end

    if ndims(raw) == 3
        raw = rgb2gray(raw);
    end

    I = im2double(raw);
end

function show_binarization_result(I, BW, figureName, thresholdText)
    figure('Name', figureName, 'Color', 'w');
    tiledlayout(1, 3, 'TileSpacing', 'compact', 'Padding', 'compact');

    nexttile;
    imshow(I);
    title('原图');

    nexttile;
    imhist(I);
    title('原图直方图');

    nexttile;
    imshow(BW);
    title({'二值化结果', thresholdText});
end

function [J, map, cdfValues] = my_histeq_gray(I)
    Iu = im2uint8(I);
    counts = imhist(Iu, 256);
    cdfValues = cumsum(counts) / numel(Iu);
    map = uint8(round(255 * cdfValues));
    J = map(double(Iu) + 1);
end
