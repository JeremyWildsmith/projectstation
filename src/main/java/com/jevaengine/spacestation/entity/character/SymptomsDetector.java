package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.DamageSeverity;
import com.jevaengine.spacestation.entity.character.symptoms.ISymptom;
import com.jevaengine.spacestation.entity.character.symptoms.RecoveringSuffocation;
import com.jevaengine.spacestation.entity.character.symptoms.Suffocation;
import com.jevaengine.spacestation.gas.GasMetaData;
import com.jevaengine.spacestation.gas.GasType;

import java.util.*;


public enum SymptomsDetector {
    InsufficientOxygen("Insufficient Oxygen", (gas)-> {
        float o2mols = 0;

        if(gas.amount.containsKey(GasType.Oxygen))
            o2mols = gas.amount.get(GasType.Oxygen);

        if(o2mols < ToxicityConstants.MIN_O2_MOLS) {
            DamageSeverity sev = ToxicityConstants.mapSeverity(ToxicityConstants.MIN_O2_MOLS, o2mols, false);
            return new ISymptom[] {
                    new Suffocation(sev)
            };
        }

        return new ISymptom[0];
    }),
    IdealOxygen("Ideal Oxygen", (gas)-> {
        float o2mols = 0;

        if(gas.amount.containsKey(GasType.Oxygen))
            o2mols = gas.amount.get(GasType.Oxygen);

        if(o2mols > ToxicityConstants.MIN_O2_MOLS && o2mols < ToxicityConstants.MAX_O2_MOLS) {
            return new ISymptom[] {
                    new RecoveringSuffocation()
            };
        }

        return new ISymptom[0];
    });

    private final String description;
    private final ISymptomCalculator calculator;

    SymptomsDetector(String description, ISymptomCalculator calculator) {
        this.calculator = calculator;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public static ISymptom[] getToxicitySymptoms(GasMetaData consumed) {
        List<ISymptom> symptoms = new ArrayList<>();

        for(SymptomsDetector d : SymptomsDetector.values()) {
            symptoms.addAll(Arrays.asList(d.calculator.getSymptoms(consumed)));
        }

        return symptoms.toArray(new ISymptom[0]);
    }
}

/**
 * These constants are entirely arbitrary... unfortunately.
 */
class ToxicityConstants {
    public static final float IDEAL_O2_MOLS = 0.009375f;
    public static final float MIN_O2_MOLS = IDEAL_O2_MOLS * 0.9f;
    public static final float MAX_O2_MOLS = IDEAL_O2_MOLS * 1.1f;

    public static DamageSeverity mapSeverity(float upper_threshold, float val, boolean inverted) {
        float index = val / upper_threshold;
        DamageSeverity[] sev = DamageSeverity.ASCENDING_SEVERITY;

        int max = sev.length - 1;
        if(inverted)
            return DamageSeverity.ASCENDING_SEVERITY[Math.round(index * max)];
        else
            return DamageSeverity.ASCENDING_SEVERITY[max - Math.round(index * max)];
    }
}

interface ISymptomCalculator {
    ISymptom[] getSymptoms(GasMetaData gas);
}
