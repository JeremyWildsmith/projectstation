/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.util.Nullable;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public interface INetworkDevice extends IDevice {
	<T extends INetworkDevice> List<T> getConnected(List<INetworkDevice> requested, Class<T> device);
	boolean isConnected(List<INetworkDevice> requested, INetworkDevice device);
	@Nullable
	String getNodeName();
}
