# 公式与方法清单

## 图像增强

- 周期噪声：空间域中表现为规则条纹，频率域中表现为远离中心的成对亮点。
- 高斯陷波滤波器：

```text
H_k(u, v) = 1 - exp(-D_k(u, v)^2 / (2D0^2))
H(u, v) = product(H_k(u, v))
```

- 高提升锐化：

```text
g = f + alpha * (f - f_smooth)
```

## 图像复原

- 退化模型：

```text
g(x, y) = h(x, y) * f(x, y) + n(x, y)
```

- 维纳滤波：

```text
F_hat(u, v) = H*(u, v) / (|H(u, v)|^2 + K) * G(u, v)
```

## 有参考指标

- MSE：

```text
MSE = mean((f - g)^2)
```

- SNR：

```text
SNR = 10 * log10(sum(f^2) / sum((f - g)^2))
```

- PSNR：

```text
PSNR = 10 * log10(1 / MSE)
```

- SSIM：局部窗口内综合亮度、对比度和结构相似度。

## 无参考指标

- 信息熵：灰度信息量。
- 标准差：整体对比度。
- 平均梯度：边缘变化强度。
- 拉普拉斯方差：高频细节和清晰度。
