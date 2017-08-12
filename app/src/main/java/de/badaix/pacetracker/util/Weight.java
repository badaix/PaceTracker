package de.badaix.pacetracker.util;

import de.badaix.pacetracker.settings.GlobalSettings;

public class Weight {
    public final static double kilogram = 1.0;
    public final static double pound = 0.453592370;

    public static double weightToDouble(double kilograms, Unit unit) {
        return kilograms / unit.getFactor();
    }

    public static double weightToKilograms(double weight) {
        return weight * GlobalSettings.getInstance().getWeightUnit().getFactor();
    }

    public static double weightToDouble(double kilograms) {
        return weightToDouble(kilograms, GlobalSettings.getInstance().getWeightUnit());
    }

    public static String weightToString(double weightKilograms, Unit unit, int precision) {
        return Distance.doubleToString(weightToDouble(weightKilograms, unit), precision);
    }

    public static String weightToString(double weightKilograms, int precision) {
        return Distance.doubleToString(weightToDouble(weightKilograms), precision);
    }

    public enum System {
        METRIC, IMPERIAL;
    }

    public enum Unit {
        KILOGRAM, POUND;

        public String toShortString() {
            if (this == KILOGRAM)
                return "kg";
            else if (this == POUND)
                return "lbs";
            else
                return "";
        }

        public double getFactor() {
            if (this == KILOGRAM)
                return kilogram;
            else if (this == POUND)
                return pound;
            else
                return kilogram;
        }
    }
}
