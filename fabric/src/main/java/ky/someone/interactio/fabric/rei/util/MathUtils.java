package ky.someone.interactio.fabric.rei.util;

import me.shedaniel.rei.api.fractions.Fraction;

// dan pls
public class MathUtils {
    public static Fraction reiFraction(me.shedaniel.architectury.utils.Fraction fraction) {
        return Fraction.of(fraction.getNumerator(), fraction.getDenominator());
    }

    public static me.shedaniel.architectury.utils.Fraction archFraction(Fraction fraction) {
        return me.shedaniel.architectury.utils.Fraction.of(fraction.getNumerator(), fraction.getDenominator());
    }
}
