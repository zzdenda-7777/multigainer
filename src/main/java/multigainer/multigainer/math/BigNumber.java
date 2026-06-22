package multigainer.multigainer.math;

public class BigNumber implements Comparable<BigNumber> {
    private final double mantissa;
    private final double exponent;

    public BigNumber(double value) {
        if (value == 0) {
            this.mantissa = 0;
            this.exponent = 0;
        } else {
            double log = Math.floor(Math.log10(Math.abs(value)));
            this.mantissa = value / Math.pow(10, log);
            this.exponent = log;
        }
    }

    // Integrated Constructor: Correctly assigns final fields exactly once
    public BigNumber(double mantissa, double exponent) {
        double calcMantissa = mantissa;
        double calcExponent = exponent;

        if (calcMantissa == 0) {
            calcMantissa = 0;
            calcExponent = 0;
        } else if (Math.abs(calcMantissa) >= 10.0 || Math.abs(calcMantissa) < 1.0) {
            double log = Math.floor(Math.log10(Math.abs(calcMantissa)));
            calcMantissa = calcMantissa / Math.pow(10, log);
            calcExponent = calcExponent + log;
        }

        this.mantissa = calcMantissa;
        this.exponent = calcExponent;
    }

    public double getMantissa() { return mantissa; }
    public double getExponent() { return exponent; }

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