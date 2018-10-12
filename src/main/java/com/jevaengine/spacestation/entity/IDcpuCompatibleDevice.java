/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;

/**
 *
 * @author Jeremy
 */
public interface IDcpuCompatibleDevice extends IDevice {
	IDcpuHardware[] getHardware();
}
