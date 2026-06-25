package multigainer.multigainer.formatting;

import multigainer.multigainer.math.BigNumber;
import java.util.Locale;

public class NumberFormatter {

    private static final String[] SUFFIXES = {
            "k", "m", "b", "t", "qd", "qn", "sx", "sp", "oc", "no",
            "dc", "udc", "ddc", "tdc", "qddc", "qndc", "sxdc", "spdc", "ocdc", "nodc",
            "V", "unV", "duV", "trV", "qdV", "qnV", "sxV", "spV", "ocV", "noV",
            "c", "uc", "duc", "trc", "qdc", "qnc", "sxc", "spc", "occ", "noc"
    };

    public static String format(BigNumber value) {
        if (value.getMantissa() == 0) return "0";

        double exp = value.getExponent();

        // Fixed: Clear trailing decimals for clean numbers under 1000 across scoreboards & holograms
        if (exp < 3) {
            double raw = value.getMantissa() * Math.pow(10, exp);
            if (raw % 1 == 0) {
                return String.format(Locale.US, "%.0f", raw);
            }
            return String.format(Locale.US, "%.2f", raw).replaceAll("\\.?0+$", "");
        }

        double indexFloor = Math.floor(exp / 3.0);
        int remainder = (int) (exp % 3);
        if (remainder < 0) remainder += 3;

        double displayedMantissa = value.getMantissa() * Math.pow(10, remainder);

        if (indexFloor <= SUFFIXES.length) {
            return String.format(Locale.US, "%.2f%s", displayedMantissa, SUFFIXES[(int) indexFloor - 1]);
        } else {
            double engineeringExponent = indexFloor * 3.0;
            String expStr = (engineeringExponent < 1000000)
                    ? String.format(Locale.US, "%.0f", engineeringExponent)
                    : format(new BigNumber(engineeringExponent));

            return String.format(Locale.US, "%.2fe%s", displayedMantissa, expStr);
        }
    }
}