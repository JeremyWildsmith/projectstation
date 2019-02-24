package com.jevaengine.spacestation;

import io.github.jevaengine.config.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DamageDescription implements ISerializable, Serializable {
    private Map<DamageCategory, DamageSeverity> damageSeverityMapping = new HashMap<>();

    public DamageDescription(Map<DamageCategory, DamageSeverity> desc) {
        damageSeverityMapping = new HashMap<>(desc);
    }

    public DamageDescription() {
    }

    public DamageSeverity getDamageSeverity(DamageCategory category) {
        if(!damageSeverityMapping.containsKey(category))
            return DamageSeverity.None;

        return damageSeverityMapping.get(category);
    }

    @Override
    public void serialize(IVariable target) throws ValueSerializationException {
        for(Map.Entry<DamageCategory, DamageSeverity> e : damageSeverityMapping.entrySet())
            target.addChild(e.getKey().toString()).setValue(e.getValue().toString());
    }

    private static DamageSeverity tryReadSeverity(IImmutableVariable source, String name) throws ValueSerializationException, NoSuchChildVariableException {
        if(source.childExists(name)) {
            String sev = source.getChild(name).getValue(String.class);
            for(DamageSeverity s : DamageSeverity.values()) {
                if(sev.compareTo(s.toString()) == 0)
                    return s;
            }

            throw new ValueSerializationException("Unrecognized damage severity: " + sev);
        } else
            return DamageSeverity.None;
    }

    @Override
    public void deserialize(IImmutableVariable source) throws ValueSerializationException {
        try {
            for(DamageCategory cat : DamageCategory.values())
                damageSeverityMapping.put(cat, tryReadSeverity(source, cat.toString()));
        } catch (ValueSerializationException | NoSuchChildVariableException ex) {
            throw new ValueSerializationException(ex);
        }
    }
}
