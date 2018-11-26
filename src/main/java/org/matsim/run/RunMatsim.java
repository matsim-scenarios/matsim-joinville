/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.run;

import org.matsim.api.core.v01.DefaultActivityTypes;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.*;
import static org.matsim.core.config.groups.StrategyConfigGroup.*;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector.*;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy.ReRoute;

/**
 * @author nagel
 *
 */
public class RunMatsim {

	enum ScenarioInstance { equil, fromDaviTest, fromDaviProduction, fromJoinvilleDataTest }

	private static final ScenarioInstance scenarioInstance = ScenarioInstance.fromJoinvilleDataTest ;

	private final String[] args;
	private Config config ;
	private Scenario scenario ;
	private Controler controler ;

	public RunMatsim( String [] args ) {
		this.args = args ;
	}

	public final Config prepareConfig() {
		if ( args!=null && args.length>0 && args[0]!=null && args[0].length() > 0 ){
			config = ConfigUtils.loadConfig( args[0] ) ;
		} else {
			switch( scenarioInstance ) {
				case equil:
					config = ConfigUtils.loadConfig( "./scenarios/equil/config.xml" ) ;
					break;
				case fromDaviTest:
					config = ConfigUtils.loadConfig( "./scenarios/model-from-davi-2018-11-22/00config.xml" ) ;
					config.controler().setLastIteration( 0 );
					break;
				case fromDaviProduction:
					config = ConfigUtils.loadConfig( "./scenarios/model-from-davi-2018-11-22/00config.xml" ) ;
					break;
				case fromJoinvilleDataTest:
					config = ConfigUtils.createConfig() ;
					config.network().setInputFile( "./scenarios/model-from-joinville-data-2018-11-26/input/network.xml.gz" );
//					config.plans().setInputFile( "./scenarios/model-from-joinville-data-2018-11-26/input/pop.xml.gz" );
					config.plans().setInputFile( "./pop.xml.gz" );
					config.controler().setLastIteration( 1 );
				{
					ActivityParams params = new ActivityParams( DefaultActivityTypes.home ) ;
					params.setTypicalDuration( 3600.*12. );
					config.planCalcScore().addActivityParams( params );
				}
				{
					ActivityParams params = new ActivityParams( DefaultActivityTypes.work ) ;
					params.setTypicalDuration( 3600.*8. );
					params.setOpeningTime( 3600.*7. );
					params.setClosingTime( 3600.*18. );
					config.planCalcScore().addActivityParams( params );
				}
				{
					StrategySettings stratSets = new StrategySettings(  ) ;
					stratSets.setStrategyName( ReRoute );
					stratSets.setWeight( 0.1 );
					config.strategy().addStrategySettings( stratSets );
				}
				{
					StrategySettings stratSets = new StrategySettings(  ) ;
					stratSets.setStrategyName( ChangeExpBeta );
					stratSets.setWeight( 0.9 );
					config.strategy().addStrategySettings( stratSets );
				}
					break ;
				default:
						throw new RuntimeException( Gbl.NOT_IMPLEMENTED ) ;
			}
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setRoutingAlgorithmType( ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks );
		}

		return config ;
	}

	public final Scenario prepareScenario() {
		if ( config==null ) {
			prepareConfig() ;
		}
		scenario = ScenarioUtils.loadScenario( config ) ;
		return scenario ;
	}

	public final Controler prepareControler() {
		if ( scenario==null ) {
			this.prepareScenario() ;
		}
		controler = new Controler( scenario ) ;
		return controler ;
	}

	public final void run() {
		if ( controler==null ) {
			this.prepareControler() ;
		}
		controler.run() ;
	}

	public static void main(String[] args) {
		new RunMatsim( args ).run() ;
	}
	
}
