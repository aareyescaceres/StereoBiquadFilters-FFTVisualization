# Mathematical Logic & Computational Algorithms

This document details the digital signal processing (DSP) algorithms and mathematical models implemented in this project. The system relies on pure Java implementations of the **Fast Fourier Transform (FFT)** for visualization and **Infinite Impulse Response (IIR)** filters for audio equalization.

## 1. Fast Fourier Transform (FFT)

The visualization engine uses the **Cooley-Tukey** algorithm to transform time-domain audio signals into the frequency domain.

### 1.1 The Algorithm
The implementation in `FFT.java` uses a **radix-2 recursive** approach. This divides the Discrete Fourier Transform (DFT) of size $N$ into two interleaved DFTs of size $N/2$.

The core recursive relationship is defined as:
$$X_k = E_k + e^{-i \frac{2\pi k}{N}} O_k$$
$$X_{k + N/2} = E_k - e^{-i \frac{2\pi k}{N}} O_k$$

Where:
* $E_k$ is the DFT of the even-indexed inputs.
* $O_k$ is the DFT of the odd-indexed inputs.
* $e^{-i \frac{2\pi k}{N}}$ is the complex rotation factor (Twiddle Factor).

### 1.2 Zero Padding
The Cooley-Tukey algorithm requires the input array length $N$ to be a power of 2. The implementation handles arbitrary buffer sizes by inspecting the length using bitwise operations:
```java
if ((n & (n - 1)) != 0) { ... } // Checks if N is power of 2

```

If the buffer size is not a power of 2, the signal is automatically padded with zeros (silence) to the next power of 2 to ensure algorithmic stability.

### 1.3 Complexity

* **Time Complexity:** , significantly faster than the  of a standard DFT.
* **Space Complexity:**  for recursive stack storage and auxiliary arrays.

## 2. Digital Parametric Equalizer (IIR Filters)

The equalizer consists of a bank of 10 independent **Bi-quad (Bi-quadratic)** filters implemented in `EqualizerFilter.java`.

### 2.1 Filter Topology

We utilize a **Direct Form I** topology for the implementation of the difference equation. This structure is chosen for its stability in fixed-point or double-precision floating-point arithmetic.

**Difference Equation:**

Where:

*  is the current input sample.
*  is the current output sample.
*  are previous inputs (Feedforward).
*  are previous outputs (Feedback).

### 2.2 Coefficient Calculation (Peaking EQ)

The coefficients () are recalculated dynamically whenever the gain changes, based on **Robert Bristow-Johnson's Audio EQ Cookbook** formulas.

For a specific frequency  and sampling rate :

1. **Intermediate variables:**

$$w_0 = 2\pi \frac{f_0}{F_s}$$

$$\alpha = \frac{\sin(w_0)}{2Q}$$

$$A = 10^{\frac{dB_{gain}}{40}}$$

2. **Coefficients:**
The filter updates `b0`, `b1`, `b2`, `a1`, and `a2` to shape the frequency response, boosting or cutting the amplitude at  with a specific Q-factor (bandwidth). In this implementation,  is fixed at approx.  (1 octave bandwidth).

## 3. Signal Windowing

Before performing the FFT for visualization, a **Hann Window** (or raised cosine) is applied to the time-domain signal in `AudioController.java`.

**Purpose:**
This reduces **spectral leakage**. Since the FFT assumes the signal is infinite and periodic, abrupt cuts at the edges of the buffer (discontinuities) would introduce false high-frequency artifacts. The window function smooths the edges of the sample buffer to zero.

## 4. Complex Number Arithmetic

To support the FFT calculations, a custom `Complex` class was implemented in `Complex.java`. It handles arithmetic operations required for the butterfly diagram in the FFT algorithm:

* Addition / Subtraction
* Multiplication ($ (a+bi)(c+di) = (ac-bd) + (ad+bc)i $)
* Phase/Magnitude extraction (using `Math.atan2` and `Math.sqrt`).
