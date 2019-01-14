package com.jevaengine.spacestation.entity.character;


public enum SpaceCharacterAnimation
{
    Idle("idle");
    private final String m_name;

    private SpaceCharacterAnimation(String name)
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }
}