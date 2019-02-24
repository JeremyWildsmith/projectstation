package com.jevaengine.spacestation.entity.character;

import io.github.jevaengine.rpg.IImmutableAttributeSet;

public enum SpaceCharacterAttribute implements IImmutableAttributeSet.IAttributeIdentifier {
    BruteDamage(0, "Brute", "The amount of brute damaged incurred"),
    ElectricalDamage(0, "Electrical", "The amount of electrical damage incurred"),
    BurnDamage(0, "Burn", "The amount of burn damaged incurred"),
    SuffocationDamage(0, "Suffocation", "The amount of suffocation damaged incurred"),
    ToxinDamage(0, "Toxin", "The amount of toxin damage incurred"),
    HeartRateBpm(60, "Heart Rate", "The heart-rate in BPM"),
    BloodVolumeMl(5700, "Blood Volume", "The volume of blood in a person."),
    Nutrition(100, "nutrition", "The nutrition level"),
    Fatigue(0, "Fatigue", "Level of fatigue"),
    Speed(10, "Speed", "Movement speed"),
    EffectiveHitpoints(100, "Hitpoints", "The effective hitpoints."),
    MaxHitpoints(100, "Max Hitpoints", "The number of hitpoints if no damaged was endured."),
    TemperatureKelvn(310, "Body Temperature", "Body temperature"),
    BreathVolumeMl(500, "Breath Volume", "Volume of air consumed in one breath.");

    private final int defaultValue;
    private final String name;
    private final String description;

    SpaceCharacterAttribute(int defaultValue, String name, String description) {
        this.defaultValue = defaultValue;
        this.name = name;
        this.description = description;
    }

    public int getDefaultValue() {
        return defaultValue;
    }


    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
