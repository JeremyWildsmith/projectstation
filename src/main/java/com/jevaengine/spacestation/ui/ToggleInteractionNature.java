/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui;

import com.jevaengine.spacestation.entity.character.InteractionNature;
import static com.jevaengine.spacestation.entity.character.InteractionNature.Disarm;
import static com.jevaengine.spacestation.entity.character.InteractionNature.Grab;
import static com.jevaengine.spacestation.entity.character.InteractionNature.Harm;
import static com.jevaengine.spacestation.entity.character.InteractionNature.Help;
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
public final class ToggleInteractionNature extends Control {
	public static final String COMPONENT_NAME = "toggleInteractionNature";
	
	private final IImmutableGraphic m_harm;
	private final IImmutableGraphic m_help;
	private final IImmutableGraphic m_disarm;
	private final IImmutableGraphic m_grab;
	
	private int m_index;
	
	public ToggleInteractionNature(String instanceName, 
									IImmutableGraphic harm, 
									IImmutableGraphic help, 
									IImmutableGraphic disarm, 
									IImmutableGraphic grab) {
		
		super(COMPONENT_NAME, instanceName);
		
		m_harm = harm;
		m_help = help;
		m_disarm = disarm;
		m_grab = grab;
		
		m_index = InteractionNature.Help.ordinal();
	}
	
	private IImmutableGraphic getGraphic() {
		switch(getInteractionNature()) {
			case Disarm:
				return m_disarm;
			case Help:
				return m_help;
			case Grab:
				return m_grab;
			case Harm:
				return m_harm;
		}
		
		throw new IndexOutOfBoundsException();
	}
		
	public InteractionNature getInteractionNature() {
		return InteractionNature.values()[m_index];
	}

	@Override
	public boolean onMouseEvent(InputMouseEvent mouseEvent) {
		if(mouseEvent.type != InputMouseEvent.MouseEventType.MouseClicked)
			return false;
		
		m_index = (m_index + 1) % InteractionNature.values().length;
		
		return true;
	}

	@Override
	public boolean onKeyEvent(InputKeyEvent keyEvent) {
		return false;
	}

	@Override
	public Rect2D getBounds() {
		return getGraphic().getBounds();
	}

	@Override
	public void update(int deltaTime) { }

	@Override
	public void render(Graphics2D g, int x, int y, float scale) {
		getGraphic().render(g, x, y, scale);
	}
}
