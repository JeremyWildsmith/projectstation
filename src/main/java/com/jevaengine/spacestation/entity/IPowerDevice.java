/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.world.entity.IEntity;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public interface IPowerDevice extends IEntity {
	boolean addConnection(IPowerDevice device);
	void removeConnection(IPowerDevice device);
	int drawEnergy(List<IPowerDevice> requested, int joules);
}
