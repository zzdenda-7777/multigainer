package multigainer.multigainer.math;

public class BigNumber implements Comparable<BigNumber> {

    // Maximální rozumný exponent - vysoko nad veškeré reálné potřeby hry,
    // ale dost nízko pod Double.MAX_VALUE (~1.8e308), aby se nikdy nedostali
    // do oblasti, kde by dalsi multiply/add mohlo prerazit do Infinity.
    private static final double MAX_EXPONENT = 1.0e15;

    private final double mantissa;
    private final double exponent;

    public BigNumber(double value) {
        if (value == 0 || Double.isNaN(value)) {
            this.mantissa = 0;
            this.exponent = 0;
        } else if (Double.isInfinite(value)) {
            // Nemělo by se stávat, ale kdyby někdo zavolal s Infinity přímo,
            // ošetříme to capem místo NaN mantissy.
            this.mantissa = value > 0 ? 1.0 : -1.0;
            this.exponent = MAX_EXPONENT;
        } else {
            double log = Math.floor(Math.log10(Math.abs(value)));
            double calcMantissa = value / Math.pow(10, log);
            double calcExponent = log;

            // Math.pow(10, log) může pro extrémně malá/velká log vrátit 0 nebo Infinity,
            // což by dalo calcMantissa = Infinity nebo NaN. Ošetřeno níže společnou
            // sanitizací stejně jako u druhého konstruktoru.
            if (Double.isNaN(calcMantissa) || Double.isInfinite(calcMantissa)) {
                calcMantissa = value > 0 ? 1.0 : -1.0;
                calcExponent = MAX_EXPONENT;
            }

            calcExponent = clampExponent(calcExponent);

            this.mantissa = calcMantissa;
            this.exponent = calcExponent;
        }
    }

    // Integrated Constructor: Correctly assigns final fields exactly once
    public BigNumber(double mantissa, double exponent) {
        double calcMantissa = mantissa;
        double calcExponent = exponent;

        if (Double.isNaN(calcMantissa) || Double.isNaN(calcExponent)) {
            calcMantissa = 0;
            calcExponent = 0;
        } else if (calcMantissa == 0) {
            calcMantissa = 0;
            calcExponent = 0;
        } else if (Double.isInfinite(calcMantissa) || Double.isInfinite(calcExponent)) {
            // Exponent přetekl (typicky z opakovaného multiply/add) - capneme
            // na MAX_EXPONENT místo propagace Infinity dál do formatteru.
            calcMantissa = calcMantissa > 0 ? 1.0 : -1.0;
            calcExponent = MAX_EXPONENT;
        } else if (Math.abs(calcMantissa) >= 10.0 || Math.abs(calcMantissa) < 1.0) {
            double log = Math.floor(Math.log10(Math.abs(calcMantissa)));
            double normalizedMantissa = calcMantissa / Math.pow(10, log);
            double normalizedExponent = calcExponent + log;

            if (Double.isNaN(normalizedMantissa) || Double.isInfinite(normalizedMantissa)
                    || Double.isNaN(normalizedExponent) || Double.isInfinite(normalizedExponent)) {
                calcMantissa = calcMantissa > 0 ? 1.0 : -1.0;
                calcExponent = MAX_EXPONENT;
            } else {
                calcMantissa = normalizedMantissa;
                calcExponent = normalizedExponent;
            }
        }

        calcExponent = clampExponent(calcExponent);

        this.mantissa = calcMantissa;
        this.exponent = calcExponent;
    }

    /**
     * Capne exponent na MAX_EXPONENT (oběma směry), aby žádná navazující
     * operace (multiply, add přes Math.pow(10, diff)...) nemohla vyrobit
     * Infinity/NaN po dalším kroku.
     */
    private static double clampExponent(double exp) {
        if (exp > MAX_EXPONENT) return MAX_EXPONENT;
        if (exp < -MAX_EXPONENT) return -MAX_EXPONENT;
        return exp;
    }

    public double getMantissa() { return mantissa; }
    public double getExponent() { return exponent; }

    // Helper method to convert BigNumber back to a standard double
    public double toDouble() {
        return this.mantissa * Math.pow(10, this.exponent);
    }

    public BigNumber add(BigNumber other) {
        if (this.mantissa == 0) return other;
        if (other.mantissa == 0) return this;

        BigNumber max = this.exponent >= other.exponent ? this : other;
        BigNumber min = this.exponent < other.exponent ? this : other;

        double diff = max.exponent - min.exponent;
        if (diff > 16) return max;

        return new BigNumber(max.mantissa + (min.mantissa / Math.pow(10, diff)), max.exponent);
    }

    public BigNumber subtract(BigNumber other) {
        if (other.mantissa == 0) return this;
        if (this.compareTo(other) <= 0) return new BigNumber(0);

        double diff = this.exponent - other.exponent;
        if (diff > 16) return this;

        return new BigNumber(this.mantissa - (other.mantissa / Math.pow(10, diff)), this.exponent);
    }

    public BigNumber multiply(BigNumber other) {
        return new BigNumber(this.mantissa * other.mantissa, this.exponent + other.exponent);
    }

    @Override
    public int compareTo(BigNumber o) {
        if (this.exponent != o.exponent) {
            return Double.compare(this.exponent, o.exponent);
        }
        return Double.compare(this.mantissa, o.mantissa);
    }
}