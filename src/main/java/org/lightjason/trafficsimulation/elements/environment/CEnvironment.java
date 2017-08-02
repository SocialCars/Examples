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

package org.lightjason.trafficsimulation.elements.environment;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseObjectMatrix2D;
import com.codepoetics.protonpack.StreamUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lightjason.agentspeak.action.binding.IAgentAction;
import org.lightjason.agentspeak.action.binding.IAgentActionFilter;
import org.lightjason.agentspeak.action.binding.IAgentActionName;
import org.lightjason.agentspeak.configuration.IAgentConfiguration;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.ILiteral;
import org.lightjason.agentspeak.language.instantiable.plan.trigger.CTrigger;
import org.lightjason.agentspeak.language.instantiable.plan.trigger.ITrigger;
import org.lightjason.trafficsimulation.common.CCommon;
import org.lightjason.trafficsimulation.elements.CUnit;
import org.lightjason.trafficsimulation.elements.IBaseObject;
import org.lightjason.trafficsimulation.elements.IObject;
import org.lightjason.trafficsimulation.elements.area.IArea;
import org.lightjason.trafficsimulation.elements.vehicle.IVehicle;
import org.lightjason.trafficsimulation.ui.api.CAnimation;
import org.lightjason.trafficsimulation.ui.api.CData;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * environment agent
 */
@IAgentAction
public final class CEnvironment extends IBaseObject<IEnvironment> implements IEnvironment
{
    /**
     * serial id
     */
    private static final long serialVersionUID = -4598520582091540701L;
    /**
     * literal functor
     */
    private static final String FUNCTOR = "environment";
    /**
     * shutdown flag
     */
    private final AtomicBoolean m_shutdown = new AtomicBoolean();
    /**
     * set elements
     */
    private final Map<String, IObject<?>> m_elements;
    /**
     * grid
     */
    private final AtomicReference<ObjectMatrix2D> m_grid = new AtomicReference<>( new SparseObjectMatrix2D( 0, 0 ) );
    /**
     * set with areas
     */
    private final Set<IArea> m_areas = new CopyOnWriteArraySet<>();
    /**
     * area generator
     */
    private final IArea.IGenerator<IArea> m_generatorarea;
    /**
     * user vehicle generator
     */
    private final IVehicle.IGenerator<IVehicle> m_generatoruservehicle;
    /**
     * default vehicle generator
     */
    private final IVehicle.IGenerator<IVehicle> m_generatordefaultvehicle;
    /**
     * user vehicle execution flag
     */
    private final AtomicBoolean m_uservehicleuse = new AtomicBoolean();
    /**
     * set / cache with generated vehicles but not set within the grid
     */
    private final Set<IVehicle> m_vehiclecache = new CopyOnWriteArraySet<>();
    /**
     * number of lanes for each side
     */
    private final AtomicReference<Pair<Number, Number>> m_lanes = new AtomicReference<>( new ImmutablePair<>( 0, 0 ) );

    /**
     * ctor
     *
     * @param p_configuration agent configuration
     * @param p_id name of the object
     * @param p_elements objects
     * @param p_generatordefaultvehicle default vehicle generator
     * @param p_generatoruservehicle user vehicle generator
     */
    private CEnvironment( @Nonnull final IAgentConfiguration<IEnvironment> p_configuration, @Nonnull final String p_id,
                          @Nonnull final Map<String, IObject<?>> p_elements,
                          @Nonnull final IVehicle.IGenerator<IVehicle> p_generatordefaultvehicle,
                          @Nonnull final IVehicle.IGenerator<IVehicle> p_generatoruservehicle,
                          @Nonnull final IArea.IGenerator<IArea> p_generatorarea
    )
    {
        super( p_configuration, FUNCTOR, p_id );
        m_elements = p_elements;
        m_generatorarea = p_generatorarea;
        m_generatoruservehicle = p_generatoruservehicle;
        m_generatordefaultvehicle = p_generatordefaultvehicle;
    }

    @Nonnull
    @Override
    public final DoubleMatrix1D position()
    {
        return new DenseDoubleMatrix1D( new double[]{m_grid.get().rows(), m_grid.get().columns()} );
    }

    @Nonnull
    @Override
    public final DoubleMatrix1D nextposition()
    {
        return this.position();
    }

    @Override
    public final boolean shutdown()
    {
        return m_shutdown.get();
    }

    @Override
    public final synchronized boolean set( @Nonnull final IVehicle p_vehicle, @Nonnull final DoubleMatrix1D p_position )
    {
        final IVehicle l_vehicle = (IVehicle) m_grid.get().getQuick( (int) p_position.get( 0 ), (int) p_position.get( 1 ) );
        if ( l_vehicle != null )
            return false;

        m_grid.get().set( (int) p_position.get( 0 ), (int) p_position.get( 1 ), p_vehicle );
        p_vehicle.position().setQuick( 0, p_position.get( 0 ) );
        p_vehicle.position().setQuick( 1, p_position.get( 1 ) );
        return true;
    }

    @Override
    public final synchronized boolean move( @Nonnull final IVehicle p_vehicle )
    {
        final Number l_lane = p_vehicle.position().get( 0 );
        final Number l_start = p_vehicle.position().get( 1 );
        final Number l_target = p_vehicle.nextposition().get( 1 );

        if ( ( l_start.intValue() < l_target.intValue()
            ? IntStream.range( l_start.intValue() + 1, Math.min( l_target.intValue(), m_grid.get().columns() ) )
            : IntStream.range( Math.max( l_target.intValue(), 0 ), l_start.intValue() - 1 ) )
                      .parallel()
                      .filter( i -> m_grid.get().getQuick( l_lane.intValue(), i ) != null )
                      .findAny()
                      .isPresent()
        )
            return false;

        if ( ( l_target.intValue() > m_grid.get().columns() ) && ( p_vehicle.type().equals( IVehicle.ETYpe.USERVEHICLE ) ) )
        {
            this.trigger( CTrigger.from( ITrigger.EType.ADDGOAL, CLiteral.from( "shutdown" ) ), true );
            return true;
        }

        m_grid.get().setQuick( l_lane.intValue(), l_start.intValue(), null );
        m_grid.get().setQuick( l_lane.intValue(), l_target.intValue(), p_vehicle );
        p_vehicle.position().setQuick( 1, l_target.intValue() );
        return true;
    }

    @Override
    public final Map<String, Object> map( @Nonnull final EStatus p_status )
    {
        return StreamUtils.zip(
            Stream.of( "type", "status", "id", "length", "laneslefttoright", "lanesrighttoleft" ),
            Stream.of( FUNCTOR, p_status.toString(), this.id(), this.position().get( 1 ), m_lanes.get().getLeft(), m_lanes.get().getRight() ),
            ImmutablePair::new
        ).collect( Collectors.toMap( ImmutablePair::getLeft, ImmutablePair::getRight ) );
    }

    @Override
    protected final Stream<ILiteral> individualliteral( final Stream<IObject<?>> p_object )
    {
        return Stream.empty();
    }

    @Override
    public final IEnvironment call() throws Exception
    {
        super.call();

        // add all cached elements to the grid if possible
        m_vehiclecache.removeAll(
            m_vehiclecache.parallelStream()
                          .filter( i -> this.set( i, i.position() ) )
                          .peek( i -> m_elements.put( i.id(), i ) )
                          .collect( Collectors.toSet() )
        );

        return this;
    }

    // --- agent actions ---------------------------------------------------------------------------------------------------------------------------------------

    /**
     * action to initialize the simulation
     *
     * @param p_length length of the street in kilometer
     * @param p_lefttorightlanes number of lanes that goes from right to left
     * @param p_righttoleftlanes number of lanes that goes from left ro right
     */
    @IAgentActionFilter
    @IAgentActionName( name = "simulation/initialize" )
    private void simulationinitialize( final Number p_length, final Number p_lefttorightlanes, final Number p_righttoleftlanes )
    {
        if ( m_grid.get().size() != 0 )
            throw new RuntimeException( "world is initialized" );

        m_areas.clear();
        m_lanes.set( new ImmutablePair<>( p_lefttorightlanes, p_righttoleftlanes ) );
        m_grid.set( new SparseObjectMatrix2D(
            p_lefttorightlanes.intValue() + p_righttoleftlanes.intValue(),
            CUnit.INSTANCE.kilometertocell( p_length ).intValue()
        ) );

        CAnimation.CInstance.INSTANCE.send( EStatus.INITIALIZE, this );
    }

    /**
     * shuts down the current execution
     */
    @IAgentActionFilter
    @IAgentActionName( name = "simulation/shutdown" )
    private void simulationshutdown()
    {
        m_shutdown.set( true );
        CAnimation.CInstance.INSTANCE.send( EStatus.SHUTDOWN, this );
    }

    /**
     * send penalty to simulation
     *
     * @param p_value penalty value
     */
    @IAgentActionFilter
    @IAgentActionName( name = "simulation/penalty" )
    private void simulationpenalty( final Number p_value )
    {
        CData.CInstance.INSTANCE.penalty( p_value.doubleValue() );
    }

    /**
     * creates an area
     * @param p_positionfrom position start on the lane (inclusive)
     * @param p_positionto position end on the lane (inclusive)
     * @param p_lanefrom lane start (inclusive)
     * @param p_laneto lane end (inclusive)
     * @param p_maximumspeed maximum allowed speed
     */
    @IAgentActionFilter
    @IAgentActionName( name = "area/create" )
    private void areacreate( final Number p_positionfrom, final Number p_positionto, final Number p_lanefrom, final Number p_laneto, final Number p_maximumspeed )
    {
        final IArea l_area = m_generatorarea.generatesingle( this, new DenseDoubleMatrix1D(
            new double[]{p_lanefrom.doubleValue(), p_laneto.doubleValue(), p_positionfrom.doubleValue(), p_positionto.doubleValue()}
        ), p_maximumspeed );

        if ( l_area == null )
            return;

        m_areas.add( l_area );
        m_elements.put( l_area.id(), l_area );
    }


    /**
     * action to create default vehicle on the left side
     *
     * @param p_maximumspeed maximum speed in km/h
     * @param p_acceleration acceleration in m/s^2
     * @param p_deceleration deceleration in m/s^2
     * @param p_lane lane index (0 is right in driving direction)
     */
    @IAgentActionFilter
    @IAgentActionName( name = "vehicle/default/left" )
    private void defaultvehicleleft( @Nonnegative final Number p_maximumspeed, @Nonnegative final Number p_acceleration,
                                     @Nonnegative final Number p_deceleration, @Nonnegative final Number p_lane )
    {
        this.defaultvehicle(
            new DenseDoubleMatrix1D( new double[]{this.position().get( 0 ) - 1 - p_lane.intValue(), 0} ),
            this.position().get( 1 ) - 1,

            p_maximumspeed,
            p_acceleration,
            p_deceleration
        );
    }


    /**
     * creates a default vehicle
     *
     * @param p_start start position
     * @param p_goal goal position
     * @param p_maximumspeed maximum speed in km/h
     * @param p_acceleration acceleration in m/s^2
     * @param p_deceleration deceleration in m/s^2
     */
    private void defaultvehicle( @Nonnull final DoubleMatrix1D p_start, @Nonnegative final double p_goal,
                                 @Nonnegative final Number p_maximumspeed, @Nonnegative final Number p_acceleration, @Nonnegative final Number p_deceleration )
    {
        m_vehiclecache.add(
                 m_generatordefaultvehicle.generatesingle(
                 this,

                 p_start,
                 p_goal,

                 p_maximumspeed,
                 p_acceleration,
                 p_deceleration
            )
        );
    }


    /**
     * action to create an user vehicle
     *
     * @param p_maximumspeed maximum speed in km/h
     * @param p_acceleration acceleration in m/s^2
     * @param p_deceleration deceleration in m/s^2
     */
    @IAgentActionFilter
    @IAgentActionName( name = "vehicle/user" )
    private void uservehicle( @Nonnegative final Number p_maximumspeed, @Nonnegative final Number p_acceleration, @Nonnegative final Number p_deceleration )
    {
        if ( !m_uservehicleuse.compareAndSet( false, true ) )
            throw new RuntimeException( "user vehicle has be created" );

        m_vehiclecache.add(
            m_generatoruservehicle.generatesingle(
                this,

                new DenseDoubleMatrix1D( new double[]{this.position().get( 0 ) - 1, 0} ),
                this.position().get( 1 ) - 1,

                p_maximumspeed,
                p_acceleration,
                p_deceleration
            )
        );

    }

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * environment generator
     */
    public static final class CGenerator extends IBaseGenerator<IEnvironment>
    {
        /**
         * ctor
         *
         * @param p_asl asl source
         * @throws Exception on any error
         */
        public CGenerator( @Nonnull final String p_asl ) throws Exception
        {
            super( IOUtils.toInputStream( p_asl, "UTF-8" ), CEnvironment.class );
        }

        @Override
        public final IGenerator<IEnvironment> resetcount()
        {
            return this;
        }

        @Override
        @SuppressWarnings( "unchecked" )
        protected final Triple<IEnvironment, Boolean, Stream<String>> generate( @Nullable final Object... p_data )
        {
            if ( ( p_data == null ) || ( p_data.length < 3 ) )
                throw new RuntimeException( CCommon.languagestring( this, "parametercount" ) );

            return new ImmutableTriple<>(
                new CEnvironment(
                    m_configuration,
                    FUNCTOR,
                    (Map<String, IObject<?>>) p_data[0],
                    (IVehicle.IGenerator<IVehicle>) p_data[1],
                    (IVehicle.IGenerator<IVehicle>) p_data[2],
                    (IArea.IGenerator<IArea>) p_data[3]
                ),
                true,
                Stream.of( FUNCTOR )
            );
        }
    }

}
