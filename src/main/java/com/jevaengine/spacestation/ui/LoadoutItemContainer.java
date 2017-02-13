/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui;

import io.github.jevaengine.graphics.IImmutableGraphic;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Rect2D;
import io.github.jevaengine.rpg.entity.character.IImmutableLoadout.IImmutableLoadoutSlot;
import io.github.jevaengine.rpg.entity.character.ILoadout.NullLoadoutSlot;
import io.github.jevaengine.ui.Control;
import java.awt.Graphics2D;

/**
 *
 * @author Jeremy
 */
public final class LoadoutItemContainer extends Control {
	
	public static final String COMPONENT_NAME = "loadoutItemContainer";
	
	private final IImmutableGraphic m_background;
	private IImmutableLoadoutSlot m_loadoutSlot = new NullLoadoutSlot();
		
	public LoadoutItemContainer(String instanceName, IImmutableGraphic background) {
		super(COMPONENT_NAME, instanceName);
		
		m_background = background;
	}
	
	public void setSlot(IImmutableLoadoutSlot slot) {
		m_loadoutSlot = slot;
	}

	@Override
	public boolean onMouseEvent(InputMouseEvent mouseEvent) {
		if(mouseEvent.type != InputMouseEvent.MouseEventType.MouseClicked)
			return false;

		//Do something here...
		
		return true;
	}

	@Override
	public boolean onKeyEvent(InputKeyEvent keyEvent) {
		return false;
	}

	@Override
	public Rect2D getBounds() {
		return m_background.getBounds();
	}

	@Override
	public void update(int deltaTime) { }

	@Override
	public void render(Graphics2D g, int x, int y, float scale) {
		m_background.render(g, x, y, scale);
		
		if(!m_loadoutSlot.isEmpty()) {
			int xOff = x + m_background.getBounds().width / 2;
			int yOff = y + m_background.getBounds().width / 2;
			
			IRenderable icon = m_loadoutSlot.getItem().getIcon();
			
			if(icon != null)
				icon.render(g, xOff, yOff, scale);
		}
	}
	
}
