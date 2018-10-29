/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.playing;

import com.jevaengine.spacestation.gas.GasSimulation;
import com.jevaengine.spacestation.gas.GasSimulationEntity;
import com.jevaengine.spacestation.gas.GasSimulationNetwork;
import com.jevaengine.spacestation.gas.GasType;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WorldView;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.steering.DirectionSteeringBehavior;
import io.github.jevaengine.world.steering.ISteeringBehavior;
import java.awt.event.KeyEvent;
import java.util.HashMap;

/**
 *
 * @author Jeremy
 */
public class PlayerMovementBehaviorInjector extends WindowBehaviourInjector {

	private final PlayerMovementDirector m_playerMovementDirector = new PlayerMovementDirector();
	private final IRpgCharacter m_character;

	public PlayerMovementBehaviorInjector(IRpgCharacter character) {
		m_character = character;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView demoWorldView = getControl(WorldView.class, "worldView");
		final Timer timer = new Timer();
		addControl(timer);

		timer.getObservers().add(new Timer.ITimerObserver() {
			@Override
			public void update(int deltaTime) {
				m_playerMovementDirector.update(deltaTime);
			}
		});

		demoWorldView.getObservers().add(new WorldView.IWorldViewInputObserver() {
			@Override
			public void mouseEvent(InputMouseEvent event) {
			}

			@Override
			public void keyEvent(InputKeyEvent event) {
				if (event.type == InputKeyEvent.KeyEventType.KeyTyped) {
					return;
				}

				Vector2F vec = m_playerMovementDirector.getMovementDelta();

				switch (event.keyCode) {
					case KeyEvent.VK_UP:
						vec.y = event.type == InputKeyEvent.KeyEventType.KeyDown ? - 1 : 0;
						break;
					case KeyEvent.VK_DOWN:
						vec.y = event.type == InputKeyEvent.KeyEventType.KeyDown ? 1 : 0;
						break;
					case KeyEvent.VK_RIGHT:
						vec.x = event.type == InputKeyEvent.KeyEventType.KeyDown ? 1 : 0;
						break;
					case KeyEvent.VK_LEFT:
						vec.x = event.type == InputKeyEvent.KeyEventType.KeyDown ? -1 : 0;
						break;
					case KeyEvent.VK_U:
					{
						GasSimulationEntity e = m_character.getWorld().getEntities().getByName(GasSimulationEntity.class, GasSimulationEntity.INSTANCE_NAME);
						HashMap<GasType, Float> gas = new HashMap<>();
						gas.put(GasType.Water, 100f);
						e.produce(GasSimulationNetwork.Environment, m_character.getBody().getLocation().getXy().round(), new GasSimulation.GasMetaData(gas, 0));
					}
				}

				m_playerMovementDirector.setMovementDelta(vec);
			}
		});

		getObservers().add(new Window.IWindowFocusObserver() {
			@Override
			public void onFocusChanged(boolean hasFocus) {
				if (!hasFocus) {
					m_playerMovementDirector.setMovementDelta(new Vector2F());
				}
			}
		});
	}

	private final class PlayerMovementDirector implements IMovementResolver.IMovementDirector {

		private Vector2F m_movementDelta = new Vector2F();
		private boolean m_isQueued = false;

		public void setMovementDelta(Vector2F movementDelta) {
			m_movementDelta = new Vector2F(movementDelta);
		}

		public Vector2F getMovementDelta() {
			return new Vector2F(m_movementDelta);
		}

		@Override
		public ISteeringBehavior getBehavior() {
			if (m_movementDelta.isZero()) {
				return new ISteeringBehavior.NullSteeringBehavior();
			} else {
				return new DirectionSteeringBehavior(Direction.fromVector(m_movementDelta));
			}
		}

		@Override
		public boolean isDone() {
			boolean isDone = m_movementDelta.isZero();

			if (isDone) {
				m_isQueued = false;
			}

			return isDone;
		}

		public void update(int deltaTime) {
			if (!m_movementDelta.isZero() && !m_isQueued) {
				m_isQueued = true;
				m_character.getMovementResolver().queueTop(this);
			}
		}
	}
}
