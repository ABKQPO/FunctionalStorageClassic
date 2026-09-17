package com.hfstudio.functionalstorage.api.upgrade;

import java.util.Objects;

import lombok.Getter;

/**
 * An immutable numeric contribution made by a storage upgrade.
 *
 * <p>
 * Evaluation is independent of the order in which operation kinds were added. All
 * {@link Operation#SET_BASE} operations are applied first, followed by
 * {@link Operation#ADD_BASE}, then {@link Operation#MULTIPLY}. Contributions of the same kind
 * retain their iteration order. Calculations use {@code double}; a negative, NaN, or negatively
 * infinite result is normalized to zero.
 * </p>
 *
 * <p>
 * Instances are thread-safe. Collections supplied to {@link #calculate(Iterable, double)}
 * are only read and must not be mutated concurrently.
 * </p>
 */
@Getter
public class UpgradeModifier {

    private final Operation operation;
    private final double value;

    public UpgradeModifier(Operation operation, double value) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.value = value;
    }

    public static UpgradeModifier setBase(double value) {
        return new UpgradeModifier(Operation.SET_BASE, value);
    }

    public static UpgradeModifier addBase(double value) {
        return new UpgradeModifier(Operation.ADD_BASE, value);
    }

    public static UpgradeModifier multiply(double value) {
        return new UpgradeModifier(Operation.MULTIPLY, value);
    }

    /**
     * Evaluates modifiers in the fixed SET_BASE, ADD_BASE, MULTIPLY order.
     *
     * @param modifiers   contributions to evaluate; never {@code null} and containing no nulls
     * @param defaultBase base value used when no SET_BASE modifier is present
     * @return a value greater than or equal to zero
     * @throws NullPointerException if the iterable or one of its values is {@code null}
     */
    public static double calculate(Iterable<UpgradeModifier> modifiers, double defaultBase) {
        Objects.requireNonNull(modifiers, "modifiers");
        double base = defaultBase;
        double additions = 0D;
        double factor = 1.0D;
        for (UpgradeModifier modifier : modifiers) {
            Objects.requireNonNull(modifier, "modifier");
            switch (modifier.operation) {
                case SET_BASE -> base = modifier.value;
                case ADD_BASE -> additions += modifier.value;
                case MULTIPLY -> factor *= modifier.value;
            }
        }

        double result = (base + additions) * factor;
        return Double.isNaN(result) || result <= 0.0D ? 0.0D : result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UpgradeModifier that)) {
            return false;
        }
        return operation == that.operation && Double.doubleToLongBits(value) == Double.doubleToLongBits(that.value);
    }

    @Override
    public int hashCode() {
        return 31 * operation.hashCode() + Long.hashCode(Double.doubleToLongBits(value));
    }

    @Override
    public String toString() {
        return "UpgradeModifier{" + operation + ", value=" + value + '}';
    }

    public enum Operation {
        /**
         * Replace the current base value. If repeated, the last value wins.
         */
        SET_BASE,
        ADD_BASE,
        MULTIPLY
    }
}
