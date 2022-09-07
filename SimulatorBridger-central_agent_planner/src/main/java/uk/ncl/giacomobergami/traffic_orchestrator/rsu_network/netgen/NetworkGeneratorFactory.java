/*
 * NetworkGeneratorFactory.java
 * This file is part of SimulatorBridger-central_agent_planner
 *
 * Copyright (C) 2022 - Giacomo Bergami
 *
 * SimulatorBridger-central_agent_planner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * SimulatorBridger-central_agent_planner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SimulatorBridger-central_agent_planner. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.netgen;

import uk.ncl.giacomobergami.utils.design_patterns.ReflectiveFactoryMethod;

public class NetworkGeneratorFactory {
    public static NetworkGenerator generateFacade(String clazzPath) {
        return ReflectiveFactoryMethod
                .getInstance(NetworkGenerator.class)
                .generateFacade(clazzPath, BogusNetworkGenerator::getInstance);
    }
}
