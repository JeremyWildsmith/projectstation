package com.jevaengine.spacestation.entity.character;

import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.IImmutableLoadout;
import io.github.jevaengine.rpg.entity.character.SimpleModelActionBehavior;
import io.github.jevaengine.rpg.item.IImmutableItemStore;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.action.DefaultActionModel;

import java.util.Map;

public enum SpaceCharacterAction
{
    Die("die", new SimpleActionBehaviorBuilder(SpaceCharacterAnimation.Idle, false)),
    Attack("attack", new SimpleActionBehaviorBuilder(SpaceCharacterAnimation.Idle, false)),
    Walk("walk", new SimpleActionBehaviorBuilder(SpaceCharacterAnimation.Idle, true)),
    Idle("idle", new SimpleActionBehaviorBuilder(SpaceCharacterAnimation.Idle, true)),
    Flinch("flinch", new SimpleActionBehaviorBuilder(SpaceCharacterAnimation.Idle, true));

    private final String m_name;
    private final IActionBuilder m_builder;

    SpaceCharacterAction(String name, IActionBuilder builder)
    {
        m_name = name;
        m_builder = builder;

        builder.setMe(this);
    }

    public String getName()
    {
        return m_name;
    }

    DefaultActionModel.IDefaultActionModelBehavior build(Map<SpaceCharacterAnimation, IAnimationSceneModel.IAnimationSceneModelAnimation> animations, IImmutableAttributeSet attributes, IImmutableLoadout loadout, IImmutableItemStore itemStore)
    {
        return m_builder.build(animations, attributes, loadout, itemStore);
    }

    private interface IActionBuilder
    {
        void setMe(SpaceCharacterAction action);
        DefaultActionModel.IDefaultActionModelBehavior build(Map<SpaceCharacterAnimation, IAnimationSceneModel.IAnimationSceneModelAnimation> animations, IImmutableAttributeSet attributes, IImmutableLoadout loadout, IImmutableItemStore itemStore);
    }

    public static class SimpleActionBehaviorBuilder implements IActionBuilder
    {
        private SpaceCharacterAction m_action;
        private final SpaceCharacterAnimation m_animation;
        private final boolean m_isPassive;

        public SimpleActionBehaviorBuilder(@Nullable SpaceCharacterAnimation animation, boolean isPassive)
        {
            m_animation = animation;
            m_isPassive = isPassive;
        }

        @Override
        public void setMe(SpaceCharacterAction action)
        {
            m_action = action;
        }

        @Override
        public DefaultActionModel.IDefaultActionModelBehavior build(
                Map<SpaceCharacterAnimation, IAnimationSceneModel.IAnimationSceneModelAnimation> animations,
                IImmutableAttributeSet attributes,
                IImmutableLoadout loadout, IImmutableItemStore itemStore)
        {
            return new SimpleModelActionBehavior(m_action.getName(), m_animation == null ? new IAnimationSceneModel.NullAnimationSceneModelAnimation() : animations.get(m_animation), m_isPassive);
        }
    }
}
