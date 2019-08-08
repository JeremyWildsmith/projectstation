/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.ui.starmap;

import com.jevaengine.spacestation.ui.ToggleIcon;
import io.github.jevaengine.joystick.InputKeyEvent;
import io.github.jevaengine.joystick.InputMouseEvent;
import io.github.jevaengine.math.*;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.ui.*;
import io.github.jevaengine.ui.Button;
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
import io.github.jevaengine.world.physics.PhysicsBodyDescription;
import io.github.jevaengine.world.physics.PhysicsBodyShape;
import io.github.jevaengine.world.scene.ISceneBuffer;
import io.github.jevaengine.world.scene.camera.FollowCamera;
import io.github.jevaengine.world.scene.camera.ICamera;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.scene.model.NullSceneModel;
import io.github.jevaengine.world.steering.DirectionSteeringBehavior;
import io.github.jevaengine.world.steering.ISteeringBehavior;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public class NavigateBehaviorInjector extends WindowBehaviourInjector {
	private static final float DELTA_TOLERANCE = 1.0f;

	private final PlayerMovementDirector m_playerMovementDirector = new PlayerMovementDirector();
	private final IMovementResolver m_targetMovementResolver;
	private final IEntity m_player;
	private NavigateTargetEntity m_targetEntity = null;

	public NavigateBehaviorInjector(IEntity player, IMovementResolver targetMovementResolver) {
		m_targetMovementResolver = targetMovementResolver;
		m_player = player;
	}

	@Override
	protected void doInject() throws NoSuchControlException {
		final WorldView demoWorldView = getControl(WorldView.class, "worldView");
		final Timer timer = new Timer();
		final ToggleIcon fullscreen = getControl(ToggleIcon.class, "fullscreen");
		addControl(timer);

		fullscreen.getObservers().add(new ToggleIcon.IToggleIconObserver() {
			private Vector2D originalLoc = new Vector2D();
			private boolean isFullscreen = false;
			private Rect2D originalBounds = new Rect2D();
			private Rect2D originalWorldBounds = new Rect2D();

			@Override
			public void toggled() {
				int offsetX = getWindow().getBounds().width - fullscreen.getLocation().x;

				if (!isFullscreen) {
					isFullscreen = true;
					originalLoc = getWindow().getLocation();
					getWindow().setLocation(new Vector2D(0, 40));
					originalBounds = getWindow().getBounds();
					originalWorldBounds = demoWorldView.getBounds();
					Vector2D resolution = getWindow().getManager().getResolution();

					getWindow().setBounds(new Rect2D(resolution.x, resolution.y - 40));

					demoWorldView.setBounds(new Rect2D(resolution.x - 10 - demoWorldView.getLocation().x,
							resolution.y - 50 - demoWorldView.getLocation().y));
				} else {
					isFullscreen = false;
					demoWorldView.setBounds(originalWorldBounds);
					getWindow().setLocation(originalLoc);
					getWindow().setBounds(originalBounds);
				}

				fullscreen.setLocation(new Vector2D(getWindow().getBounds().width - offsetX, fullscreen.getLocation().y));
			}
		});

		timer.getObservers().add(new Timer.ITimerObserver() {
			@Override
			public void update(int deltaTime) {
				m_playerMovementDirector.update(deltaTime);

				if (m_targetEntity == null)
					return;

				Vector2F delta = m_targetEntity.getBody().getLocation().getXy();
				delta = delta.difference(m_player.getBody().getLocation().getXy());

				if (delta.getLength() <= DELTA_TOLERANCE) {
					delta = new Vector2F();
					m_targetEntity.setActive(false);
				} else
					m_targetEntity.setActive(true);

				m_playerMovementDirector.setMovementDelta(delta);
			}
		});

		demoWorldView.getObservers().add(new WorldView.IWorldViewInputObserver() {
			@Override
			public void mouseEvent(InputMouseEvent event) {
				if(event.type != InputMouseEvent.MouseEventType.MouseClicked || event.mouseButton != InputMouseEvent.MouseButton.Left)
					return;

				Vector2F newDest = demoWorldView.translateScreenToWorld(new Vector2F(event.location).difference(demoWorldView.getLocation()));

				if (m_targetEntity == null && m_player.getWorld() != null) {
					m_targetEntity = new NavigateTargetEntity(m_player);
					m_player.getWorld().addEntity(m_targetEntity);
				}

				if(m_targetEntity != null)
					m_targetEntity.getBody().setLocation(new Vector3F(newDest.x, newDest.y, -1f));
			}

			@Override
			public void keyEvent(InputKeyEvent event) {
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
				m_targetMovementResolver.queue(this);
			}
		}
	}

	static class NavigateTargetEntity implements IEntity {

		private static int NAME_COUNT = 0;

		private final IEntity m_source;
		private World m_world;
		private String m_name;
		private IPhysicsBody m_body;
		private Observers m_observers = new Observers();
		private IImmutableSceneModel m_model = new NavModel();
		private boolean m_active = false;

		public NavigateTargetEntity(IEntity source) {
			m_source = source;
			m_name = this.getClass().getName() + (NAME_COUNT++);
		}

		public void setActive(boolean active) {
			m_active = active;
		}

		@Override
		public World getWorld() {
			return m_world;
		}

		@Override
		public void associate(World world) {
			m_world = world;
			m_body = new NonparticipantPhysicsBody();
		}

		@Override
		public void disassociate() {
			m_world = null;
			m_body = null;
		}

		@Override
		public String getInstanceName() {
			return m_name;
		}

		@Override
		public Map<String, Integer> getFlags() {
			return new HashMap<>();
		}

		@Override
		public boolean isStatic() {
			return true;
		}

		@Override
		public IImmutableSceneModel getModel() {
			if(!m_active)
				return new NullSceneModel();

			return m_model;
		}

		@Override
		public IPhysicsBody getBody() {
			return m_body;
		}

		@Override
		public IEntityTaskModel getTaskModel() {
			return new NullEntityTaskModel();
		}

		@Override
		public IObserverRegistry getObservers() {
			return m_observers;
		}

		@Override
		public EntityBridge getBridge() {
			return new EntityBridge(this);
		}

		@Override
		public void update(int delta) {

		}

		@Override
		public void dispose() {

		}

		private final Rect3F calculateRegion() {
			Vector3F source_loc = m_source.getBody().getLocation();
			Vector3F nav_loc = NavigateTargetEntity.this.m_body.getLocation();

			float minX = Math.min(source_loc.x, nav_loc.x);
			float minY = Math.min(source_loc.y, nav_loc.y);
			float minZ = Math.min(source_loc.z, nav_loc.z);

			float width = Math.max(source_loc.x, nav_loc.x) - minX;
			float height = Math.max(source_loc.y, nav_loc.y) - minY;
			float depth = Math.max(source_loc.z, nav_loc.z) - minZ;

			return new Rect3F(new Vector3F(minX, minY, minZ), width, height, depth);
		}

		private class NavModel implements IImmutableSceneModel {

			@Override
			public ISceneModel clone() throws SceneModelNotCloneableException {
				throw new SceneModelNotCloneableException();
			}

			@Override
			public Collection<ISceneModelComponent> getComponents(Matrix3X3 projection) {
				ISceneModelComponent c = new ISceneModelComponent() {
					@Override
					public String getName() {
						return "c";
					}

					@Override
					public boolean testPick(int x, int y, float scale) {
						return false;
					}

					@Override
					public Rect3F getBounds() {
						Rect3F region = calculateRegion();
						region.x = region.y = region.z = 0;
						return region;
					}

					@Override
					public Vector3F getOrigin() {
						Rect3F region = calculateRegion();
						Vector3F loc = new Vector3F(region.x, region.y, region.z);
						loc = loc.difference(NavigateTargetEntity.this.m_body.getLocation());
						return loc;
					}

					@Override
					public void render(Graphics2D g, int x, int y, float scale) {
						g.setColor(Color.white);

						Vector2D origin = projection.dot(getOrigin()).getXy().multiply(scale).round();
						Vector2D navLOc = new Vector2D(x - origin.x, y - origin.y);

						Vector2D start = projection.dot(m_source.getBody().getLocation().difference(NavigateTargetEntity.this.getBody().getLocation())).getXy().multiply(scale).round();
						start = start.add(navLOc);

                        g.setStroke(new BasicStroke());

                        /*
                        int length = (int)start.difference(navLOc).getLength();
                        for(int i = 0; i < length; i+= 40 * scale) {
                            Vector2F dir = new Vector2F(navLOc.difference(start)).normalize();
                            Vector2D loc = start.add(dir.multiply(i).round());
                            //g.fillOval(loc.x - 2, loc.y - 2, 4, 4);
                            int offset = (int)(4 * scale);
                            g.drawPolygon(new int[]{loc.x-offset, loc.x, loc.x+offset, loc.x}, new int[]{loc.y, loc.y + offset, loc.y, loc.y - offset}, 4);
                        }*/

                        g.drawLine(start.x, start.y, navLOc.x, navLOc.y);
					}
				};

				ArrayList<ISceneModelComponent> comps = new ArrayList<>();
				comps.add(c);

				return comps;
			}

			@Override
			public Rect3F getAABB() {
				Rect3F region = calculateRegion();
				region.x = region.y = region.z = 0;
				return region;
			}

			@Override
			public Direction getDirection() {
				return Direction.Zero;
			}

			@Override
			public PhysicsBodyShape getBodyShape() {
				return new PhysicsBodyShape(PhysicsBodyShape.PhysicsBodyShapeType.Box, getAABB());
			}
		}
	}
}
