package com.hfstudio.functionalstorage.api.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;

/**
 * Immutable snapshot of all contributions made by installed storage upgrades.
 *
 * <p>
 * The snapshot owns copies of all builder collections. Returned maps, lists, and sets are
 * unmodifiable and remain stable if the originating builder is reused. Instances are therefore
 * thread-safe after construction; {@link Builder} itself is mutable and not thread-safe.
 * </p>
 */
@Getter
public class UpgradeState {

    private static final UpgradeState EMPTY = new Builder().build();

    private final Map<UpgradeAttribute, List<UpgradeModifier>> modifiers;
    private final Set<StorageFeature> features;

    private UpgradeState(Builder builder) {
        EnumMap<UpgradeAttribute, List<UpgradeModifier>> modifierCopies = new EnumMap<>(UpgradeAttribute.class);
        for (Map.Entry<UpgradeAttribute, List<UpgradeModifier>> entry : builder.modifiers.entrySet()) {
            modifierCopies.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        modifiers = Collections.unmodifiableMap(modifierCopies);
        features = builder.features.isEmpty() ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(builder.features));
    }

    /**
     * @return the shared empty upgrade snapshot
     */
    public static UpgradeState empty() {
        return EMPTY;
    }

    /**
     * @return a new mutable builder with no contributions
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Evaluates an attribute using this snapshot's modifiers.
     *
     * @param attribute   attribute to evaluate
     * @param defaultBase base value supplied by the storage implementation
     * @return the evaluated non-negative value
     */
    public double calculate(UpgradeAttribute attribute, double defaultBase) {
        return UpgradeModifier.calculate(getModifiers(attribute), defaultBase);
    }

    /**
     * @param feature feature to test
     * @return whether the specified feature is enabled
     */
    public boolean hasFeature(StorageFeature feature) {
        return features.contains(Objects.requireNonNull(feature, "feature"));
    }

    /**
     * @param attribute attribute to read
     * @return the immutable modifiers for one attribute, in insertion order
     */
    public List<UpgradeModifier> getModifiers(UpgradeAttribute attribute) {
        List<UpgradeModifier> values = modifiers.get(Objects.requireNonNull(attribute, "attribute"));
        return values == null ? Collections.emptyList() : values;
    }

    /**
     * Mutable, reusable accumulator for creating immutable {@link UpgradeState} snapshots.
     */
    public static class Builder {

        private final EnumMap<UpgradeAttribute, List<UpgradeModifier>> modifiers = new EnumMap<>(
            UpgradeAttribute.class);
        private final EnumSet<StorageFeature> features = EnumSet.noneOf(StorageFeature.class);

        /**
         * Adds one numeric contribution and returns this builder.
         *
         * @param attribute modified attribute
         * @param modifier  contribution to add
         * @return this builder
         */
        public Builder addModifier(UpgradeAttribute attribute, UpgradeModifier modifier) {
            modifiers.computeIfAbsent(Objects.requireNonNull(attribute, "attribute"), key -> new ArrayList<>())
                .add(Objects.requireNonNull(modifier, "modifier"));
            return this;
        }

        /**
         * Adds every contribution from a map and returns this builder.
         *
         * @param values contributions to add
         * @return this builder
         */
        public Builder addModifiers(Map<UpgradeAttribute, UpgradeModifier> values) {
            Objects.requireNonNull(values, "values");
            for (Map.Entry<UpgradeAttribute, UpgradeModifier> entry : values.entrySet()) {
                addModifier(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Enables a feature and returns this builder.
         *
         * @param feature feature to enable
         * @return this builder
         */
        public Builder addFeature(StorageFeature feature) {
            features.add(Objects.requireNonNull(feature, "feature"));
            return this;
        }

        /**
         * Copies all contributions from an existing snapshot into this builder.
         *
         * @param state snapshot to copy
         * @return this builder
         */
        public Builder addAll(UpgradeState state) {
            Objects.requireNonNull(state, "state");
            for (Map.Entry<UpgradeAttribute, List<UpgradeModifier>> entry : state.modifiers.entrySet()) {
                for (UpgradeModifier modifier : entry.getValue()) {
                    addModifier(entry.getKey(), modifier);
                }
            }
            features.addAll(state.features);
            return this;
        }

        /**
         * @return a detached immutable snapshot of the current contributions
         */
        public UpgradeState build() {
            return new UpgradeState(this);
        }
    }
}
