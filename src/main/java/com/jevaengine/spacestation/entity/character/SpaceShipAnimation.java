package com.jevaengine.spacestation.entity.character;


public enum SpaceShipAnimation
{
    Idle("idle");
    private final String m_name;

    private SpaceShipAnimation(String name)
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }
}