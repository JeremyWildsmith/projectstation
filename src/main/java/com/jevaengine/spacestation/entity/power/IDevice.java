/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.power;

import io.github.jevaengine.world.entity.IEntity;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public interface IDevice extends IEntity {
	boolean addConnection(IDevice device);
	void removeConnection(IDevice device);
	void clearConnections();

	List<IDevice> getConnections();

	<T extends IDevice> List<T> getConnections(Class<T> device);
}
