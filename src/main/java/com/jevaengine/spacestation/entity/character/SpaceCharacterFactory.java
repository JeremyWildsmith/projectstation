/*
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.jevaengine.spacestation.entity.character;

import com.jevaengine.spacestation.DamageCategory;
import com.jevaengine.spacestation.DamageSeverity;
import com.jevaengine.spacestation.entity.ItemDrop;
import com.jevaengine.spacestation.item.SpaceCharacterWieldTarget;
import io.github.jevaengine.audio.IAudioClipFactory;
import io.github.jevaengine.config.*;
import io.github.jevaengine.config.IConfigurationFactory.ConfigurationConstructionException;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet.IAttributeIdentifier;
import io.github.jevaengine.rpg.IImmutableAttributeSet.IImmutableAttribute;
import io.github.jevaengine.rpg.dialogue.IDialogueRoute;
import io.github.jevaengine.rpg.dialogue.IDialogueRouteFactory;
import io.github.jevaengine.rpg.dialogue.IDialogueRouteFactory.DialogueRouteConstructionException;
import io.github.jevaengine.rpg.dialogue.NullDialogueRoute;
import io.github.jevaengine.rpg.entity.character.*;
import io.github.jevaengine.rpg.item.IImmutableItemStore;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItem.IWieldTarget;
import io.github.jevaengine.rpg.item.IItemFactory;
import io.github.jevaengine.rpg.item.IItemFactory.ItemContructionException;
import io.github.jevaengine.script.IScriptBuilder;
import io.github.jevaengine.script.IScriptBuilderFactory;
import io.github.jevaengine.script.IScriptBuilderFactory.ScriptBuilderConstructionException;
import io.github.jevaengine.script.NullScriptBuilder;
import io.github.jevaengine.ui.style.IUIStyleFactory;
import io.github.jevaengine.ui.style.IUIStyleFactory.UIStyleConstructionException;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.physics.PhysicsBodyDescription;
import io.github.jevaengine.world.physics.PhysicsBodyDescription.PhysicsBodyType;
import io.github.jevaengine.world.scene.model.IActionSceneModel;
import io.github.jevaengine.world.scene.model.IActionSceneModel.NullActionSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.IAnimationSceneModelAnimation;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel.NullAnimationSceneModelAnimation;
import io.github.jevaengine.world.scene.model.IAnimationSceneModelFactory;
import io.github.jevaengine.world.scene.model.ISceneModelFactory.SceneModelConstructionException;
import io.github.jevaengine.world.scene.model.action.DefaultActionModel;
import io.github.jevaengine.world.scene.model.particle.IParticleEmitterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public final class SpaceCharacterFactory implements IRpgCharacterFactory
{
    private final Logger m_logger = LoggerFactory.getLogger(SpaceCharacterFactory.class);
    private final IItemFactory m_itemFactory;
    private final IScriptBuilderFactory m_scriptBuilderFactory;
    private final IAudioClipFactory m_audioClipFactory;
    private final IAnimationSceneModelFactory m_animationSceneModelFactory;
    private final IConfigurationFactory m_configurationFactory;
    private final IDialogueRouteFactory m_dialogueRouteFactory;
    private final IUIStyleFactory m_styleFactory;
    private final IParticleEmitterFactory m_particleEmitterFactory;

    @Inject
    public SpaceCharacterFactory(IItemFactory itemFactory, IScriptBuilderFactory scriptBuilderFactory, IAudioClipFactory audioClipFactory, IAnimationSceneModelFactory animationSceneModelFactory, IConfigurationFactory configurationFactory, IDialogueRouteFactory dialogueRouteFactory, IUIStyleFactory styleFactory, IParticleEmitterFactory particleEmitterFactory)
    {
        m_itemFactory = itemFactory;
        m_scriptBuilderFactory = scriptBuilderFactory;
        m_audioClipFactory = audioClipFactory;
        m_animationSceneModelFactory = animationSceneModelFactory;
        m_configurationFactory = configurationFactory;
        m_dialogueRouteFactory = dialogueRouteFactory;
        m_styleFactory = styleFactory;
        m_particleEmitterFactory = particleEmitterFactory;
    }

    private List<IItem> createItems(URI name, String ... itemNames)
    {
        List<IItem> items = new ArrayList<>();
        for(String itemName : itemNames)
        {
            try
            {
                items.add(m_itemFactory.create(name.resolve(new URI(itemName))));
            } catch (ItemContructionException | URISyntaxException e)
            {
                m_logger.error("Error constructing character item, assuming no such item is in possession of character.", e);
            }
        }

        return items;
    }

    private Map<SpaceCharacterAnimation, IAnimationSceneModelAnimation> createAnimations(IAnimationSceneModel model)
    {
        Map<SpaceCharacterAnimation, IAnimationSceneModelAnimation> result = new HashMap<>();

        for(SpaceCharacterAnimation a : SpaceCharacterAnimation.values())
        {
            IAnimationSceneModelAnimation animation = new NullAnimationSceneModelAnimation();

            if(model.hasAnimation(a.getName()))
                animation = model.getAnimation(a.getName());
            else
                m_logger.warn("Character is missing usr animation " + a.getName() + ". Using null animation as replacement.");

            result.put(a, animation);
        }

        return Collections.unmodifiableMap(result);
    }

    private DefaultActionModel createActionModel(IAnimationSceneModel animationModel, IImmutableAttributeSet attributes, IImmutableLoadout loadout, IImmutableItemStore itemStore)
    {
        Map<SpaceCharacterAnimation, IAnimationSceneModelAnimation> animations = createAnimations(animationModel);
        DefaultActionModel actionModel = new DefaultActionModel(animationModel);

        for(SpaceCharacterAction a : SpaceCharacterAction.values())
            actionModel.addAction(a.build(animations, attributes, loadout, itemStore));

        return actionModel;
    }

    private IDialogueRoute createDialogue(URI name, String dialogue)
    {
        //Initialize dialogue
        IDialogueRoute dialogueRoute = new NullDialogueRoute();
        try
        {
            if(dialogue != null)
                dialogueRoute = m_dialogueRouteFactory.create(name.resolve(new URI(dialogue)));
        } catch (URISyntaxException | DialogueRouteConstructionException e)
        {
            m_logger.error("Unable to construct character dialogue, assuming no dialogue", e);
        }

        return dialogueRoute;
    }

    private DefaultInventory createInventory(URI name, String[] items, int inventorySize)
    {
        DefaultInventory inventory = new DefaultInventory(inventorySize);

        for(IItem item : createItems(name, items))
        {
            if(!inventory.addItem(item))
                m_logger.error("Error equiping character inventory with item " + item.getName() + " as doing so exceeds the characters inventory size.");
        }

        return inventory;
    }

    private DefaultLoadout createLoadout(URI name, IWieldTarget[] wieldTargets, Map<String, String> items)
    {
        //Initialize loadout/inventory
        DefaultLoadout loadout = new DefaultLoadout();

        for(IWieldTarget w : wieldTargets)
            loadout.addWieldTarget(w);

        for(Map.Entry<String, String> i : items.entrySet())
        {
            IWieldTarget target = null;

            for(IWieldTarget t : wieldTargets)
                if(t.getName().equals(i.getKey()))
                    target = t;

            if(target == null || !loadout.getSlot(target).isEmpty()) {
                m_logger.error("Could not add loadout item " + i.getValue() + " as " + i.getKey() + " is already in use or not a valid wield target.");
            } else {
                try {
                    IItem item = m_itemFactory.create(new URI(i.getValue()));
                    loadout.getSlot(target).setItem(item);
                } catch (ItemContructionException | URISyntaxException e) {
                    m_logger.error("Could not add loadout item " + i.getValue() + " as it could not be constructed", e);
                }
            }
        }

        return loadout;
    }

    private IScriptBuilder createBehavior(URI name, String behavior)
    {
        //Initialize character behaviour
        IScriptBuilder scriptBuilder = new NullScriptBuilder();

        try
        {
            if(behavior != null)
                scriptBuilder = m_scriptBuilderFactory.create(name.resolve(new URI(behavior)));
            else
                scriptBuilder = m_scriptBuilderFactory.create();

        } catch (ScriptBuilderConstructionException | URISyntaxException e)
        {
            m_logger.error("Error instantiating character behaviour. Assuming null behaviour.", e);
        }

        return scriptBuilder;
    }

    private IActionSceneModel createModel(URI name, String model, @Nullable String statusStyle, String blood, AttributeSet attributes, DefaultLoadout loadout, DefaultInventory inventory)
    {
        //Initialize model...
        IActionSceneModel actionModel = new NullActionSceneModel();

        try
        {
            IAnimationSceneModel spriteModel = new EquipmentCompositedAnimationSceneModel(m_animationSceneModelFactory.create(name.resolve(new URI(model))), loadout);

            actionModel = createActionModel(spriteModel, attributes, loadout, inventory);
        } catch(SceneModelConstructionException | URISyntaxException e)
        {
            m_logger.error("Unable to construct character model, using null model instead.", e);
        }

        return actionModel;
    }

    protected IAllegianceResolverFactory createAllegienceResolverFactory() {
        return new IAllegianceResolverFactory.NullAllegianceResolverFactory();
    }

    protected IStatusResolverFactory createStatusResolverFactory(URI configContext, UsrCharacterDeclaration characterDecl) throws URISyntaxException, UIStyleConstructionException {
        return new SpaceCharacterStatusResolverFactory();
    }

    protected IMovementResolverFactory createMovementResolverFactory() {
        return new SpaceMovementResolverFactory();
    }

    protected IDialogueResolverFactory createDialogueResolverFactory(IDialogueRoute route) {
        return new DefaultDialogueResolverFactory(route);
    }

    protected IVisionResolverFactory createVisionResolverFactory() {
        return new IVisionResolverFactory.NullVisionResolverFactory();
    }

    @Override
    public IRpgCharacter create(String instanceName, @Nullable URI config, IImmutableVariable auxConfig) throws CharacterCreationException
    {
        URI name = config == null ? URI.create("") : config;

        try
        {
            IImmutableVariable configuration = config == null ? auxConfig : new ImmutableVariableOverlay(auxConfig, m_configurationFactory.create(config));
            UsrCharacterDeclaration characterDecl = configuration.getValue(UsrCharacterDeclaration.class);

            IScriptBuilder behavior = createBehavior(name, characterDecl.behavior);
            DefaultInventory inventory = createInventory(name, characterDecl.items, characterDecl.inventorySize);
            DefaultLoadout loadout = createLoadout(name, characterDecl.wieldTargets, characterDecl.loadout);

            IDialogueRoute dialogueRoute = createDialogue(name, characterDecl.dialogue);

            AttributeSet attributes = new AttributeSet(characterDecl.attributes);
            IActionSceneModel model = createModel(name, characterDecl.model, characterDecl.statusStyle, characterDecl.blood, attributes, loadout, inventory);

            PhysicsBodyDescription physicsBody = new PhysicsBodyDescription(PhysicsBodyType.Dynamic,
                    model.getBodyShape(),
                    1.0F, true, false, 0.0F, DefaultRpgCharacter.class);

            SpaceCharacter.ICorpseProducer corpseProducer = () -> {
                try {
                    IItem item = m_itemFactory.create(new URI(characterDecl.produce));
                    return new ItemDrop(item);
                } catch (ItemContructionException | URISyntaxException ex) {
                    m_logger.error("Unable to produce character corpse.", ex);
                    return null;
                }
            };

            return new SpaceCharacter(behavior,
                    m_dialogueRouteFactory,
                    attributes,
                    createStatusResolverFactory(name, characterDecl),
                    createDialogueResolverFactory(dialogueRoute),
                    createMovementResolverFactory(),
                    createVisionResolverFactory(),
                    createAllegienceResolverFactory(),
                    loadout,
                    inventory,
                    model,
                    physicsBody,
                    characterDecl.baseDamageMapping,
                    characterDecl.damageMultiplierMapping,
                    corpseProducer,
                    instanceName);

        } catch (UIStyleConstructionException | URISyntaxException | ValueSerializationException | ConfigurationConstructionException e)
        {
            throw new CharacterCreationException(instanceName, e);
        }
    }

    @Override
    public IRpgCharacter create(String instanceName, IImmutableVariable auxConfig) throws CharacterCreationException
    {
        return create(instanceName, null, auxConfig);
    }

    public static final class UsrCharacterDeclaration implements ISerializable
    {
        public AttributeSet attributes = new AttributeSet();
        public IWieldTarget[] wieldTargets;
        public Map<String, String> loadout;
        public String[] items;
        public int inventorySize;

        @Nullable
        public String dialogue;

        @Nullable
        public String behavior;

        public String model;

        @Nullable
        public String statusStyle;

        public String blood;

        public String produce;

        @Nullable
        public SpaceCharacterWieldTarget combatWieldTarget;

        public Map<DamageCategory, Integer> baseDamageMapping = new HashMap<>();
        public Map<DamageSeverity, Integer> damageMultiplierMapping = new HashMap<>();

        @Override
        public void serialize(IVariable target) throws ValueSerializationException
        {
            IVariable attributesVar = target.addChild("attributes");

            for(Map.Entry<IAttributeIdentifier, IImmutableAttribute> attribute : attributes.getSet())
                attributesVar.addChild(attribute.getKey().getName()).setValue(attribute.getValue().get());

            target.addChild("wieldTargets").setValue(wieldTargets);
            target.addChild("inventorySize").setValue(inventorySize);

            target.addChild("blood").setValue(blood);
            target.addChild("items").setValue(items);
            target.addChild("loadout").setValue(loadout);

            if(behavior != null)
                target.addChild("behavior").setValue(behavior);

            if(dialogue != null)
                target.addChild("dialogue").setValue(dialogue);

            target.addChild("model").setValue(model);

            if(statusStyle != null)
                target.addChild("statusStyle").setValue(statusStyle);

            if(combatWieldTarget != null)
                target.addChild("combatWieldTarget").setValue(combatWieldTarget.ordinal());
        }

        @Override
        public void deserialize(IImmutableVariable source) throws ValueSerializationException
        {
            try
            {
                if(source.childExists("dialogue"))
                    dialogue = source.getChild("dialogue").getValue(String.class);

                if(source.childExists("statusStyle"))
                    statusStyle = source.getChild("statusStyle").getValue(String.class);

                blood = source.getChild("blood").getValue(String.class);

                IImmutableVariable attributesVar = source.getChild("attributes");
                for(String attributeName : attributesVar.getChildren())
                {
                    for(IAttributeIdentifier attributeIdentifiers[] : new IAttributeIdentifier[][] {SpaceCharacterAttribute.values()})
                    {
                        for(IAttributeIdentifier attributeIdentifier : attributeIdentifiers)
                        {
                            if(attributeName.equals(attributeIdentifier.getName()))
                                attributes.get(attributeIdentifier).set(attributesVar.getChild(attributeName).getValue(Double.class).floatValue());
                        }
                    }
                }

                String[] wieldTargetNames = source.getChild("wieldTargets").getValues(String[].class);
                wieldTargets = new SpaceCharacterWieldTarget[wieldTargetNames.length];

                for(int i = 0; i < wieldTargetNames.length; i++)
                {
                    IWieldTarget wieldTarget = null;
                    for(IWieldTarget w : SpaceCharacterWieldTarget.values())
                    {
                        if(w.getName().equals(wieldTargetNames[i]))
                            wieldTarget = w;
                    }

                    if(wieldTarget == null)
                        throw new ValueSerializationException(new NoSuchElementException("Wield target name is not a valid usr wield target."));

                    wieldTargets[i] = wieldTarget;
                }

                inventorySize = source.getChild("inventorySize").getValue(Integer.class);

                items = source.getChild("items").getValues(String[].class);

                loadout = new HashMap<>();

                IImmutableVariable loadoutVar = source.getChild("loadout");
                for(String s : loadoutVar.getChildren())
                    loadout.put(s, loadoutVar.getChild(s).getValue(String.class));

                if(source.childExists("behavior"))
                    behavior = source.getChild("behavior").getValue(String.class);

                model = source.getChild("model").getValue(String.class);

                if(source.childExists("combatWieldTarget"))
                {
                    int ordinal = source.getChild("combatWieldTarget").getValue(Integer.class);
                    if(ordinal < 0 || ordinal >= SpaceCharacterWieldTarget.values().length)
                        throw new ValueSerializationException(new IndexOutOfBoundsException("combatWieldTarget is out of the ordinal bounds of UsrWieldTarget."));
                }


                if(source.childExists("damage")) {
                    IImmutableVariable damage = source.getChild("damage");

                    produce = damage.getChild("produce").getValue(String.class);
                    if(damage.childExists("base")) {
                        IImmutableVariable baseDamage = damage.getChild("base");
                        for(DamageCategory c : DamageCategory.values()) {
                            if(baseDamage.childExists(c.toString())) {
                                int base = baseDamage.getChild(c.toString()).getValue(Integer.class);
                                baseDamageMapping.put(c, base);
                            }
                        }
                    }

                    if(damage.childExists("multiplier")) {
                        IImmutableVariable multiplier = damage.getChild("multiplier");
                        for(DamageSeverity s : DamageSeverity.values()) {
                            if(multiplier.childExists(s.toString())) {
                                int m = multiplier.getChild(s.toString()).getValue(Integer.class);
                                damageMultiplierMapping.put(s, m);
                            }
                        }
                    }
                }
            } catch(ValueSerializationException | NoSuchChildVariableException e)
            {
                throw new ValueSerializationException(e);
            }
        }
    }
}
