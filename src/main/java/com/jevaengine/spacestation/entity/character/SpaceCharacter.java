package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.DamageDescription;
import com.jevaengine.spacestation.DamageSeverity;
import com.jevaengine.spacestation.entity.IDamageConsumer;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.dialogue.IDialogueRouteFactory;
import io.github.jevaengine.rpg.entity.character.*;
import io.github.jevaengine.rpg.item.IItemStore;
import io.github.jevaengine.script.IScriptBuilder;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.physics.PhysicsBodyDescription;
import io.github.jevaengine.world.scene.model.IActionSceneModel;

import java.util.Map;

public class SpaceCharacter extends DefaultRpgCharacter implements IDamageConsumer {
    private static final int BREATH_INTERVAL = 1500;
    private int lastBreath = 0;

    private final Map<DamageCategory, Integer> baseDamage;
    private final Map<DamageSeverity, Integer> damageMultiplier;
    private final AttributeSet attributes;
    private final ICorpseProducer corpseProducer;

    public SpaceCharacter(IScriptBuilder scriptBuilder, IDialogueRouteFactory dialogueRotueFactory, AttributeSet attributes, IStatusResolverFactory statusResolver, IDialogueResolverFactory dialogueResolver, IMovementResolverFactory movementResolver, IVisionResolverFactory visionResolver, IAllegianceResolverFactory allegianceResolver, ILoadout loadout, IItemStore inventory, IActionSceneModel model, PhysicsBodyDescription physicsBodyDescription, Map<DamageCategory, Integer> baseDamage, Map<DamageSeverity, Integer> damageMultiplier, ICorpseProducer corpseProducer, String name) {
        super(scriptBuilder, dialogueRotueFactory, attributes, statusResolver, dialogueResolver, movementResolver, visionResolver, allegianceResolver, loadout, inventory, model, physicsBodyDescription, name);
        this.baseDamage = baseDamage;
        this.damageMultiplier = damageMultiplier;
        this.attributes = attributes;
        this.corpseProducer = corpseProducer;

        for(SpaceCharacterAttribute a : SpaceCharacterAttribute.values()) {
            if(!attributes.has(a))
                attributes.get(a).set(a.getDefaultValue());
        }
    }

    @Override
    public void consume(DamageDescription damage) {
        for(DamageCategory cat : DamageCategory.values()) {
            DamageSeverity s = damage.getDamageSeverity(cat);
            if(s == DamageSeverity.None)
                continue;

            int dmg = baseDamage.containsKey(cat) ? baseDamage.get(cat) : 0;

            if(damageMultiplier.containsKey(s))
                dmg *= damageMultiplier.get(s);

            IImmutableAttributeSet.IAttribute attribute = attributes.get(cat.getAffectedAttribute());
            float newVal = attribute.get() + dmg;
            attribute.set(newVal);
        }
    }

    private void tryBreath() {
        //If not able to breath, add suffocation damage

/*        GasSimulationEntity sim = world.getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);
*/
        Vector2D location = getBody().getLocation().getXy().round();
        /*System.out.println(location.x + ", " + location.y + ": ");
                Map<GasType, Float> quantity = new HashMap<>();//sim.getQuantity(location);
                for(Map.Entry<GasType, Float> e : quantity.entrySet())
                    System.out.println("\t" + e.getKey().name() + ", " + e.getValue());*/
    }

    @Override
    public void update(int delta) {
        super.update(delta);

        lastBreath += delta;

        if(lastBreath >= BREATH_INTERVAL) {
            tryBreath();
            lastBreath = 0;
        }

        if(getStatusResolver().isDead()) {
            IEntity corpse = corpseProducer.produce();
            if(corpse != null)
            {
                getWorld().addEntity(corpse);
                corpse.getBody().setLocation(this.getBody().getLocation().difference(new Vector3F(0, 0, 0.1f)));
                getWorld().removeEntity(this);
            }
        }
    }

    public interface ICorpseProducer {
        IEntity produce();
    }
}
