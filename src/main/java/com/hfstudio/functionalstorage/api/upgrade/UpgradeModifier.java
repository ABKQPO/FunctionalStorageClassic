package com.hfstudio.functionalstorage.api.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An immutable numeric contribution made by a storage upgrade.
 *
 * <p>Evaluation is independent of the order in which operation kinds were added. All
 * {@link Operation#SET_BASE} operations are applied first, followed by
 * {@link Operation#ADD_BASE}, then {@link Operation#MULTIPLY}. Contributions of the same kind
 * retain their iteration order. Calculations use {@code double}; a negative, NaN, or negatively
 * infinite result is normalized to zero.</p>
 *
 * <p>Instances are thread-safe. Collections supplied to {@link #calculate(Iterable, double)}
 * are only read and must not be mutated concurrently.</p>
 */
public class UpgradeModifier {

    private final Operation operation;
    private final double value;

    /**
     * Creates a modifier.
     *
     * @param operation operation to perform; never {@code null}
     * @param value     operand used by the operation
     * @throws NullPointerException if {@code operation} is {@code null}
     */
    public UpgradeModifier(Operation operation, double value) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.value = value;
    }

    /**
     * @param value replacement base value
     * @return a modifier that replaces the base value
     */
    public static UpgradeModifier setBase(double value) {
        return new UpgradeModifier(Operation.SET_BASE, value);
    }

    /**
     * @param value amount added to the base value
     * @return a modifier that adds to the base value
     */
    public static UpgradeModifier addBase(double value) {
        return new UpgradeModifier(Operation.ADD_BASE, value);
    }

    /**
     * @param value factor applied to the adjusted base value
     * @return a modifier that multiplies the adjusted base value
     */
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
        List<UpgradeModifier> ordered = new ArrayList<>();
        for (UpgradeModifier modifier : modifiers) {
            ordered.add(Objects.requireNonNull(modifier, "modifier"));
        }
        double base = defaultBase;

        for (UpgradeModifier modifier : ordered) {
            if (modifier.operation == Operation.SET_BASE) {
                base = modifier.value;
            }
        }
        for (UpgradeModifier modifier : ordered) {
            if (modifier.operation == Operation.ADD_BASE) {
                base += modifier.value;
            }
        }

        double factor = 1.0D;
        for (UpgradeModifier modifier : ordered) {
            if (modifier.operation == Operation.MULTIPLY) {
                factor *= modifier.value;
            }
        }

        double result = base * factor;
        return Double.isNaN(result) || result <= 0.0D ? 0.0D : result;
    }

    /**
     * @return this modifier's operation
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * @return this modifier's operand
     */
    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UpgradeModifier)) {
            return false;
        }
        UpgradeModifier that = (UpgradeModifier) other;
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

    /**
     * Supported modifier operations, listed in their fixed evaluation order.
     */
    public enum Operation {
        /**
         * Replace the current base value. If repeated, the last value wins.
         */
        SET_BASE,
        /**
         * Add to the base value after every base replacement has run.
         */
        ADD_BASE,
        /**
         * Multiply the fully adjusted base value.
         */
        MULTIPLY
    }
}
