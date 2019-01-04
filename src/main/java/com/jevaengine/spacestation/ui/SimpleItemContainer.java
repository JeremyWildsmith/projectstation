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
import io.github.jevaengine.rpg.entity.character.ILoadout.NullLoadoutSlot;
import io.github.jevaengine.rpg.item.IImmutableItemSlot;
import io.github.jevaengine.ui.Control;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;

import java.awt.*;

/**
 *
 * @author Jeremy
 */
public final class SimpleItemContainer extends Control {
	
	public static final String COMPONENT_NAME = "simpleItemContainer";

	private final IImmutableGraphic m_background;
	private final Observers m_observers = new Observers();
	
	private IImmutableItemSlot m_loadoutSlot = new NullLoadoutSlot();
	
	public SimpleItemContainer(String instanceName, IImmutableGraphic background) {
		super(COMPONENT_NAME, instanceName);
		
		m_background = background;
	}
	
	public IObserverRegistry getObservers() {
		return m_observers;
	}
	
	public void setSlot(IImmutableItemSlot slot) {
		m_loadoutSlot = slot;
	}

	@Override
	public boolean onMouseEvent(InputMouseEvent mouseEvent) {
		if(mouseEvent.type != InputMouseEvent.MouseEventType.MouseClicked)
			return false;

		if(mouseEvent.mouseButton == InputMouseEvent.MouseButton.Left)
			m_observers.raise(ISimpleItemContainerObserver.class).selected();
		else
			m_observers.raise(ISimpleItemContainerObserver.class).alternateSelected();
		
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
			IImmutableGraphic itemGraphic = m_loadoutSlot.getItem().getIcon();
			
			if(itemGraphic == null)
				return;
			
			int xOff = x + m_background.getBounds().width / 2;
			int yOff = y + m_background.getBounds().width / 2;
			
			xOff += -itemGraphic.getBounds().width / 2;
			yOff += -itemGraphic.getBounds().width / 2;
			
			itemGraphic.render(g, xOff, yOff, scale);
		}
	}
	
	public interface ISimpleItemContainerObserver {
		void selected();
		void alternateSelected();
	}
}
