/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason AgentSpeak(L++) Traffic-Simulation             #
 * # Copyright (c) 2017, LightJason (info@lightjason.org)                               #
 * # This program is free software: you can redistribute it and/or modify               #
 * # it under the terms of the GNU Lesser General Public License as                     #
 * # published by the Free Software Foundation, either version 3 of the                 #
 * # License, or (at your option) any later version.                                    #
 * #                                                                                    #
 * # This program is distributed in the hope that it will be useful,                    #
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of                     #
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                      #
 * # GNU Lesser General Public License for more details.                                #
 * #                                                                                    #
 * # You should have received a copy of the GNU Lesser General Public License           #
 * # along with this program. If not, see http://www.gnu.org/licenses/                  #
 * ######################################################################################
 * @endcond
 */

package org.lightjason.trafficsimulation.runtime;

import org.lightjason.trafficsimulation.elements.IObject;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;


/**
 * runtime instance
 */
public interface IRuntime extends Runnable
{

    /**
     * sets the task-supplier
     *
     * @param p_supplier task supplier
     * @return self reference
     */
    IRuntime supplier( @Nonnull final BiFunction<Map<String, ERuntime.CAgentDefinition>, Map<String, IObject<?>>, ITask> p_supplier );

    /**
     * task is running
     *
     * @return running flag
     */
    boolean running();

    /**
     * saves all data
     *
     * @return self reference
     */
    @Nonnull
    IRuntime save();

    /**
     * get time reference
     *
     * @return time
     */
    @Nonnull
    AtomicInteger time();


    /**
     * returns an element from the current executed objects
     *
     * @return current element list
     */
    @Nonnull
    Map<String, IObject<?>> elements();

    /**
     * agents map
     *
     * @return map with agent names and visibilites
     */
    @Nonnull
    Map<String, ERuntime.CAgentDefinition> agents();
}
