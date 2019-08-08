/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.starmap;

import com.jevaengine.spacestation.entity.character.SpaceShip;
import com.jevaengine.spacestation.ui.ToggleIcon;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.*;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.PhysicsBodyShape;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.scene.model.NullSceneModel;
import io.github.jevaengine.world.steering.DirectionSteeringBehavior;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public class GunBehaviorInjector extends WindowBehaviourInjector {

	private final SpaceShip m_player;

	public GunBehaviorInjector(SpaceShip player) {
		m_player = player;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView demoWorldView = getControl(WorldView.class, "worldView");

		demoWorldView.getObservers().add(new WorldView.IWorldViewInputObserver() {
			@Override
			public void mouseEvent(InputMouseEvent event) {
				if(event.type != InputMouseEvent.MouseEventType.MouseClicked || event.mouseButton != InputMouseEvent.MouseButton.Right)
					return;

				Vector2F target = demoWorldView.translateScreenToWorld(new Vector2F(event.location).difference(demoWorldView.getLocation()));

				m_player.aim(new Vector3F(target, m_player.getBody().getLocation().z));

			}

			@Override
			public void keyEvent(InputKeyEvent event) {
				if(event.type != InputKeyEvent.KeyEventType.KeyUp || !Character.isDigit(event.keyChar))
					return;

				int group = Character.getNumericValue(event.keyChar) - 1;

				m_player.selectGunGroup(group);
			}
		});
	}
}
