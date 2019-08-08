package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.DamageDescription;
import com.jevaengine.spacestation.DamageSeverity;
import com.jevaengine.spacestation.entity.IDamageConsumer;
import io.github.jevaengine.math.Rect3F;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.math.Vector3F;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.dialogue.NullDialogueResolverFactory;
import io.github.jevaengine.rpg.dialogue.NullDialogueRouteFactory;
import io.github.jevaengine.rpg.entity.character.*;
import io.github.jevaengine.script.IScriptBuilder;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.physics.PhysicsBodyDescription;
import io.github.jevaengine.world.scene.model.DecoratedSceneModel;
import io.github.jevaengine.world.scene.model.IActionSceneModel;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpaceShip extends DefaultRpgCharacter implements IDamageConsumer {
    private final Map<DamageCategory, Integer> baseDamage;
    private final Map<DamageSeverity, Integer> damageMultiplier;
    private final AttributeSet attributes;
    private final ICorpseProducer corpseProducer;
    private final List<List<Gun>> m_gunGroups = new ArrayList<>();

    public SpaceShip(IScriptBuilder scriptBuilder, AttributeSet attributes, ISpaceCharacterStatusResolverFactory statusResolver, IMovementResolverFactory movementResolver, IVisionResolverFactory visionResolver, IAllegianceResolverFactory allegianceResolver, ILoadout loadout, IActionSceneModel model, PhysicsBodyDescription physicsBodyDescription, Map<DamageCategory, Integer> baseDamage, Map<DamageSeverity, Integer> damageMultiplier, ICorpseProducer corpseProducer, String name) {
        super(scriptBuilder, new NullDialogueRouteFactory(), attributes, statusResolver, new NullDialogueResolverFactory(), movementResolver, visionResolver, allegianceResolver, loadout, new DefaultInventory(0), model, physicsBodyDescription, name);
        this.baseDamage = baseDamage;
        this.damageMultiplier = damageMultiplier;
        this.attributes = attributes;
        this.corpseProducer = corpseProducer;

        for(SpaceCharacterAttribute a : SpaceCharacterAttribute.values()) {
            if(!attributes.has(a))
                attributes.get(a).set(a.getDefaultValue());
        }
        List<Gun> mainGroup = new ArrayList<>();
        mainGroup.add(new Gun((float)Math.PI / 10));

        m_gunGroups.add(mainGroup);
    }

    @Override
    public ISpaceCharacterStatusResolver getStatusResolver() {
        return (ISpaceCharacterStatusResolver)super.getStatusResolver();
    }

    @Override
    public IImmutableSceneModel getModel() {
        List<IImmutableSceneModel.ISceneModelComponent> guns = new ArrayList<>();
        for(List<Gun> group : m_gunGroups)
            guns.addAll(group);

        return new DecoratedSceneModel((ISceneModel)super.getModel(), guns);
    }

    public int getGunGroups() {
        return m_gunGroups.size();
    }

    public void selectGunGroup(int index) {
        if(index >= m_gunGroups.size())
            deselectGunGroup();
        else {
            for (int i = 0; i < m_gunGroups.size(); i++) {
                for (Gun g : m_gunGroups.get(i))
                    g.setActive(i == index);
            }
        }
    }

    public void deselectGunGroup() {
        for(int i = 0; i < m_gunGroups.size(); i++) {
            for(Gun g : m_gunGroups.get(i))
                g.setActive(false);
        }
    }

    public void aim(Vector3F location) {
        Vector2F relative = location.difference(this.getBody().getLocation()).getXy();
        for(int i = 0; i < m_gunGroups.size(); i++) {
            for(Gun g : m_gunGroups.get(i))
                g.aim(relative);
        }
    }

    @Override
    public void consume(DamageDescription damage) {
        consume(damage, false);
    }

    public void consume(DamageDescription damage, boolean isRecovery) {
        for(DamageCategory cat : DamageCategory.values()) {
            DamageSeverity s = damage.getDamageSeverity(cat);
            if(s == DamageSeverity.None)
                continue;

            int dmg = baseDamage.containsKey(cat) ? baseDamage.get(cat) : 0;

            if(damageMultiplier.containsKey(s))
                dmg *= damageMultiplier.get(s);

            if(isRecovery)
                dmg = -dmg;

            IImmutableAttributeSet.IAttribute attribute = attributes.get(cat.getAffectedAttribute());
            float newVal = Math.max(0, attribute.get() + dmg);
            attribute.set(newVal);
        }
    }

    @Override
    public void update(int delta) {
        super.update(delta);

        if(getStatusResolver().isDead()) {
            IEntity corpse = corpseProducer.produce();
            if(corpse != null)
            {
                getWorld().addEntity(corpse);
                corpse.getBody().setLocation(this.getBody().getLocation().difference(new Vector3F(0, 0, 0.1f)));
                getWorld().removeEntity(this);
            }
        }

        for(List<Gun> gr : m_gunGroups) {
            for(Gun g : gr)
                g.update(delta);
        }
    }

    public interface ICorpseProducer {
        IEntity produce();
    }

    private final class Gun implements IImmutableSceneModel.ISceneModelComponent {
        private static final int SIGHTS_LENGTH = 400;
        private final float m_rotationSpeed;

        private float m_rotation;
        private float m_targetRotation;
        private boolean m_active = false;
        private boolean m_dir = false;

        public Gun(float m_rotationSpeed) {
            this.m_rotationSpeed = m_rotationSpeed;
        }

        @Override
        public String getName() {
            return "gun";
        }

        @Override
        public boolean testPick(int x, int y, float scale) {
            return false;
        }

        @Override
        public Rect3F getBounds() {
            return new Rect3F();
        }

        @Override
        public Vector3F getOrigin() {
            return new Vector3F(3, 0);
        }

        public void setActive(boolean active) {
            m_active = active;
        }

        public void aim(Vector2F location) {
            if(!m_active)
                return;

            m_targetRotation = location.add(this.getOrigin().getXy()).getAngle();


            Vector2F cur = new Vector2F(1, 0).rotate(m_rotation);
            Vector2F dest = new Vector2F(1, 0).rotate(m_targetRotation);
            m_dir = (cur.x * dest.y + dest.y * cur.x) < 0;

        }

        public void update(int deltaTime) {
            if(Math.abs(m_rotation - m_targetRotation) <= Math.PI / 48)
                return;

            float movement = deltaTime / 1000.0f * m_rotationSpeed;
            if(m_dir) {
                movement = -movement;
            }

            m_rotation += movement;
        }

        @Override
        public void render(Graphics2D g, int x, int y, float scale) {
            Vector2D render_target = new Vector2F(1, 0).rotate(m_rotation).multiply(scale * SIGHTS_LENGTH).round();
            render_target = render_target.add(new Vector2D(x, y));

            if(m_active)
                g.setColor(Color.red);
            else
                g.setColor(Color.yellow);

            Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9 * scale}, 0);
            g.setStroke(dashed);

            g.drawLine(x, y, render_target.x, render_target.y);
        }
    }
}
