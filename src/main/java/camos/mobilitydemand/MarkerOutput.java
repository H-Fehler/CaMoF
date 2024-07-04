package camos.mobilitydemand;

import camos.modeexecution.Coordinate;
import org.apache.commons.io.IOUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.CoordinateXY;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkerOutput {

    public static void readAllAgentsAndOutputJson(String filePathToAgents, String resultFileName, List<String> allPostcodes){
        File agentsFile = new File(filePathToAgents);

        try {
            if (agentsFile.exists()) {
                InputStream is = new FileInputStream(filePathToAgents);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                JSONObject json = new JSONObject(jsonTxt);
                JSONArray postcodes = (JSONArray) json.get("postcodes");
                JSONObject markerJson = new JSONObject();
                markerJson.put("type","FeatureCollection");
                JSONArray features = new JSONArray();

                for (Object obj : postcodes) {
                    JSONObject object = (JSONObject) obj;
                    JSONArray agentsInAPostcode = (JSONArray) object.get("agents");
                    for (Object agentObj : agentsInAPostcode) {
                        JSONObject agentObject = (JSONObject) agentObj;
                        double homeLongitude = agentObject.getDouble("home location longitude");
                        double homeLatitude = agentObject.getDouble("home location latitude");
                        String uniPostcode = agentObject.getString("uni location");

                        JSONObject color = new JSONObject();
                        color.put("color",postcodeToColorMapper(uniPostcode,allPostcodes));
                        JSONObject feature = new JSONObject();
                        feature.put("type","Feature");
                        JSONObject properties = new JSONObject();
                        properties.put("_umap_options",color);
                        JSONObject geometry = new JSONObject();
                        geometry.put("type","Point");
                        geometry.put("coordinates",new double[]{homeLongitude,homeLatitude});
                        feature.put("properties",properties);
                        feature.put("geometry",geometry);

                        features.put(feature);

                    }
                }

                markerJson.put("features",features);

                FileWriter file = new FileWriter(resultFileName);
                file.write(markerJson.toString());
                file.close();
            }
        }catch (Exception e){
            throw new RuntimeException("Error for MarkerOutput");
        }
    }


    public static void readRadiusAgentsAndOutputJson(String filePathToAgents, double lowerRadius, double upperRadius, String resultFileName, List<String> allPostcodes, String pathToConfigFile) throws Exception {
        File agentsFile = new File(filePathToAgents);
        String radiusFileName = "sources\\radius" + upperRadius + ".json";
        Map<String,Coordinate> postcodeToCoordinate = new HashMap<>();
        File configFile = new File(pathToConfigFile);

        if (configFile.exists()) {
            InputStream is = new FileInputStream(pathToConfigFile);
            String jsonTxt = IOUtils.toString(is, "UTF-8");
            org.json.JSONObject json = new org.json.JSONObject(jsonTxt);
            org.json.JSONObject postcodeMapping = json.getJSONObject("postcode mapping");
            for(String postcode : postcodeMapping.keySet()){
                org.json.JSONObject postcodeJson = postcodeMapping.getJSONObject(postcode);
                Coordinate uniCoordinate = new Coordinate(postcodeJson.getDouble("longitude"),postcodeJson.getDouble("latitude"));
                postcodeToCoordinate.put(postcode,uniCoordinate);
            }
        }else throw new RuntimeException("config does not exist!");

        File radiusFile = new File(radiusFileName);
        if(!radiusFile.exists()){
            String postcodePairFileString = "sources\\all_postcode_pairs.json";
            File postcodePairFile = new File(postcodePairFileString);
            if(!postcodePairFile.exists()){
                throw new RuntimeException("Die angegebene postcode pair Datei existiert nicht.");
            }
            Map<String,Integer> postcodes = PostcodeManager.getPostcodesWithinDistance(upperRadius,postcodePairFileString);
            PostcodeManager.putOutPostcodeJson(postcodes,"sources\\radius" + upperRadius + ".json");

        }

        try {
            if (agentsFile.exists()) {
                List<Object> maps = AgentCollector.createPostcodeToNeededAgentsMap(radiusFileName);
                Map<String,Integer> postcodesWithRemainingNeededAgents = (Map<String, Integer>) maps.get(0);
                InputStream is = new FileInputStream(filePathToAgents);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                JSONObject json = new JSONObject(jsonTxt);


                JSONArray array = (JSONArray) json.get("postcodes");
                JSONObject markerJson = new JSONObject();
                markerJson.put("type","FeatureCollection");
                JSONArray features = new JSONArray();

                for (Object postcodeObj : array) {
                    JSONObject postcodeObject = (org.json.JSONObject) postcodeObj;
                    String postcode = postcodeObject.getString("postcode");
                    if (postcodesWithRemainingNeededAgents.containsKey(postcode)) {
                        int numberOfAgentsToRetrieve = postcodesWithRemainingNeededAgents.get(postcode);
                        org.json.JSONArray agentArray = (org.json.JSONArray) postcodeObject.get("agents");
                        for (int i = 0; i < numberOfAgentsToRetrieve; i++) {
                            if (i == agentArray.length()) {
                                break;
                            }
                            JSONObject agentObject = agentArray.getJSONObject(i);
                            double homeLongitude = agentObject.getDouble("home location longitude");
                            double homeLatitude = agentObject.getDouble("home location latitude");
                            String uniPostcode = agentObject.getString("uni location");
                            org.locationtech.jts.geom.Coordinate homeCoordinate = new CoordinateXY(homeLongitude,homeLatitude);
                            org.locationtech.jts.geom.Coordinate campusCoordinate = new CoordinateXY(postcodeToCoordinate.get(uniPostcode).getLongitude(),postcodeToCoordinate.get(uniPostcode).getLatitude());

                            double distance = JTS.orthodromicDistance(homeCoordinate,campusCoordinate, DefaultGeographicCRS.WGS84);
                            distance = distance/1000;
                            if(distance<lowerRadius && (uniPostcode.equals("97080") || uniPostcode.equals("97074A") || uniPostcode.equals("97074B"))){
                                int f = 2;
                            }
                            if(distance <= upperRadius && distance>=lowerRadius){

                                JSONObject color = new JSONObject();
                                color.put("color",postcodeToColorMapper(uniPostcode,allPostcodes));
                                JSONObject feature = new JSONObject();
                                feature.put("type","Feature");
                                JSONObject properties = new JSONObject();
                                properties.put("_umap_options",color);
                                JSONObject geometry = new JSONObject();
                                geometry.put("type","Point");
                                geometry.put("coordinates",new double[]{homeLongitude,homeLatitude});
                                feature.put("properties",properties);
                                feature.put("geometry",geometry);

                                features.put(feature);
                            }
                        }
                    }
                }
                markerJson.put("features",features);

                FileWriter file = new FileWriter(resultFileName);
                file.write(markerJson.toString());
                file.close();
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Error for MarkerOutput");
        }
    }


    /*public static void readAllAgentsAndGroupOutputJson(String filePathToAgents, String resultFileName, String pathToConfigFile) throws IOException {
        File agentsFile = new File(filePathToAgents);
        File configFile = new File(pathToConfigFile);
        Map<String,Coordinate> postcodeToCoordinate = new HashMap<>();

        if (configFile.exists()) {
            InputStream is = new FileInputStream(pathToConfigFile);
            String jsonTxt = IOUtils.toString(is, "UTF-8");
            org.json.JSONObject json = new org.json.JSONObject(jsonTxt);
            GeneralManager.ridePooling.centralCoordinate = new Coordinate(json.getJSONObject("centralCoordinate").getDouble("longitude"),json.getJSONObject("centralCoordinate").getDouble("latitude"));
            GeneralManager.ridePooling.compareVector = new Coordinate(0.0, 1.0);
            GeneralManager.ridePooling.countOfGroups = json.getInt("countOfGroups");
            GeneralManager.ridePooling.compareAngle = (double) 360 / GeneralManager.ridePooling.countOfGroups;
            GeneralManager.ridePooling.radiusToExclude = json.getDouble("radiusToExclude");;
            org.json.JSONObject postcodeMapping = json.getJSONObject("postcode mapping");

            for(String postcode : postcodeMapping.keySet()){
                org.json.JSONObject postcodeJson = postcodeMapping.getJSONObject(postcode);
                Coordinate uniCoordinate = new Coordinate(postcodeJson.getDouble("longitude"),postcodeJson.getDouble("latitude"));
                postcodeToCoordinate.put(postcode,uniCoordinate);
            }
        }else throw new RuntimeException("config does not exist!");

        try {
            if (agentsFile.exists()) {
                InputStream is = new FileInputStream(filePathToAgents);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                JSONObject json = new JSONObject(jsonTxt);
                JSONArray postcodes = (JSONArray) json.get("postcodes");
                JSONObject markerJson = new JSONObject();
                markerJson.put("type","FeatureCollection");
                JSONArray features = new JSONArray();

                for (Object obj : postcodes) {
                    JSONObject object = (JSONObject) obj;
                    JSONArray agentsInAPostcode = (JSONArray) object.get("agents");
                    for (Object agentObj : agentsInAPostcode) {
                        JSONObject agentObject = (JSONObject) agentObj;
                        double homeLongitude = agentObject.getDouble("home location longitude");
                        double homeLatitude = agentObject.getDouble("home location latitude");
                        String uniPostcode = agentObject.getString("uni location");

                        JSONObject color = new JSONObject();
                        Request r = new Request();
                        r.setHomePosition(new Coordinate(homeLongitude,homeLatitude));
                        r.setDropOffPosition(postcodeToCoordinate.get(uniPostcode));
                        int groupnumber = GeneralManager.ridePooling.assignRequestToGroup(r);

                        color.put("color", groupnumber%2==0 ? "blue" : groupnumber==-1 ? "black" : "orange");
                        JSONObject feature = new JSONObject();
                        feature.put("type","Feature");
                        JSONObject properties = new JSONObject();
                        properties.put("_umap_options",color);
                        JSONObject geometry = new JSONObject();
                        geometry.put("type","Point");
                        geometry.put("coordinates",new double[]{homeLongitude,homeLatitude});
                        feature.put("properties",properties);
                        feature.put("geometry",geometry);

                        features.put(feature);
                    }
                }

                markerJson.put("features",features);

                FileWriter file = new FileWriter(resultFileName);
                file.write(markerJson.toString());
                file.close();
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Error for MarkerOutput");
        }
    }*/


    public static String postcodeToColorMapper(String postcode, List<String> postcodes){
        String[] colors = new String[]{"yellow","red","blue","green","pink","violet","orange"};
        Collections.sort(postcodes);
        for(int i=0; i<postcodes.size(); i++){
            if(postcode.equals(postcodes.get(i))){
                return colors[i];
            }
        }
        throw new RuntimeException("not a valid postcode");
    }


}
