/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity.network;

import com.jevaengine.spacestation.dcpu.devices.NetworkPacket;
import com.jevaengine.spacestation.entity.power.IDevice;

import java.util.List;

/**
 *
 * @author Jeremy
 */
public interface INetworkDataCarrier extends IDevice {
	void carry(List<INetworkDataCarrier> carried, NetworkPacket packet);
}
