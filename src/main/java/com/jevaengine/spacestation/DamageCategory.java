package com.jevaengine.spacestation;

import com.jevaengine.spacestation.entity.character.SpaceCharacterAttribute;

public enum DamageCategory {
    Electrical(SpaceCharacterAttribute.ElectricalDamage),
    Burn(SpaceCharacterAttribute.BurnDamage),
    Brute(SpaceCharacterAttribute.BruteDamage),
    Toxin(SpaceCharacterAttribute.ToxinDamage),
    Suffocation(SpaceCharacterAttribute.SuffocationDamage);

    private final SpaceCharacterAttribute affect;

    DamageCategory(SpaceCharacterAttribute affect) {
        this.affect = affect;
    }

    public SpaceCharacterAttribute getAffectedAttribute() {
        return affect;
    }
}
