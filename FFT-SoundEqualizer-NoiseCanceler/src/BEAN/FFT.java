package BEAN;

public class FFT {

    /**
     * Calcula la Transformada Rápida de Fourier.
     * Si el tamaño del arreglo 'x' NO es potencia de 2, lo rellena con ceros automáticamente.
     * @param x
     * @return 
     */
    public static Complex[] fft(Complex[] x) {
        int n = x.length;

        // Caso base
        if (n == 1) return new Complex[] { x[0] };

        // --- AUTO-Relleno (Zero Padding) ---
        // Si n es potencia de 2 usa bitwise operator
        if ((n & (n - 1)) != 0) { 
            // Si no es potencia de 2
            int nextPow2 = Integer.highestOneBit(n) << 1;
            
            // Nuevo arreglo más grande
            Complex[] padded = new Complex[nextPow2];
            
            // Copiar los datos originales
            System.arraycopy(x, 0, padded, 0, n);
            // Rellenar el resto con ceros (silencio)
            for (int i = n; i < nextPow2; i++) {
                padded[i] = new Complex(0, 0);
            }
            
            // Llamar a FFT con el arreglo corregido
            return fft(padded);
        }
        // -----------------------------------------------

        // Algoritmo Cooley-Tukey estándar
        Complex[] even = new Complex[n / 2];
        Complex[] odd = new Complex[n / 2];

        for (int k = 0; k < n / 2; k++) {
            even[k] = x[2 * k];
            odd[k] = x[2 * k + 1];
        }

        Complex[] q = fft(even);
        Complex[] r = fft(odd);

        Complex[] y = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = q[k].add(wk.multiply(r[k]));
            y[k + n / 2] = q[k].subtract(wk.multiply(r[k]));
        }
        return y;
    }

    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];

        // Conjugada
        for (int i = 0; i < n; i++) {
            y[i] = new Complex(x[i].getReal(), -x[i].getImag());
        }

        // FFT
        y = fft(y);

        // Conjugada nuevamente y escalar
        Complex[] result = new Complex[n];
        for (int i = 0; i < n; i++) {
            result[i] = new Complex(y[i].getReal() / n, -y[i].getImag() / n);
        }
        return result;
    }
}