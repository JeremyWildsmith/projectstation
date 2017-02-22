/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.world.entity.IEntity;

/**
 *
 * @author Jeremy
 */
public interface IDevice extends IEntity {
	boolean addConnection(IDevice device);
	void removeConnection(IDevice device);
	void clearConnections();
}
