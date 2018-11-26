package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.controler.Controler ;

class RunMatsimSimple{

    public static void main ( String [] args ) {

        Config config ;
        if ( args!=null && args[0]!=null && args[0].length()>0 ){
            config = ConfigUtils.loadConfig( args[0] );
        } else {
            config = ConfigUtils.createConfig() ;
            // (works with empty network and population ... but evidently will not do anything).
        }

        Scenario scenario = ScenarioUtils.loadScenario( config ) ;


        Controler controler = new Controler( scenario ) ;


        controler.run() ;


    }

}
