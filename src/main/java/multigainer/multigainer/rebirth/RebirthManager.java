package multigainer.multigainer.rebirth;

import multigainer.multigainer.math.BigNumber;

public class RebirthManager {
    public static final double REBIRTH_THRESHOLD = 500000.0;

    // Calculation: Math.cbrt(money) (Cube root of 3rd power/Money)
    public static double calculateRebirthPoints(double money) {
        // Pokud je money 0 nebo méně, vracíme 0, nebudeme dělit
        if (money <= 0) return 0.0;

        // Zde teprve proveď svůj výpočet, např.:
        return Math.pow(money, 1.0 / 3.0);
    }

    // Calculation: Sqrt(15) of Rebirth Points
    public static double calculateMoneyMultiplier(double rebirthPoints) {
        return 1.0 + Math.pow(rebirthPoints, 1.0 / 15.0);
    }
}
