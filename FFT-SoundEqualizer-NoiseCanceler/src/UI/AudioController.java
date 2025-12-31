package UI;

import BEAN.Complex;
import BEAN.EqualizerFilter;
import BEAN.FFT;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.sound.sampled.*;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class AudioController implements Runnable {

    private File audioFile;
    private AudioInputStream audioStream;
    private SourceDataLine sourceLine;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private Thread playerThread;

    private final JSlider progressSlider;
    private final JCheckBox noiseCheck;
    private final JPanel complexWavePanel;
    private final JPanel simpleWavesPanel;
    
    private final int BUFFER_SIZE = 4096;
    
    // Filtros Estéreo
    private EqualizerFilter[] filtersLeft;
    private EqualizerFilter[] filtersRight;
    private final int[] freqs = {31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
    
    // Control de Ganancia
    private final double[] targetDb = new double[10];
    private final double[] currentDb = new double[10];
    private int channels = 1; 

    // --- VARIABLES PARA EL SEEKING (Salto de tiempo) ---
    private volatile boolean isUserSeeking = false; // ¿El usuario tiene agarrado el slider?
    private volatile int seekRequestValue = -1;     // ¿A dónde quiere saltar?
    
    public AudioController(JSlider[] eqSliders, JSlider progressSlider, JCheckBox noiseCheck, JPanel complexPanel, JPanel simplePanel) {
        this.progressSlider = progressSlider;
        this.noiseCheck = noiseCheck;
        this.complexWavePanel = complexPanel;
        this.simpleWavesPanel = simplePanel;
        
        for(int i=0; i<10; i++) {
            targetDb[i] = 0.0;
            currentDb[i] = 0.0;
        }
    }

    private void initFilters(float sampleRate, int numChannels) {
        this.channels = numChannels;
        filtersLeft = new EqualizerFilter[10];
        filtersRight = new EqualizerFilter[10];
        for (int i = 0; i < 10; i++) {
            filtersLeft[i] = new EqualizerFilter(freqs[i], sampleRate);
            filtersRight[i] = new EqualizerFilter(freqs[i], sampleRate);
        }
    }

    // --- MÉTODOS PARA CONTROLAR EL SEEKING---
    
    // 1. Avisar que el usuario tocó el slider
    public void setUserSeeking(boolean seeking) {
        this.isUserSeeking = seeking;
    }

    // 2. Ordenar el salto a una posición (0 a 100)
    public void seek(int sliderValue) {
        this.seekRequestValue = sliderValue;
    }
    
    public void loadFile(File file) {
        this.audioFile = file;
        reset();
    }

    public void play() {
        if (audioFile == null) return;
        if (isPlaying && !isPaused) return;
        isPlaying = true;
        isPaused = false;
        if (playerThread == null || !playerThread.isAlive()) {
            playerThread = new Thread(this);
            playerThread.start();
        }
    }

    public void pause() { isPaused = true; }

    public void reset() {
        isPlaying = false;
        isPaused = false;
        progressSlider.setValue(0);
        clearPanels();
        if (sourceLine != null && sourceLine.isOpen()) sourceLine.close();
    }

    public synchronized void setBandGain(int index, int sliderValue) {
        if (index >= 0 && index < targetDb.length) {
            targetDb[index] = (sliderValue - 50) * (24.0 / 50.0);
        }
    }

    @Override
    public void run() {
        try {
            audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            initFilters(format.getSampleRate(), format.getChannels());
            
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            sourceLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceLine.open(format, BUFFER_SIZE * 2);
            sourceLine.start();

            byte[] buffer = new byte[BUFFER_SIZE * 2];
            int bytesRead;
            
            long totalFrames = audioStream.getFrameLength(); 
            long totalBytes = totalFrames * format.getFrameSize();
            long currentBytes = 0;

            double[] samplesL = new double[BUFFER_SIZE];
            double[] samplesR = new double[BUFFER_SIZE];

            while (isPlaying) {
                
                // --- LÓGICA DE SALTO (SEEKING) ---
                if (seekRequestValue != -1) {
                    // 1. Calcular a qué byte saltar
                    double percent = seekRequestValue / (double) progressSlider.getMaximum();
                    long targetByte = (long) (totalBytes * percent);
                    
                    // 2. Alinear al tamaño del frame
                    int frameSize = format.getFrameSize();
                    targetByte -= (targetByte % frameSize);
                    
                    // 3. Reiniciar el stream desde ese punto
                    // Los streams no pueden saltar atrás fácilmente, así que se reabren
                    try {
                        audioStream.close();
                        audioStream = AudioSystem.getAudioInputStream(audioFile);
                        audioStream.skip(targetByte);
                        
                        currentBytes = targetByte;
                        sourceLine.flush(); // Limpiar el buffer de audio antiguo
                        
                        // Reiniciar filtros
                        initFilters(format.getSampleRate(), format.getChannels());
                        
                        seekRequestValue = -1; // Salto completado
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // ----------------------------------

                // Lectura normal
                bytesRead = audioStream.read(buffer, 0, buffer.length);
                if (bytesRead == -1) break;

                while (isPaused) { Thread.sleep(10); if (!isPlaying) break; }
                if (!isPlaying) break;

                int framesProcessed = decodeBytesToDoubles(buffer, samplesL, samplesR, bytesRead);
                updateFiltersSmoothly();

                boolean noiseGate = noiseCheck.isSelected();
                for (int i = 0; i < framesProcessed; i++) {
                    if (noiseGate) {
                         double threshold = 0.001; 
                         if (Math.abs(samplesL[i]) < threshold) samplesL[i] *= 0.1;
                         if (channels == 2 && Math.abs(samplesR[i]) < threshold) samplesR[i] *= 0.1;
                    }
                    // EQ L
                    for (EqualizerFilter f : filtersLeft) samplesL[i] = f.process(samplesL[i]);
                    samplesL[i] = Math.tanh(samplesL[i]);
                    // EQ R
                    if (channels == 2) {
                        for (EqualizerFilter f : filtersRight) samplesR[i] = f.process(samplesR[i]);
                        samplesR[i] = Math.tanh(samplesR[i]); 
                    }
                }

                // Visualización
                Complex[] visSignal = new Complex[framesProcessed];
                for(int i=0; i<framesProcessed; i++) {
                    double monoVal = (channels == 2) ? (samplesL[i] + samplesR[i]) * 0.5 : samplesL[i];
                    double window = 0.5 * (1 - Math.cos(2*Math.PI*i/(framesProcessed-1)));
                    visSignal[i] = new Complex(monoVal * 32767 * window, 0);
                }
                // Zero Padding
                if (visSignal.length < BUFFER_SIZE) {
                    Complex[] padded = new Complex[BUFFER_SIZE];
                    System.arraycopy(visSignal, 0, padded, 0, visSignal.length);
                    for(int k=visSignal.length; k<BUFFER_SIZE; k++) padded[k] = new Complex(0,0);
                    visSignal = padded;
                }
                
                drawWaves(visSignal, FFT.fft(visSignal), framesProcessed);

                byte[] outBuffer = encodeDoublesToBytes(samplesL, samplesR, framesProcessed);
                sourceLine.write(outBuffer, 0, bytesRead);
                
                currentBytes += bytesRead;
                
                // --- ACTUALIZAR SLIDER ---
                if (!isUserSeeking) {
                    int progress = (int) ((double) currentBytes / totalBytes * progressSlider.getMaximum());
                    progressSlider.setValue(progress);
                }
            }
            sourceLine.drain(); sourceLine.close(); isPlaying = false;
        } catch (Exception e) { e.printStackTrace(); isPlaying = false; }
    }

    private void updateFiltersSmoothly() {
        for(int i=0; i<10; i++) {
            if (Math.abs(targetDb[i] - currentDb[i]) > 0.01) {
                currentDb[i] += (targetDb[i] - currentDb[i]) * 0.2;
                filtersLeft[i].updateGain(currentDb[i]);
                if (channels == 2) filtersRight[i].updateGain(currentDb[i]);
            }
        }
    }

    private int decodeBytesToDoubles(byte[] buffer, double[] left, double[] right, int bytesRead) {
        int bytesPerSample = 2; 
        int frameSize = bytesPerSample * channels; 
        int totalFrames = bytesRead / frameSize;
        for (int i = 0; i < totalFrames; i++) {
            int baseIndex = i * frameSize;
            int sampleL = (short) ((buffer[baseIndex] & 0xFF) | (buffer[baseIndex + 1] << 8));
            left[i] = sampleL / 32768.0;
            if (channels == 2) {
                int sampleR = (short) ((buffer[baseIndex + 2] & 0xFF) | (buffer[baseIndex + 3] << 8));
                right[i] = sampleR / 32768.0;
            }
        }
        return totalFrames;
    }

    private byte[] encodeDoublesToBytes(double[] left, double[] right, int frames) {
        int bytesPerSample = 2;
        int frameSize = bytesPerSample * channels;
        byte[] buffer = new byte[frames * frameSize];
        for (int i = 0; i < frames; i++) {
            int baseIndex = i * frameSize;
            int valL = (int) (left[i] * 32767);
            if (valL > 32767) valL = 32767; if (valL < -32768) valL = -32768;
            buffer[baseIndex] = (byte) (valL & 0xFF);
            buffer[baseIndex + 1] = (byte) ((valL >> 8) & 0xFF);
            if (channels == 2) {
                int valR = (int) (right[i] * 32767);
                if (valR > 32767) valR = 32767; if (valR < -32768) valR = -32768;
                buffer[baseIndex + 2] = (byte) (valR & 0xFF);
                buffer[baseIndex + 3] = (byte) ((valR >> 8) & 0xFF);
            }
        }
        return buffer;
    }

    private void drawWaves(Complex[] timeSignal, Complex[] frequencySignal, int validSamples) {
        BufferedImage imgComplex = new BufferedImage(801, 160, BufferedImage.TYPE_INT_ARGB);
        BufferedImage imgSimple = new BufferedImage(801, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imgComplex.createGraphics();
        Graphics2D g3 = imgSimple.createGraphics();
        g2.setColor(Color.BLACK); g2.fillRect(0, 0, 801, 160);
        g3.setColor(Color.BLACK); g3.fillRect(0, 0, 801, 160);

        g2.setColor(Color.GREEN);
        int midY = 80;
        double horizontalScale = (double) validSamples / 800.0;
        for (int x = 0; x < 800; x++) {
            int idx = (int) (x * horizontalScale);
            int nextIdx = (int) ((x + 1) * horizontalScale);
            if (idx >= validSamples) idx = validSamples - 1;
            if (nextIdx >= validSamples) nextIdx = validSamples - 1;
            int val = (int) (timeSignal[idx].getReal() / 100.0);
            int nextVal = (int) (timeSignal[nextIdx].getReal() / 100.0);
            g2.drawLine(x, midY - val, x + 1, midY - nextVal);
        }

        Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA};
        for (int k = 0; k < 7; k++) {
            g3.setColor(colors[k]);
            int freqIndex = (k + 1) * (frequencySignal.length / 60); 
            if (freqIndex < frequencySignal.length) {
                double amplitude = frequencySignal[freqIndex].abs() / 50000.0; 
                double phase = frequencySignal[freqIndex].getPhase();
                for (int x = 0; x < 800; x++) {
                    double y1 = Math.sin(x * 0.05 * (k + 1) + phase) * amplitude * 150; 
                    double y2 = Math.sin((x + 1) * 0.05 * (k + 1) + phase) * amplitude * 150;
                    g3.drawLine(x, midY - (int)y1, x + 1, midY - (int)y2);
                }
            }
        }
        g2.dispose(); g3.dispose();
        if(complexWavePanel.getGraphics() != null) complexWavePanel.getGraphics().drawImage(imgComplex, 0, 0, null);
        if(simpleWavesPanel.getGraphics() != null) simpleWavesPanel.getGraphics().drawImage(imgSimple, 0, 0, null);
    }
    
    private void clearPanels() {
        if (complexWavePanel.getGraphics() != null) {
            complexWavePanel.getGraphics().setColor(Color.BLACK);
            complexWavePanel.getGraphics().fillRect(0, 0, 801, 160);
        }
        if (simpleWavesPanel.getGraphics() != null) {
            simpleWavesPanel.getGraphics().setColor(Color.BLACK);
            simpleWavesPanel.getGraphics().fillRect(0, 0, 801, 160);
        }
    }
}