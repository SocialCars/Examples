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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.lightjason.trafficsimulation.common.CCommon;
import org.lightjason.trafficsimulation.elements.IObject;
import org.lightjason.trafficsimulation.elements.area.CArea;
import org.lightjason.trafficsimulation.elements.area.IArea;
import org.lightjason.trafficsimulation.elements.environment.CEnvironment;
import org.lightjason.trafficsimulation.elements.environment.IEnvironment;
import org.lightjason.trafficsimulation.elements.vehicle.CVehicle;
import org.lightjason.trafficsimulation.elements.vehicle.IVehicle;
import org.lightjason.trafficsimulation.ui.api.CData;
import org.lightjason.trafficsimulation.ui.api.CMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;


/**
 * runtime task instance
 */
public class CTask implements ITask
{
    /**
     * logger
     */
    private static final Logger LOGGER = CCommon.logger( ITask.class );
    /**
     * thread
     */
    private final Thread m_thread;

    /**
     * ctor
     *
     * @param p_asl asl map
     * @param p_elements element map
     */
    public CTask( @Nonnull final Map<String, Pair<Boolean, String>> p_asl, @Nonnull final Map<String, IObject<?>> p_elements )
    {
        m_thread = new Thread( () ->
        {

            // --- initialize generators ---
            final IEnvironment.IGenerator<IEnvironment> l_environmentgenerator = this.generatorenvironment( p_asl );

            if ( l_environmentgenerator == null )
                return;

            final IEnvironment l_environment = l_environmentgenerator.generatesingle(
                p_elements,
                this.generatorvehicle( p_asl, "defaultvehicle", IVehicle.ETYpe.DEFAULTVEHICLE ),
                this.generatorvehicle( p_asl, "uservehicle", IVehicle.ETYpe.USERVEHICLE ),
                this.generatorarea( p_asl )
            );

            if ( l_environment == null )
                return;


            // --- execute objects ---
            CMessage.CInstance.INSTANCE.write(
                CMessage.EType.SUCCESS,
                CCommon.languagestring( this, "initialize", "" ),
                CCommon.languagestring( this, "simulationstart" )
            );


            p_elements.put( l_environment.id(), l_environment );
            while ( !l_environment.shutdown() )
            {
                p_elements.values().parallelStream().forEach( CTask::execute );
                try
                {
                    Thread.sleep( ERuntime.INSTANCE.time().get() );
                }
                catch ( final InterruptedException l_exception )
                {
                    break;
                }
            }

            p_elements.clear();

            CMessage.CInstance.INSTANCE.write(
                CMessage.EType.SUCCESS,
                CCommon.languagestring( this, "shutdown" ),
                CCommon.languagestring( this, "simulationstop" )
            );


            // test sending penalty data
            CData.CInstance.INSTANCE.penalty( 3.5 );
        } );
    }


    /**
     * gets a generator of a vehicle
     *
     * @param p_agents agent map
     * @param p_agent agent id
     * @param p_type vehicle type
     * @return null or generator
     */
    @Nullable
    private IVehicle.IGenerator<IVehicle> generatorvehicle( @Nonnull final Map<String, Pair<Boolean, String>> p_agents,
                                                            @Nonnull final String p_agent, @Nonnull final IVehicle.ETYpe p_type )
    {
        final Pair<Boolean, String> l_asl = p_agents.get( p_agent );
        try
        {
            return new CVehicle.CGenerator(
                IOUtils.toInputStream( l_asl.getRight(), "UTF-8" ),
                l_asl.getLeft(),
                p_type
            ).resetcount();
        }
        catch ( final Exception l_exception )
        {
            CMessage.CInstance.INSTANCE.write(
                CMessage.EType.ERROR,
                CCommon.languagestring( this, "initialize", p_agent ),
                l_exception.getLocalizedMessage()
            );
            return null;
        }
    }


    /**
     * area generator
     *
     * @param p_agents agent map
     * @return null or generator
     */
    @Nullable
    private IArea.IGenerator<IArea> generatorarea( @Nonnull final Map<String, Pair<Boolean, String>> p_agents )
    {
        try
        {
            return new CArea.CGenerator( IOUtils.toInputStream( p_agents.get( "area" ).getRight(), "UTF-8" ) )
                .resetcount();
        }
        catch ( final Exception l_exception )
        {
            CMessage.CInstance.INSTANCE.write(
                CMessage.EType.ERROR,
                CCommon.languagestring( this, "initialize", "area" ),
                l_exception.getLocalizedMessage()
            );
            return null;
        }
    }


    /**
     * environment generator
     *
     * @param p_agents agent map
     * @return null or generator
     */
    @Nullable
    private IEnvironment.IGenerator<IEnvironment> generatorenvironment( @Nonnull final Map<String, Pair<Boolean, String>> p_agents )
    {
        try
        {
            return new CEnvironment.CGenerator( p_agents.get( "environment" ).getRight() ).resetcount();
        }
        catch ( final Exception l_exception )
        {
            CMessage.CInstance.INSTANCE.write(
                CMessage.EType.ERROR,
                CCommon.languagestring( this, "initialize", "environment" ),
                l_exception.getLocalizedMessage()
            );
            return null;
        }
    }



    /**
     * execute any object
     *
     * @param p_object callable
     */
    private static void execute( @Nonnull final Callable<?> p_object )
    {
        try
        {
            p_object.call();
        }
        catch ( final Exception l_execution )
        {
            LOGGER.warning( l_execution.getLocalizedMessage() );
        }
    }


    @Override
    public final ITask call() throws Exception
    {
        if ( ( !m_thread.isAlive() ) && ( !m_thread.isInterrupted() ) )
            m_thread.start();

        return this;
    }

    @Override
    public final boolean running()
    {
        return m_thread.isAlive();
    }
}
