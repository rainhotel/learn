clear; clc; close all;

topicDir = fileparts(mfilename('fullpath'));
imageDir = fullfile(topicDir, 'images');
outDir = fullfile(topicDir, 'output', 'matlab-figures');
if ~exist(outDir, 'dir')
    mkdir(outDir);
end

Iorig = im2double(imread(fullfile(imageDir, 'Fig0507(a)(ckt-board-orig).tif')));
Inoise = im2double(imread(fullfile(imageDir, 'Fig0507(b)(ckt-board-gauss-var-400).tif')));
Ipepper = im2double(imread(fullfile(imageDir, 'Fig0508(a)(circuit-board-pepper-prob-pt1).tif')));
Isalt = im2double(imread(fullfile(imageDir, 'Fig0508(b)(circuit-board-salt-prob-pt1).tif')));
Imixed = im2double(imread(fullfile(imageDir, 'Fig0512(b)(ckt-uniform-plus-saltpepr-prob-pt1).tif')));

%% Task 1: Gaussian kernel coefficients and parameter comparison
sameSigma = 1.0;
kernelSizes = [3, 5, 7];
sigmaList = [0.6, 1.0, 1.6];

figure('Color', 'w', 'Position', [100 100 1200 350]);
tiledlayout(1, 3, 'Padding', 'compact', 'TileSpacing', 'compact');
for i = 1:numel(kernelSizes)
    h = fspecial('gaussian', kernelSizes(i), sameSigma);
    nexttile;
    surf(h);
    title(sprintf('%dx%d, sigma=%.1f', kernelSizes(i), kernelSizes(i), sameSigma));
end
exportgraphics(gcf, fullfile(outDir, 'task1_kernel_surface.png'));

figure('Color', 'w', 'Position', [100 100 1250 360]);
tiledlayout(1, 4, 'Padding', 'compact', 'TileSpacing', 'compact');
nexttile; imshow(Inoise, []); title('Noisy Fig0507(b)');
for i = 1:numel(kernelSizes)
    h = fspecial('gaussian', kernelSizes(i), sameSigma);
    J = imfilter(Inoise, h, 'replicate');
    nexttile; imshow(J, []); title(sprintf('%dx%d, sigma=%.1f', kernelSizes(i), kernelSizes(i), sameSigma));
end
exportgraphics(gcf, fullfile(outDir, 'task1_size_compare.png'));

figure('Color', 'w', 'Position', [100 100 1250 360]);
tiledlayout(1, 4, 'Padding', 'compact', 'TileSpacing', 'compact');
nexttile; imshow(Inoise, []); title('Noisy Fig0507(b)');
for i = 1:numel(sigmaList)
    h = fspecial('gaussian', [5 5], sigmaList(i));
    J = imfilter(Inoise, h, 'replicate');
    nexttile; imshow(J, []); title(sprintf('5x5, sigma=%.1f', sigmaList(i)));
end
exportgraphics(gcf, fullfile(outDir, 'task1_sigma_compare.png'));

%% Task 2: Bilateral versus Gaussian
Jg = imfilter(Inoise, fspecial('gaussian', [5 5], 1.0), 'replicate');
Jb = imbilatfilt(Inoise);

figure('Color', 'w', 'Position', [100 100 1200 360]);
tiledlayout(1, 4, 'Padding', 'compact', 'TileSpacing', 'compact');
nexttile; imshow(Iorig, []); title('Clean Fig0507(a)');
nexttile; imshow(Inoise, []); title('Noisy Fig0507(b)');
nexttile; imshow(Jg, []); title('Gaussian 5x5');
nexttile; imshow(Jb, []); title('Bilateral');
exportgraphics(gcf, fullfile(outDir, 'task2_bilateral_vs_gaussian.png'));

%% Task 3: Gaussian noise denoising with three mean filters
noiseVars = [0.001, 0.005, 0.01];
compareSize = 5;
for i = 1:numel(noiseVars)
    Jn = imnoise(Iorig, 'gaussian', 0, noiseVars(i));
    J1 = imfilter(Jn, ones(compareSize) / (compareSize^2), 'replicate');
    J2 = imfilter(Jn, weighted_kernel(compareSize), 'replicate');
    J3 = imfilter(Jn, fspecial('gaussian', [compareSize compareSize], 1.0), 'replicate');

    figure('Color', 'w', 'Position', [100 100 1450 360]);
    tiledlayout(1, 5, 'Padding', 'compact', 'TileSpacing', 'compact');
    nexttile; imshow(Iorig, []); title('Clean Fig0507(a)');
    nexttile; imshow(Jn, []); title(sprintf('Noisy var=%.3f', noiseVars(i)));
    nexttile; imshow(J1, []); title('Arithmetic mean');
    nexttile; imshow(J2, []); title('Weighted mean');
    nexttile; imshow(J3, []); title('Gaussian mean');
    exportgraphics(gcf, fullfile(outDir, sprintf('task3_var_%0.3f.png', noiseVars(i))));
end

%% Task 4: Arithmetic versus geometric mean
Ja = imfilter(Inoise, ones(5) / 25, 'replicate');
Jgeo = geometric_mean_filter(Inoise, 5);

figure('Color', 'w', 'Position', [100 100 1200 360]);
tiledlayout(1, 4, 'Padding', 'compact', 'TileSpacing', 'compact');
nexttile; imshow(Iorig, []); title('Clean Fig0507(a)');
nexttile; imshow(Inoise, []); title('Noisy Fig0507(b)');
nexttile; imshow(Ja, []); title('Arithmetic mean 5x5');
nexttile; imshow(Jgeo, []); title('Geometric mean 5x5');
exportgraphics(gcf, fullfile(outDir, 'task4_arithmetic_vs_geometric.png'));

%% Task 5: Contra-harmonic mean filtering
qPepper = [0.5, 1.5, 3.0];
qSalt = [-0.5, -1.5, -3.0];

figure('Color', 'w', 'Position', [100 100 1450 360]);
tiledlayout(1, 5, 'Padding', 'compact', 'TileSpacing', 'compact');
nexttile; imshow(Iorig, []); title('Reference Fig0507(a)');
nexttile; imshow(Ipepper, []); title('Pepper noise');
for i = 1:numel(qPepper)
    nexttile; imshow(contra_harmonic_filter(Ipepper, 3, qPepper(i)), []); title(sprintf('Q=%.1f', qPepper(i)));
end
exportgraphics(gcf, fullfile(outDir, 'task5_pepper_compare.png'));

figure('Color', 'w', 'Position', [100 100 1450 360]);
tiledlayout(1, 5, 'Padding', 'compact', 'TileSpacing', 'compact');
nexttile; imshow(Iorig, []); title('Reference Fig0507(a)');
nexttile; imshow(Isalt, []); title('Salt noise');
for i = 1:numel(qSalt)
    nexttile; imshow(contra_harmonic_filter(Isalt, 3, qSalt(i)), []); title(sprintf('Q=%.1f', qSalt(i)));
end
exportgraphics(gcf, fullfile(outDir, 'task5_salt_compare.png'));

%% Task 6: Mixed noise comparison
Jarith = imfilter(Imixed, ones(3) / 9, 'replicate');
Jgeom = geometric_mean_filter(Imixed, 3);
Jharm = harmonic_mean_filter(Imixed, 3);
Jcontra = contra_harmonic_filter(Imixed, 3, 1.5);

figure('Color', 'w', 'Position', [100 100 1450 360]);
tiledlayout(1, 5, 'Padding', 'compact', 'TileSpacing', 'compact');
nexttile; imshow(Imixed, []); title('Noisy Fig0512(b)');
nexttile; imshow(Jarith, []); title('Arithmetic mean 3x3');
nexttile; imshow(Jgeom, []); title('Geometric mean 3x3');
nexttile; imshow(Jharm, []); title('Harmonic mean 3x3');
nexttile; imshow(Jcontra, []); title('Contra-harmonic Q=1.5');
exportgraphics(gcf, fullfile(outDir, 'task6_mixed_compare.png'));

disp('MATLAB figures exported successfully.');

function h = weighted_kernel(n)
    row = pascal_row(n);
    h = row' * row;
    h = h / sum(h(:));
end

function row = pascal_row(n)
    row = 1;
    for k = 2:n
        row = conv(row, [1 1]);
    end
end

function J = geometric_mean_filter(I, n)
    epsVal = 1e-6;
    J = exp(imfilter(log(I + epsVal), ones(n) / (n^2), 'replicate'));
end

function J = harmonic_mean_filter(I, n)
    epsVal = 1e-6;
    J = (n^2) ./ imfilter(1 ./ (I + epsVal), ones(n), 'replicate');
end

function J = contra_harmonic_filter(I, n, Q)
    epsVal = 1e-6;
    numerator = imfilter((I + epsVal) .^ (Q + 1), ones(n), 'replicate');
    denominator = imfilter((I + epsVal) .^ Q, ones(n), 'replicate');
    J = numerator ./ (denominator + epsVal);
end
