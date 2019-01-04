/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.pathfinding;

import io.github.jevaengine.math.Rect2F;
import io.github.jevaengine.math.Vector2F;
import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.SceneArtifact;
import io.github.jevaengine.world.pathfinding.IRoutingRules;
import io.github.jevaengine.world.search.RectangleSearchFilter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class RoomRestrictedDevicePathFinder implements IRoutingRules {

	boolean isAccessible(IEntity[] entities) {
		for (IEntity e : entities) {
			if (e instanceof Door) {
				return false;
			}
			
			if(e instanceof SceneArtifact && e.getBody().isCollidable())
				return false;
		}
		
		return true;
	}

	@Override
	public Direction[] getMovements(World world, Vector2F origin) {
		List<Direction> directions = new ArrayList<>();

		for (Direction dir : Direction.HV_DIRECTIONS) {
			Vector2F newLocation = origin.add(dir.getDirectionVector());
			Rect2F searchBounds = new Rect2F(.8F, .8F);
			searchBounds = searchBounds.add(newLocation).add(new Vector2F(-.5F, -.5F));
			RectangleSearchFilter<IEntity> searchFilter = new RectangleSearchFilter<>(searchBounds);

			IEntity entities[] = world.getEntities().search(IEntity.class, searchFilter);

			if(isAccessible(entities))
				directions.add(dir);
		}

		return directions.toArray(new Direction[directions.size()]);
	}

}
