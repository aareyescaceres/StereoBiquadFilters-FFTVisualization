# Java Audio Equalizer & FFT Visualizer 🎵

A robust WAV audio player built with **Java Swing** that features a real-time **10-band Parametric Equalizer**, a **Noise Gate**, and dynamic **FFT-based signal visualizations**.

This project demonstrates the implementation of Digital Signal Processing (DSP) concepts including the **Cooley-Tukey FFT algorithm** and **Bi-quad IIR Filters** for audio equalization without relying on external audio processing libraries.

## 🚀 Features

* **10-Band Equalizer:** Adjustable gain (+/- 24dB) for standard ISO frequencies: 31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz.
* **Real-time Visualization:**
    * **Time Domain:** Oscilloscope-style waveform view (Green).
    * **Frequency Domain:** FFT-based sine wave approximation (Multi-colored).
* **Audio Controls:** Play, Pause, Reset (Stop), and Seek bar (Time slider).
* **Noise Reduction:** A toggleable "Canceler" (Noise Gate) to attenuate low-level background noise.
* **File Support:** Supports standard `.wav` files (Mono and Stereo).

## 🛠️ Technical Architecture

The application is structured into two main packages:

### 1. Digital Signal Processing (`BEAN` Package)
* **`FFT.java`**: Implements the recursive **Cooley-Tukey** algorithm to transform time-domain audio signals into frequency-domain data for visualization. Handles zero-padding for non-power-of-two buffer sizes.
* **`EqualizerFilter.java`**: Implements **Bi-quad IIR filters** (Peaking EQ) based on *Robert Bristow-Johnson's Audio EQ Cookbook*. It uses a Direct Form I difference equation to process audio samples individually.
* **`Complex.java`**: A helper class for complex number arithmetic required by the FFT calculations.

### 2. Core Logic (`UI` Package)
* **`AudioController.java`**: The engine of the application. It handles:
    * Byte-stream reading and buffering (`4096` buffer size).
    * Decoding bytes to doubles for processing.
    * Applying the EQ filters and Noise Gate loop.
    * Computing FFT for the UI.
    * Managing audio threads and synchronization.
* **`FFTAudio.java`**: The main `JFrame` UI entry point.

## 📦 Project Structure

```text
src/
├── BEAN/
│   ├── Complex.java          # Complex number math
│   ├── EqualizerFilter.java  # IIR Filter logic (Peaking EQ)
│   └── FFT.java              # Fast Fourier Transform algorithm
└── UI/
    ├── AudioController.java  # Audio thread & logic controller
    ├── FFTAudio.java         # Main Swing GUI
    └── FFTAudio.form         # NetBeans GUI Form

```

## 🔧 Installation & Usage

### Prerequisites

* Java Development Kit (JDK) 8 or higher.
* An IDE (NetBeans, IntelliJ, Eclipse) or command line tools.

### Running the App

1. Clone the repository or download the source code.
2. Compile the Java files.
3. Run the main class: `UI.FFTAudio`.

### Controls

1. **Upload:** Click to select a `.wav` file from your computer.
2. **Play/Pause:** Control playback.
3. **Sliders:** Move the vertical sliders to adjust specific frequency bands.
4. **Canceler Checkbox:** Check to enable the Noise Gate (reduces static/hiss when audio is quiet).
5. **Time Slider:** Drag the horizontal slider to seek through the song.

## 🧠 DSP Theory Used

* **Fast Fourier Transform (FFT):** Used strictly for the visualization aspect. It breaks the audio signal down into its constituent sine waves to display the frequency content.
* **IIR Filters:** Infinite Impulse Response filters are used for the equalizer because they are computationally efficient. The specific topology is a 2nd-order section (Bi-quad) configured as a Peaking Filter.
