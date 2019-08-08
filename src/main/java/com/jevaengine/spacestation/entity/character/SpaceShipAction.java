package com.jevaengine.spacestation.entity.character;

import io.github.jevaengine.rpg.IImmutableAttributeSet;
import io.github.jevaengine.rpg.entity.character.IImmutableLoadout;
import io.github.jevaengine.rpg.entity.character.SimpleModelActionBehavior;
import io.github.jevaengine.rpg.item.IImmutableItemStore;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.world.scene.model.IAnimationSceneModel;
import io.github.jevaengine.world.scene.model.action.DefaultActionModel;

import java.util.Map;

public enum SpaceShipAction
{
    Fly("fly", new SimpleActionBehaviorBuilder(SpaceShipAnimation.Idle, false)),
    Idle("idle", new SimpleActionBehaviorBuilder(SpaceShipAnimation.Idle, true));

    private final String m_name;
    private final IActionBuilder m_builder;

    SpaceShipAction(String name, IActionBuilder builder)
    {
        m_name = name;
        m_builder = builder;

        builder.setMe(this);
    }

    public String getName()
    {
        return m_name;
    }

    DefaultActionModel.IDefaultActionModelBehavior build(Map<SpaceShipAnimation, IAnimationSceneModel.IAnimationSceneModelAnimation> animations, IImmutableAttributeSet attributes, IImmutableLoadout loadout)
    {
        return m_builder.build(animations, attributes, loadout);
    }

    private interface IActionBuilder
    {
        void setMe(SpaceShipAction action);
        DefaultActionModel.IDefaultActionModelBehavior build(Map<SpaceShipAnimation, IAnimationSceneModel.IAnimationSceneModelAnimation> animations, IImmutableAttributeSet attributes, IImmutableLoadout loadout);
    }

    public static class SimpleActionBehaviorBuilder implements IActionBuilder
    {
        private SpaceShipAction m_action;
        private final SpaceShipAnimation m_animation;
        private final boolean m_isPassive;

        public SimpleActionBehaviorBuilder(@Nullable SpaceShipAnimation animation, boolean isPassive)
        {
            m_animation = animation;
            m_isPassive = isPassive;
        }

        @Override
        public void setMe(SpaceShipAction action)
        {
            m_action = action;
        }

        @Override
        public DefaultActionModel.IDefaultActionModelBehavior build(
                Map<SpaceShipAnimation, IAnimationSceneModel.IAnimationSceneModelAnimation> animations,
                IImmutableAttributeSet attributes,
                IImmutableLoadout loadout)
        {
            return new SimpleModelActionBehavior(m_action.getName(), m_animation == null ? new IAnimationSceneModel.NullAnimationSceneModelAnimation() : animations.get(m_animation), m_isPassive);
        }
    }
}
