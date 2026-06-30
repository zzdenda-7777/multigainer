package multigainer.multigainer.formatting;

import multigainer.multigainer.math.BigNumber;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Formats BigNumber values for display.
 *
 * Two display modes (per-player toggle via slot-8 compass):
 *   SUFFIX     →  1.23UnDc   (Conway–Wechsler, infinitely scalable — default)
 *   SCIENTIFIC →  1.23e33    (e-notation from the very first number)
 *
 * Suffix tiers:
 *   1  → K      (10^3)
 *   2  → M      (10^6)
 *   3  → B      (10^9)
 *   4  → T      (10^12)
 *   5  → Qa     (10^15)
 *   6  → Qi     (10^18)
 *   7  → Sx     (10^21)
 *   8  → Sp     (10^24)
 *   9  → Oc     (10^27)
 *   10 → No     (10^30)
 *   11 → Dc     (10^33)   ← algorithmic from here, never runs out
 *   12 → UnDc   (10^36)
 *   21 → Vg     (10^63)
 *   101→ Ct     (10^303)
 *   1001→ UnMi  (10^3003)
 *   333333333 → TrTgTctBiTrTgTctMiDuTgTct  (10^10^9)
 */
public class NumberFormatter {

    // ── Per-player format preference ──────────────────────────────────────────

    public enum FormatMode { SUFFIX, SCIENTIFIC }

    private static final Map<UUID, FormatMode> playerModes = new ConcurrentHashMap<>();

    public static void setMode(UUID id, FormatMode mode)  { if (id != null) playerModes.put(id, mode); }
    public static FormatMode getMode(UUID id)              { return id == null ? FormatMode.SUFFIX : playerModes.getOrDefault(id, FormatMode.SUFFIX); }
    public static boolean isSuffix(UUID id)                { return getMode(id) == FormatMode.SUFFIX; }
    public static void clearMode(UUID id)                  { if (id != null) playerModes.remove(id); }

    public static void toggleMode(UUID id) {
        setMode(id, getMode(id) == FormatMode.SUFFIX ? FormatMode.SCIENTIFIC : FormatMode.SUFFIX);
    }

    // ── Format entry points ───────────────────────────────────────────────────

    /** Default: SUFFIX mode. Used everywhere a player UUID is not available. */
    public static String format(BigNumber value) { return format(value, FormatMode.SUFFIX); }

    /** Per-player mode. Pass the viewing player's UUID when inside a GUI or scoreboard. */
    public static String format(BigNumber value, UUID playerId) { return format(value, getMode(playerId)); }

    public static String format(BigNumber value, FormatMode mode) {
        return format(value, mode, 0);
    }

    /**
     * Interní přetížení s hloubkou rekurze - pojistka proti tomu, aby
     * SCIENTIFIC mód u extrémně vysokých (capnutých) exponentů nemohl
     * skončit v zacyklení nebo vyprodukovat NaN/Infinity text.
     */
    private static String format(BigNumber value, FormatMode mode, int recursionDepth) {
        if (value == null || isInvalid(value) || value.getMantissa() == 0) {
            return "0";
        }

        double mantissa = value.getMantissa();
        double exp      = value.getExponent();

        // Pojistka: pokud se i přes ochrany v BigNumber dostane NaN/Infinity
        // až sem, nevypisujeme to hráči jako "NaN" nebo "Infinity".
        if (Double.isNaN(mantissa) || Double.isInfinite(mantissa)
                || Double.isNaN(exp) || Double.isInfinite(exp)) {
            return "∞";
        }

        // Pojistka proti přílišné hloubce rekurze ve SCIENTIFIC módu -
        // i kdyby exponent z nějakého důvodu neklesal dost rychle mezi
        // voláními, po 5 úrovních prostě ukončíme formátování.
        if (recursionDepth > 5) {
            return String.format(Locale.US, "%.2fe%.0f", mantissa, exp);
        }

        // ── SCIENTIFIC mode: always e-notation ───────────────────────────────
        if (mode == FormatMode.SCIENTIFIC) {
            if (exp < 1_000_000) {
                return String.format(Locale.US, "%.2fe%.0f", mantissa, exp);
            }
            // Exponent is itself huge — format it recursively
            return String.format(Locale.US, "%.2fe%s", mantissa,
                    format(new BigNumber(exp), FormatMode.SCIENTIFIC, recursionDepth + 1));
        }

        // ── SUFFIX mode ───────────────────────────────────────────────────────
        // Values below 1000: show raw
        if (exp < 3) {
            double raw = mantissa * Math.pow(10, exp);
            if (Double.isNaN(raw) || Double.isInfinite(raw)) {
                return "∞";
            }
            return (raw % 1 == 0)
                    ? String.format(Locale.US, "%.0f", raw)
                    : String.format(Locale.US, "%.2f", raw).replaceAll("\\.?0+$", "");
        }

        long tier      = (long) Math.floor(exp / 3.0);
        int  remainder = (int)  (exp % 3);
        if (remainder < 0) remainder += 3;
        double displayMantissa = mantissa * Math.pow(10, remainder);

        if (Double.isNaN(displayMantissa) || Double.isInfinite(displayMantissa)) {
            return "∞";
        }

        // If tier exceeds int range fall back to scientific
        if (tier > Integer.MAX_VALUE) {
            if (exp < 1_000_000) return String.format(Locale.US, "%.2fe%.0f", mantissa, exp);
            return String.format(Locale.US, "%.2fe%s", mantissa,
                    format(new BigNumber(exp), FormatMode.SCIENTIFIC, recursionDepth + 1));
        }

        return String.format(Locale.US, "%.2f%s", displayMantissa, getSuffix((int) tier));
    }

    private static boolean isInvalid(BigNumber value) {
        double m = value.getMantissa();
        double e = value.getExponent();
        return Double.isNaN(m) || Double.isNaN(e);
    }

    // ── Conway–Wechsler suffix system (infinite) ──────────────────────────────

    private static final String[] BASE = {
            "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No"
    };

    // Latin ones (0–9)
    private static final String[] ONES = {
            "", "Un", "Du", "Tr", "Qa", "Qi", "Sx", "Sp", "Oc", "Nv"
    };
    // Latin tens (0–9)
    private static final String[] TENS = {
            "", "Dc", "Vg", "Tg", "Qg", "Qn", "Sg", "Spg", "Og", "Ng"
    };
    // Latin hundreds (0–9)
    private static final String[] HUNDREDS = {
            "", "Ct", "Dct", "Tct", "Qct", "Qnct", "Sct", "Spct", "Oct", "Nct"
    };

    /**
     * Returns the suffix string for tier (1-indexed).
     * Tier 1 = K, tier 2 = M, …, tier 11 = Dc, tier 12 = UnDc, …
     * Works for any non-negative int — never returns a fallback or runs out.
     */
    public static String getSuffix(int tier) {
        if (tier <= 0)           return "";
        if (tier <= BASE.length) return BASE[tier - 1];

        // n is the 0-indexed "illion" number (n=10 → decillion)
        int n = tier - 1;

        int billions  = n / 1_000_000_000;
        int millions  = (n / 1_000_000) % 1_000;
        int thousands = (n / 1_000)     % 1_000;
        int rem       = n % 1_000;

        StringBuilder sb = new StringBuilder();
        if (billions  > 0) sb.append(group(billions)).append("Gi");
        if (millions  > 0) sb.append(group(millions)).append("Bi");
        if (thousands > 0) sb.append(group(thousands)).append("Mi");
        sb.append(group(rem));

        return sb.toString();
    }

    /** Converts a 0–999 number into its Latin component string. */
    private static String group(int n) {
        if (n <= 0) return "";
        int h = n / 100;
        int t = (n / 10) % 10;
        int u = n % 10;
        return ONES[u] + TENS[t] + HUNDREDS[h];
    }
}