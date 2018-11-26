package br.joinville.inputDataPreparation.demand;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.beanutils.converters.AbstractConverter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import sun.java2d.pipe.SpanShapeRenderer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

class CreateDemandFromJoinvilleData{
    private static final String baseDir = "./original-input-data/data-from-joinville-2018-11-23/AnaBazzanFiles/" ;
    private static final String zonesFile =  baseDir + "TrafficZones/clusters2.shp" ;
    private static final String odFile = baseDir + "ODMatrix/OD_AsList.csv" ;

    private static final Logger log = Logger.getLogger( CreateDemandFromJoinvilleData.class ) ;

    private static final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:32722" );

    public static void main( String[] args ){
        log.setLevel( Level.INFO );

        Logger.getLogger( AbstractConverter.class ).setLevel( Level.INFO );

        SimpleFeatureSource fts = ShapeFileReader.readDataFile( zonesFile ); //reads the shape file

        Map<String, SimpleFeature> featureMap = new LinkedHashMap<>();

        //Iterator to iterate over the features from the shape file
        try( SimpleFeatureIterator it = fts.getFeatures().features() ){
            while( it.hasNext() ){
                SimpleFeature ft = it.next(); //A feature contains a geometry (in this case a polygon) and an arbitrary number of attributes
                final String sweeping_s = (String) ft.getAttribute( "sweeping_s" );
//                log.warn( sweeping_s ) ;
                featureMap.put( sweeping_s, ft );
            }
        } catch( Exception ee ){
            throw new RuntimeException( ee );
        }

        // ---

        Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() ) ;
        Population pop = scenario.getPopulation() ;
        PopulationFactory pf = pop.getFactory() ;

        try( final FileReader reader = new FileReader( odFile ) ){
            final CsvToBeanBuilder<Record> builder = new CsvToBeanBuilder<>( reader );
            builder.withType( Record.class );
            builder.withSeparator( ' ' );
            final CsvToBean<Record> reader2 = builder.build();
            int ii=0 ;
            Random rnd = new Random(4711) ;
            for( Iterator<Record> it = reader2.iterator() ; it.hasNext() ; ){
                Record record = it.next();

                final int flow = Integer.parseInt( record.Flow );
                if ( flow > 0 ){
//                    log.warn( record.toString() );
                }

                final String fromZoneId = record.FromZoneId;
//                log.warn( "fromZoneId=" + fromZoneId ) ;
                SimpleFeature polygonOrigin = featureMap.get( fromZoneId );
                Gbl.assertNotNull( polygonOrigin );
                SimpleFeature polygonDestination = featureMap.get( record.ToZoneId ) ;
                Gbl.assertNotNull( polygonDestination );

                for ( int jj = 0 ; jj < flow ; jj++ ) {

                    ii++ ; if ( ii>10 ) break ;

                    Person person = pf.createPerson( Id.createPersonId( ii ) ) ;
                    {
                        Plan plan = pf.createPlan();
                        person.addPlan( plan );

                        Coord homeCoord = createRandomCoordinateInFeature( rnd, polygonOrigin );

                        {
                            Activity act = pf.createActivityFromCoord( DefaultActivityTypes.home, homeCoord );
                            act.setEndTime( 3600.*7. );
                            plan.addActivity( act );
                        }
                        {
                            Leg leg = pf.createLeg( TransportMode.car );
                            plan.addLeg( leg );
                        }
                        {
                            Coord workCoord = createRandomCoordinateInFeature( rnd, polygonDestination ) ;
                            Activity act = pf.createActivityFromCoord( DefaultActivityTypes.work, workCoord );
                            act.setEndTime( 3600.*8. ); // yyyyyy for debugging
                            plan.addActivity( act );
                        }
                        {
                            Leg leg = pf.createLeg( TransportMode.car );
                            plan.addLeg( leg );
                        }
                        {
                            Activity act = pf.createActivityFromCoord( DefaultActivityTypes.home, homeCoord );
                            plan.addActivity( act );
                        }
                    }
                    pop.addPerson( person );
                }
            }
        } catch( IOException e ){
            e.printStackTrace();
        }

        // ---

        PopulationUtils.writePopulation( pop, "pop.xml.gz" );

    }

    private static Coord createRandomCoordinateInFeature( Random rnd, SimpleFeature polygonOrigin ){
        Point point = GeometryUtils.getRandomPointInFeature( rnd, polygonOrigin );
        Coord coordInOrigCRS = CoordUtils.createCoord( point.getX(), point.getY() ) ;
        Coord homeCoord = ct.transform(coordInOrigCRS) ;
        Gbl.assertNotNull( homeCoord );
        return homeCoord;
    }

    public final static class Record {
        // needs to be public, otherwise one gets some incomprehensible exception.  kai, nov'17

        @CsvBindByName private String FromZoneId ;
        @CsvBindByName private String ToZoneId ;
        @CsvBindByName private String Flow ;

        @Override public String toString() {
            return this.FromZoneId
                    + "\t" + this.ToZoneId
                    + "\t" + this.Flow ;
        }
    }
}
