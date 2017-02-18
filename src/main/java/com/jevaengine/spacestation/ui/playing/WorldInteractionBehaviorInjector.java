/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.playing;

import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WorldView;

/**
 *
 * @author Jeremy
 */
public class WorldInteractionBehaviorInjector extends WindowBehaviourInjector {

	private final float m_interactionDistance;
	
	private final IRpgCharacter m_character;

	public WorldInteractionBehaviorInjector(IRpgCharacter character) {
		m_character = character;
		m_interactionDistance = character.getBody().getBoundingCircle().radius * 2.1F;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView demoWorldView = getControl(WorldView.class, "worldView");

		demoWorldView.getObservers().add(new WorldView.IWorldViewInputObserver() {
			@Override
			public void mouseEvent(InputMouseEvent event) {
				if (event.type == InputMouseEvent.MouseEventType.MouseClicked) {
					final Door pickedInteraction = demoWorldView.pick(Door.class, event.location);

					if (pickedInteraction == null) {
						return;
					}
					
					float distance = pickedInteraction.getBody().getLocation().getXy().difference(m_character.getBody().getLocation().getXy()).getLength();
					
					if(distance > m_interactionDistance)
						return;
					
					if (pickedInteraction.isOpen()) {
						pickedInteraction.close();
					} else {
						pickedInteraction.open();
					}
				}
			}

			@Override
			public void keyEvent(InputKeyEvent event) { }
		});
	}
}
