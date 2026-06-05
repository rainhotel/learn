clear; clc;

f = imread('Fig1007(a)(wirebond_mask).tif'); 

w = [2 -1 -1; 
     -1 2 -1; 
     -1 -1 2];

g = abs(imfilter(double(f), w));

T = 0.5 * max(g(:));   


g2 = g >= T;

figure('Color', 'w');
subplot(1, 3, 1), imshow(f), title('(a) 原始图像');
subplot(1, 3, 2), imshow(uint8(g)), title('(b) 滤波响应图');
subplot(1, 3, 3), imshow(g2, []), title(['(c) 检测结果 (阈值 = ', num2str(T), ')']);