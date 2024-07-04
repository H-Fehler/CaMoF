package camos.modeexecution;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.jsprit.core.problem.Location;
import camos.GeneralManager;
import camos.mobilitydemand.PostcodeManager;
import camos.modeexecution.mobilitymodels.MobilityMode;
import camos.modeexecution.mobilitymodels.modehelpers.StartHelper;
import org.apache.commons.io.IOUtils;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;


public class ModeExecutionManager {


    public static Map<String,Object> configValues;
    public static Map<String, Coordinate> postcodeToCoordinate;
    public static Map<List<Location>,Long> timeMap;
    public static Map<List<Location>,Double> distanceMap;
    public static Map<String,JSONObject> modeValues;

    public static List<Agent> agents;
    public static GraphHopper graphHopper;
    public static Map<String,String> compareModes;
    public static Map<String,MobilityMode> finishedModes;
    public static double upperRadius;
    public static double lowerRadius;
    public static double percentOfWillingStudents;



    public static void startModes(String configPath) throws Exception {
        distanceMap = new HashMap<>();
        timeMap = new HashMap<>();
        modeValues = new HashMap<>();
        compareModes = new HashMap<>();
        finishedModes = new HashMap<>();
        String[] modes;

        graphHopper = new GraphHopper();
        graphHopper.setOSMFile("sources\\merged.osm.pbf"); //TODO
        graphHopper.setGraphHopperLocation("target/routing-graph-cache");
        graphHopper.setProfiles(new Profile("car").setVehicle("car").setTurnCosts(false),new Profile("foot").setVehicle("foot").setTurnCosts(false));
        graphHopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"),new CHProfile("foot"));
        graphHopper.importOrLoad();

        JSONObject config = new JSONObject(IOUtils.toString(new FileInputStream(configPath), "UTF-8"));
        getGlobalConfig(config);

        if(config.keySet().contains("modes") && config.keySet().contains("modejson") && new File(config.getString("modejson")).exists()){
            if(config.get("modes") instanceof JSONArray modesArray){
                modes = new String[modesArray.length()];
                for(int i=0; i<modesArray.length(); i++){
                    if(modesArray.get(i) instanceof String){
                        modes[i] = (String) modesArray.get(i);
                    }else throw new RuntimeException("'modes' parameter has to be a list of Strings.");
                }

                JSONObject modejson = new JSONObject(IOUtils.toString(new FileInputStream(config.getString("modejson")), "UTF-8"));
                configValues = StartHelper.readInConfigForModes(config,modejson,modes);

                PostcodeManager.setCoordinateReferenceSystem(CRS.decode("EPSG:3857"));
                PostcodeManager.makePostcodePolygonMap();

                for(String mode : modes){
                    if(compareModes.containsKey(mode)){
                        String comparedMode = compareModes.get(mode);
                        if(comparedMode.equals(mode) || (compareModes.containsKey(comparedMode) && compareModes.get(comparedMode).equals(mode))){
                            throw new RuntimeException("There is a cyclic dependency in the modes.");
                        }
                    }
                }
                modes = sortModes(List.of(modes)).toArray(new String[0]);

                for(String mode : modes){
                    startMode(mode,agents,modejson.getJSONObject(mode).getString("resultsFolder"));
                }

            }else throw new RuntimeException("'modes' parameter not a list of Strings.");

        }else throw new RuntimeException("modes json file not found.");
    }


    public static void startMode(String modeName, List<Agent> agents, String resultPath){
        MobilityMode mode = findMode(modeName);
        mode.prepareMode(agents);
        mode.startMode();
        if(mode.checkIfConstraintsAreBroken(agents)){
            throw new RuntimeException("Constraints of mode " + modeName + " were broken.");
        }
        mode.writeResults(modeName,resultPath);
        mode.writeAdditionalResults(resultPath);
        finishedModes.put(mode.getName(),mode);
    }


    public static MobilityMode findMode(String modeName){
        try {
            Class<?> modeClass = Class.forName("camos.modeexecution.mobilitymodels."+ ModeExecutionManager.modeValues.get(modeName).getString("class name"));
            Constructor<?> ctor = modeClass.getConstructor();
            return (MobilityMode) ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }



    public static void getGlobalConfig(JSONObject config) throws Exception {
        ModeExecutionManager.setGeneralManagerAttributes(config);

        ModeExecutionManager.lowerRadius = config.getDouble("lower radius");
        double upperRadius = config.getDouble("upper radius");
        ModeExecutionManager.upperRadius = upperRadius;

        String requestFile = config.getString("request file");
        String radiusFileName;
        if(config.keySet().contains("radius file")){
            radiusFileName = config.getString("radius file");
        }else{
            radiusFileName = "sources\\radius" + upperRadius + ".json";
        }

        File radiusFile = new File(radiusFileName);
        if(!radiusFile.exists()){
            String postcodePairFileString = config.getString("postcodePairFile");
            File postcodePairFile = new File(postcodePairFileString);
            if(!postcodePairFile.exists()){
                throw new RuntimeException("The specified postcode pair file does not exist.");
            }
            Map<String,Integer> postcodes = PostcodeManager.getPostcodesWithinDistance(upperRadius,postcodePairFileString);
            PostcodeManager.putOutPostcodeJson(postcodes,"sources\\radius" + upperRadius + ".json");
        }
        agents = AgentManager.readAgentsWithRequestData(requestFile,radiusFileName);
    }


    public static void setGeneralManagerAttributes(org.json.JSONObject json){
        if(json.keySet().contains("percentOfWillingStudents")){
            ModeExecutionManager.percentOfWillingStudents = json.getDouble("percentOfWillingStudents");
        }else ModeExecutionManager.percentOfWillingStudents = 100.0;

        Map<String,Coordinate> postcodeToCoordinate = new HashMap<>();
        org.json.JSONObject postcodeMapping = json.getJSONObject("postcode mapping");
        for(String postcode : postcodeMapping.keySet()){
            org.json.JSONObject postcodeJson = postcodeMapping.getJSONObject(postcode);
            Coordinate uniCoordinate = new Coordinate(postcodeJson.getDouble("longitude"),postcodeJson.getDouble("latitude"));
            postcodeToCoordinate.put(postcode,uniCoordinate);
        }

        ModeExecutionManager.postcodeToCoordinate = postcodeToCoordinate;
    }


    public static Coordinate turnUniPostcodeIntoCoordinate(String postcode){
        return ModeExecutionManager.postcodeToCoordinate.get(postcode);
    }


    public static String turnUniCoordinateIntoPostcode(Coordinate coordinate){
        for(String postcode : ModeExecutionManager.postcodeToCoordinate.keySet()){
            if(ModeExecutionManager.postcodeToCoordinate.get(postcode).equals(coordinate)){
                return postcode;
            }
        }
        return "wrong coordinate";
    }


    public static void testMode(MobilityMode mode) throws Exception {
        distanceMap = new HashMap<>();
        timeMap = new HashMap<>();

        if(GeneralManager.useGraphhopperForTests){
            graphHopper = new GraphHopper();
            graphHopper.setOSMFile("sources\\merged.osm.pbf"); //TODO
            graphHopper.setGraphHopperLocation("target/routing-graph-cache");
            graphHopper.setProfiles(new Profile("car").setVehicle("car").setTurnCosts(false),new Profile("foot").setVehicle("foot").setTurnCosts(false));
            graphHopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"),new CHProfile("foot"));
            graphHopper.importOrLoad();
        }

        JSONObject config = new JSONObject(IOUtils.toString(new FileInputStream("testConfig.json"), "UTF-8"));
        getGlobalConfig(config);
        mode.setGraphHopper(ModeExecutionManager.graphHopper);

        // Hier beginnt der tatsächliche Test
        mode.prepareMode(agents);
        mode.startMode();
        if(mode.checkIfConstraintsAreBroken(agents)){
            throw new RuntimeException("Constraints were broken.");
        }
        mode.writeResults(mode.getName()==null?"":mode.getName(),"");
        mode.writeAdditionalResults("");
    }

    static List<String> sortModes(List<String> modes){
        List<String> sortedModes = new ArrayList<>(modes);
        for(String mode : modes){
            if(compareModes.containsKey(mode) && modes.indexOf(mode)<modes.indexOf(compareModes.get(mode))){
                int index = modes.indexOf(mode);
                sortedModes.set(modes.indexOf(compareModes.get(mode)),mode);
                sortedModes.set(index,compareModes.get(mode));
                sortedModes = sortModes(sortedModes);
                break;
            }
        }
        return sortedModes;
    }

}
