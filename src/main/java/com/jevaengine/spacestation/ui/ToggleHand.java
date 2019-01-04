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

import java.awt.*;

/**
 *
 * @author Jeremy
 */
public final class ToggleHand extends Control {
	public static final String COMPONENT_NAME = "toggleHand";
	
	private final IImmutableGraphic m_right;
	private final IImmutableGraphic m_left;
	private final IImmutableGraphic m_active;
	
	private boolean m_isLeftHand;
	
	public ToggleHand(String instanceName, IImmutableGraphic left, IImmutableGraphic right, IImmutableGraphic active) {
		super(COMPONENT_NAME, instanceName);
		
		m_left = left;
		m_right = right;
		m_active = active;
		
		m_isLeftHand = false;
	}
	
	public boolean isLeftHand() {
		return m_isLeftHand;
	}

	@Override
	public boolean onMouseEvent(InputMouseEvent mouseEvent) {
		if(mouseEvent.type != InputMouseEvent.MouseEventType.MouseClicked)
			return false;
		
		m_isLeftHand = !m_isLeftHand;
		
		return true;
	}

	@Override
	public boolean onKeyEvent(InputKeyEvent keyEvent) {
		return false;
	}

	@Override
	public Rect2D getBounds() {
		return new Rect2D(m_left.getBounds().width + m_right.getBounds().width,
							m_left.getBounds().height + m_right.getBounds().height);
	}

	@Override
	public void update(int deltaTime) { }

	@Override
	public void render(Graphics2D g, int x, int y, float scale) {
		m_left.render(g, x + m_left.getBounds().width, y, scale);
		m_right.render(g, x, y, scale);
		m_active.render(g, x + (m_isLeftHand ? m_left.getBounds().width : 0), y, scale);
	}
}
