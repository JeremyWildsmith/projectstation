package com.jevaengine.spacestation;

import io.github.jevaengine.config.*;

import java.io.Serializable;

public class DamageDescription implements ISerializable, Serializable {
    private DamageSeverity brute = DamageSeverity.None;
    private DamageSeverity burn = DamageSeverity.None;
    private DamageSeverity electrical = DamageSeverity.None;
    private DamageSeverity suffocation = DamageSeverity.None;
    private DamageSeverity toxin = DamageSeverity.None;

    public DamageDescription() {

    }

    public DamageDescription(DamageSeverity brute, DamageSeverity burn, DamageSeverity suffocation, DamageSeverity toxin, DamageSeverity electrical) {
        this.brute = brute;
        this.burn = burn;
        this.suffocation = suffocation;
        this.toxin = toxin;
        this.electrical = electrical;
    }

    public DamageSeverity getBrute() {
        return brute;
    }

    public DamageSeverity getBurn() {
        return burn;
    }

    public DamageSeverity getSuffocation() {
        return suffocation;
    }

    public DamageSeverity getToxin() {
        return toxin;
    }

    @Override
    public void serialize(IVariable target) throws ValueSerializationException {
        target.addChild("brute").setValue(brute.toString());
        target.addChild("burn").setValue(burn.toString());
        target.addChild("suffocation").setValue(suffocation.toString());
        target.addChild("toxin").setValue(toxin.toString());
        target.addChild("electrical").setValue(electrical.toString());
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
            this.brute = tryReadSeverity(source, "brute");
            this.burn = tryReadSeverity(source, "burn");
            this.suffocation = tryReadSeverity(source, "suffocation");
            this.toxin = tryReadSeverity(source, "toxin");
            this.electrical = tryReadSeverity(source, "electrical");
        } catch (ValueSerializationException | NoSuchChildVariableException ex) {
            throw new ValueSerializationException(ex);
        }
    }
}
