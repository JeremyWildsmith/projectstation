/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui;

import io.github.jevaengine.graphics.IImmutableGraphic;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.ui.Control;
import java.awt.Graphics2D;

/**
 *
 * @author Jeremy
 */
public final class ToggleIcon extends Control {
	public static final String COMPONENT_NAME = "toggleIcon";
	
	private final IImmutableGraphic m_active;
	private final IImmutableGraphic m_inactive;
	
	private boolean m_isActive;
	
	public ToggleIcon(String instanceName, IImmutableGraphic active, IImmutableGraphic inactive) {
		super(COMPONENT_NAME, instanceName);
		
		m_active = active;
		m_inactive = inactive;
		
		m_isActive = false;
	}
	
	public boolean isActive() {
		return m_isActive;
	}

	@Override
	public boolean onMouseEvent(InputMouseEvent mouseEvent) {
		if(mouseEvent.type != InputMouseEvent.MouseEventType.MouseClicked)
			return false;
		
		m_isActive = !m_isActive;
		
		return true;
	}

	@Override
	public boolean onKeyEvent(InputKeyEvent keyEvent) {
		return false;
	}

	@Override
	public Rect2D getBounds() {
		return m_isActive ? m_active.getBounds() : m_inactive.getBounds();
	}

	@Override
	public void update(int deltaTime) { }

	@Override
	public void render(Graphics2D g, int x, int y, float scale) {
		if(m_isActive)
			m_active.render(g, x, y, scale);
		else
			m_inactive.render(g, x, y, scale);
	}
}
