/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.power;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public interface IPowerDevice extends IDevice {
	int drawEnergy(List<IDevice> requested, int joules);
}
