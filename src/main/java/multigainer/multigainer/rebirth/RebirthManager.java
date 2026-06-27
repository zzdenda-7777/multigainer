package multigainer.multigainer.rebirth;

import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.upgrades.UpgradeManager;

public class RebirthManager {
    public static final double REBIRTH_THRESHOLD = 500000.0;

    // money^(1/3) via log10 — works for any BigNumber magnitude
    public static BigNumber calculateRebirthPoints(BigNumber money) {
        if (money == null || money.getMantissa() == 0) return new BigNumber(0);
        double log10money = money.getExponent() + Math.log10(money.getMantissa());
        return UpgradeManager.fromLog10(log10money / 3.0);
    }

    // 1 + RP^(1/15) via log10 — works for any BigNumber magnitude
    public static BigNumber calculateMoneyMultiplier(BigNumber rebirthPoints) {
        if (rebirthPoints == null || rebirthPoints.getMantissa() == 0) return new BigNumber(1.0);
        double log10rp = rebirthPoints.getExponent() + Math.log10(rebirthPoints.getMantissa());
        BigNumber power = UpgradeManager.fromLog10(log10rp / 15.0);
        return power.add(new BigNumber(1.0));
    }
}
