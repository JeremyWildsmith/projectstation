/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.playing;

import com.jevaengine.spacestation.entity.IInteractableEntity;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.joystick.InputMouseEvent.MouseButton;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.rpg.item.IItem;
import io.github.jevaengine.rpg.item.IItemSlot;
import io.github.jevaengine.rpg.item.usr.UsrWieldTarget;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.search.RadialSearchFilter;

import java.awt.event.KeyEvent;

/**
 *
 * @author Jeremy
 */
public class WorldInteractionBehaviorInjector extends WindowBehaviourInjector {

	private final float m_interactionDistance;

	private final IRpgCharacter m_character;
	private final IInteractionHandler[] m_handlers;

	public <T extends IEntity> WorldInteractionBehaviorInjector(IRpgCharacter character, IInteractionHandler... interactionHandlers) {
		m_character = character;
		m_interactionDistance = character.getBody().getBoundingCircle().radius * 3.0F;
		m_handlers = interactionHandlers;
	}

	public IInteractionHandler getHandler(Class<?> clazz) {
		for (IInteractionHandler h : m_handlers) {
			if (clazz.isAssignableFrom(h.getHandleSubject())) {
				return h;
			}
		}

		return null;
	}

	private boolean isInReach(IEntity entity) {
		float distance = entity.getBody().getLocation().getXy().difference(m_character.getBody().getLocation().getXy()).getLength();

		if (distance > m_interactionDistance) {
			return false;
		}
		
		return true;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView demoWorldView = getControl(WorldView.class, "worldView");
		final Timer timer = new Timer();

		addControl(timer);

		timer.getObservers().add(new Timer.ITimerObserver() {
			@Override
			public void update(int deltaTime) {
				for (IInteractionHandler h : m_handlers) {
					IEntity interaction = h.getActiveInteraction();
					
					if(interaction != null && !isInReach(interaction))
						h.outOfReach();
				}
			}
		});

		demoWorldView.getObservers().add(new WorldView.IWorldViewInputObserver() {
			@Override
			public void mouseEvent(InputMouseEvent event) {
				if (event.type == InputMouseEvent.MouseEventType.MouseClicked) {
					final IEntity pickedInteraction = demoWorldView.pick(IEntity.class, event.location);

					if (pickedInteraction == null) {
						return;
					}

					if(!isInReach(pickedInteraction))
						return;

					IInteractionHandler handler = getHandler(pickedInteraction.getClass());

					if (handler != null) {
						handler.handle(pickedInteraction, event.mouseButton == MouseButton.Right, m_interactionDistance);
					} else if (pickedInteraction instanceof IInteractableEntity) {
						((IInteractableEntity) pickedInteraction).interactedWith(m_character);
					}
				}
			}

			@Override
			public void keyEvent(InputKeyEvent event) {
				if (event.type != InputKeyEvent.KeyEventType.KeyTyped) {
					return;
				}

				if(event.keyCode == KeyEvent.VK_E) {
					Direction playerDirection = m_character.getModel().getDirection();
					Vector2F playerPos = m_character.getBody().getLocation().getXy();
					IInteractableEntity[] interactable = m_character.getWorld().getEntities().search(IInteractableEntity.class,
							new RadialSearchFilter<IInteractableEntity>(playerPos, m_interactionDistance));

					for (IInteractableEntity e : interactable) {
						if (Direction.fromVector(e.getBody().getLocation().getXy().difference(playerPos)) == playerDirection) {

							IInteractionHandler handler = getHandler(e.getClass());
							if(handler != null)
								handler.handle(e, false, m_interactionDistance);
							e.interactedWith(m_character);
						}
					}
				} else if(event.keyCode == KeyEvent.VK_R) {
					IItemSlot slot = m_character.getLoadout().getSlot(UsrWieldTarget.LeftHand);

					if(slot.isEmpty())
						return;

					IItem item = slot.getItem();

					IItem.ItemUseAbilityTestResults result = item.getFunction().testUseAbility(m_character, null, item.getAttributes());

					if(result.isUseable()) {
						item.getFunction().use(m_character, m_character, item.getAttributes(), item);
					}
				}

			}

		});
	}

	public interface IInteractionHandler {

		Class<?> getHandleSubject();

		void handle(IEntity subject, boolean isSecondary, float interactionReach);

		IEntity getActiveInteraction();

		void outOfReach();
	}
}
