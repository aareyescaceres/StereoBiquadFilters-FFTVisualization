
package BEAN;

public final class EqualizerFilter {
    // Coeficientes del filtro
    private double b0, b1, b2, a1, a2;
    
    // Memoria del filtro
    private double x1, x2, y1, y2;
    
    private final double frequency;
    private final double sampleRate;
    private final double Q = 1.41; // Ancho de banda estándar (1 octava aprox)
    
    public EqualizerFilter(double frequency, double sampleRate) {
        this.frequency = frequency;
        this.sampleRate = sampleRate;
        updateGain(0.0); // Inicia plano (0 dB)
    }

    /**
     * Recalcula los coeficientes cuando movemos el slider.
     * Basado en Robert Bristow-Johnson's Audio EQ Cookbook.
     * @param dbGain
     */
    public void updateGain(double dbGain) {
        double A = Math.pow(10.0, dbGain / 40.0);
        double w0 = 2.0 * Math.PI * frequency / sampleRate;
        double alpha = Math.sin(w0) / (2.0 * Q);
        double cosw0 = Math.cos(w0);

        // Fórmulas para Peaking EQ
        double norm = 1.0 + alpha / A;
        b0 = (1.0 + alpha * A) / norm;
        b1 = (-2.0 * cosw0) / norm;
        b2 = (1.0 - alpha * A) / norm;
        a1 = (-2.0 * cosw0) / norm;
        a2 = (1.0 - alpha / A) / norm;
    }

    /**
     * Procesa UNA sola muestra de audio.
     * @param inputSample
     * @return 
     */
    public double process(double inputSample) {
        // Ecuación de diferencia (Direct Form I)
        double outputSample = b0 * inputSample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

        // Actualizar memoria
        x2 = x1;
        x1 = inputSample;
        y2 = y1;
        y1 = outputSample;

        return outputSample;
    }
}
