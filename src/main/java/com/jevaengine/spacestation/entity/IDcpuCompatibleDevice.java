/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;
import com.jevaengine.spacestation.entity.INetworkNode;

/**
 *
 * @author Jeremy
 */
public interface IDcpuCompatibleDevice extends INetworkNode {
	IDcpuHardware[] getHardware();
}
