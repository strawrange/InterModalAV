/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package drt.optimizer.insertion.filter;

import drt.data.DrtRequest;
import drt.optimizer.VehicleData;
import drt.optimizer.VehicleData.Entry;

import java.util.List;

/**
 * @author  jbischoff
 * An interface to pre-filter vehicles.
 */

public interface DrtVehicleFilter {

	/**
	 *  applies a prefiltering to Vehicle Data set based on a certain request.
	 */
	List<Entry>  applyFilter(DrtRequest drtRequest, VehicleData vData);
}
