/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.jevaengine.spacestation.entity.character;

import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.rpg.AttributeSet;
import io.github.jevaengine.rpg.IImmutableAttributeSet.IImmutableAttribute;
import io.github.jevaengine.rpg.entity.character.IMovementResolver;
import io.github.jevaengine.rpg.entity.character.IMovementResolverFactory;
import io.github.jevaengine.rpg.entity.character.IRpgCharacter;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.entity.IEntity.IEntityBodyObserver;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.scene.model.IActionSceneModel;
import io.github.jevaengine.world.scene.model.IActionSceneModel.IActionSceneModelAction;
import io.github.jevaengine.world.scene.model.IActionSceneModel.NullActionSceneModelAction;
import io.github.jevaengine.world.scene.model.ISceneModel;
import io.github.jevaengine.world.steering.ISteeringBehavior;
import io.github.jevaengine.world.steering.VelocityLimitSteeringDriver;
import io.github.jevaengine.world.steering.VelocityLimitSteeringDriverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class SpaceMovementResolverFactory implements IMovementResolverFactory
{
	private final Logger m_logger = LoggerFactory.getLogger(SpaceMovementResolverFactory.class);
	
	@Override
	public IMovementResolver create(IRpgCharacter host, AttributeSet attributes, IActionSceneModel model)
	{
		IActionSceneModelAction walkAction = new NullActionSceneModelAction();
		IActionSceneModelAction idleAction = new NullActionSceneModelAction();
		IActionSceneModelAction dieAction = new NullActionSceneModelAction();

		if(model.hasAction(SpaceCharacterAction.Walk.getName()))
			walkAction = model.getAction(SpaceCharacterAction.Walk.getName());
		else
			m_logger.error("Walking action does not exist in character model, using null walking action.");
		
		if(model.hasAction(SpaceCharacterAction.Idle.getName()))
			idleAction = model.getAction(SpaceCharacterAction.Idle.getName());
		else
			m_logger.error("Idle action does not exist in character model, using null idle action.");
		
		
		if(model.hasAction(SpaceCharacterAction.Die.getName()))
			dieAction = model.getAction(SpaceCharacterAction.Die.getName());
		else
			m_logger.error("Die action does not exist in character model, using null idle action.");
		
		if(!attributes.has(SpaceCharacterAttribute.Speed))
			m_logger.error("Character does not have a speed attribute. Creating default attribute for speed.");
		
		if(!attributes.has(SpaceCharacterAttribute.EffectiveHitpoints))
			m_logger.error("Character does not have a hits attribute. Assuming default hits.");
		
		return new UsrMovementResolver(host, model, attributes.get(SpaceCharacterAttribute.Speed), attributes.get(SpaceCharacterAttribute.EffectiveHitpoints), walkAction, idleAction, dieAction);
	}
	
	private static final class UsrMovementResolver implements IMovementResolver
	{
		private final VelocityLimitSteeringDriver m_driver;

		private final IImmutableAttribute m_speed;
		private final IImmutableAttribute m_health;
		private final IActionSceneModelAction m_walkAction;
		private final IActionSceneModelAction m_idleAction;
		private final IActionSceneModelAction m_dieAction;
		private final LinkedList<IMovementDirector> m_queue = new LinkedList<>();
		
		public UsrMovementResolver(IRpgCharacter host, final ISceneModel model, IImmutableAttribute speed, IImmutableAttribute health, IActionSceneModelAction walkAction, IActionSceneModelAction idleAction, IActionSceneModelAction dieAction)
		{
			m_speed = speed;
			m_health = health;
			m_walkAction = walkAction;
			m_idleAction = idleAction;
			m_dieAction = dieAction;
			
			m_driver =  new VelocityLimitSteeringDriverFactory(m_speed.get()).create();
			m_driver.attach(host.getBody());
		
			m_driver.getBehaviors().add(new ISteeringBehavior() {
				@Override
				public Vector2F direct()
				{
					if(!m_walkAction.isActive() || m_queue.isEmpty())
						return new Vector2F();
					
					Vector2F direction = m_queue.peek().getBehavior().direct();

					Direction dir = Direction.fromVector(direction);
					if(dir != Direction.Zero)
						model.setDirection(Direction.fromVector(direction));

					return direction;
				}
			});
			
			host.getObservers().add(new IEntityBodyObserver() {
				@Override
				public void bodyChanged(IPhysicsBody oldBody, IPhysicsBody newBody)
				{
					m_driver.dettach();
					m_driver.attach(newBody);
				}
			});
		}

		@Override
		public IMovementDirector getActiveDirector() {
			IMovementDirector currentDirector = m_queue.peek();

			//dequeue items until we find one which is not done.
			for(; currentDirector != null && currentDirector.isDone(); currentDirector = m_queue.peek())
				m_queue.pop();

			if(currentDirector == null)
				return new NullMovementDirector();

			return currentDirector;
		}

		@Override
		public void queue(IMovementDirector director)
		{
			m_queue.add(director);
		}

		@Override
		public void queueTop(IMovementDirector director)
		{
			m_queue.push(director);
		}
		
		@Override
		public void dequeue(IMovementDirector director)
		{
			m_queue.remove(director);
		}
		
		@Override
		public void update(int deltaTime)
		{
			if(m_health.isZero())
			{
				if(!(m_dieAction.isActive() || m_dieAction.isQueued()))
				{
					m_idleAction.cancel();
					m_walkAction.cancel();
					
					if(!m_dieAction.isQueued())
						m_dieAction.queue();
				}
				return;
			}
			
			IMovementDirector currentDirector = m_queue.peek();
			
			//dequeue items until we find one which is not done.
			for(; currentDirector != null && currentDirector.isDone(); currentDirector = m_queue.peek())
				m_queue.pop();
			
			if(currentDirector == null)
			{
				if(m_walkAction.isQueued())
					m_walkAction.dequeue();
				
				if(m_walkAction.isActive())
					m_walkAction.cancel();
			}
			else
			{
				float speed = currentDirector.getSpeed();
				if(speed < 0)
					speed = m_speed.get();

				m_driver.setSpeed(speed);
				if((m_walkAction.isActive() && m_walkAction.isDone()) || 
						!(m_walkAction.isQueued() || m_walkAction.isActive()))
					m_walkAction.queueTop();
			}
			
			if(!(m_walkAction.isQueued() || (m_walkAction.isActive() && !m_walkAction.isDone())) && !(m_idleAction.isQueued() || (m_idleAction.isActive() && !m_idleAction.isDone())))
				m_idleAction.queue();
			else if(m_walkAction.isQueued() && (m_idleAction.isQueued() || m_idleAction.isActive()))
			{
				m_idleAction.dequeue();
				m_idleAction.cancel();
			}
			
			m_driver.update(deltaTime);
		}

		@Override
		public IActionSceneModel decorate(IActionSceneModel subject)
		{
			return subject;
		}		
	}
}
