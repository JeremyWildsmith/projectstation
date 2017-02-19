/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.dcpu;

import com.jevaengine.spacestation.entity.INetworkDevice;
import de.codesourcery.jasm16.emulator.devices.IDcpuHardware;

/**
 *
 * @author Jeremy
 */
public interface IDcpuCompatibleDevice extends INetworkDevice {
	IDcpuHardware[] getHardware();
}
