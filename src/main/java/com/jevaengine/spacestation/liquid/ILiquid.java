/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jevaengine.spacestation.liquid;

/**
 *
 * @author Jeremy
 */
public interface ILiquid {
	//Ration between 0 and 1. What percentage of a difference flows.
	//Transfer rate per second
	//I.e. difference of 50, if flow rate is .5, than 25 is transfered per second
	float getRateOfFlow();
}
