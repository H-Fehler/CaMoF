package camof.mobilitydemand;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import de.uniwuerzburg.omod.core.MobiAgent;
import de.uniwuerzburg.omod.core.Omod;
import de.uniwuerzburg.omod.core.Weekday;
import org.apache.commons.io.IOUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.*;
import org.opengis.referencing.operation.MathTransform;

import java.io.*;
import java.util.*;

import static camof.mobilitydemand.PostcodeManager.getPostcodePolygon;

public class AgentCollector {


    /**
     * This function collects agents whose home postcode is given in the "postcodesWithRemainingNeededAgents" map;
     * Returned is a map with postcode to agents;
     * */
    public static Map<String,List<MobiAgent>> collectUsefulAgents(int numberOfAgents, Map<String,Integer> postcodesWithRemainingNeededAgents, String osmFilePath, String areaFilePath){
        List<MobiAgent> allAgents = AgentCollector.createOmodAgents(numberOfAgents, osmFilePath, areaFilePath);
        //List<MobiAgent> usefulAgents = new ArrayList<>();
        Map<String,List<MobiAgent>> postcodesWithUsefulAgents = new HashMap<>();
        for(MobiAgent agent : allAgents){
            List<Object> returnWithPostcode = AgentCollector.checkIfAgentHomeInDesiredPostcode(agent,postcodesWithRemainingNeededAgents);
            boolean isAgentUseful = (boolean) returnWithPostcode.get(0);
            if(isAgentUseful){
                String postcodeOfAgent = (String) returnWithPostcode.get(1);
                List<MobiAgent> postcodeAgentsSoFar = new ArrayList<>();
                if(postcodesWithUsefulAgents.containsKey(postcodeOfAgent)){
                    postcodeAgentsSoFar = postcodesWithUsefulAgents.get(postcodeOfAgent);
                }
                postcodeAgentsSoFar.add(agent);
                postcodesWithUsefulAgents.put(postcodeOfAgent,postcodeAgentsSoFar);
                //usefulAgents.add(agent);
            }
            if(postcodesWithRemainingNeededAgents.keySet().isEmpty()){
                break;
            }
        }
        return postcodesWithUsefulAgents;
    }


    /**
     * This function checks if enough agents with a relevant home postcode were created in the omod mobilitysimulation.simulation;
     * */
    public static void checkIfEnoughUsefulAgents(String radiusFileString,String resultFileName, String osmFilePath, String areaFilePath, int numberOfAgents){
        List<Object> maps = AgentCollector.createPostcodeToNeededAgentsMap(radiusFileString);
        Map<String,Integer> postcodesWithRemainingNeededAgents = (Map<String, Integer>) maps.get(0);
        Map<String,Map<String,Integer>> postcodePairs = (Map<String, Map<String, Integer>>) maps.get(1);
        Map<String,List<MobiAgent>> postcodesWithUsefulAgents = AgentCollector.collectUsefulAgents(numberOfAgents, postcodesWithRemainingNeededAgents, osmFilePath, areaFilePath);
        if(!postcodesWithRemainingNeededAgents.keySet().isEmpty()){
            System.out.println("Nicht genug Agenten in den Postleitzahlgebieten.");
        }else{
            System.out.println("Genug Agenten. Sie werden nun mit ihrer Adresse gespeichert.");
            AgentCollector.saveUsefulAgentsWithHomeLocation(postcodesWithUsefulAgents,postcodePairs,resultFileName);
        }
    }


    /**
     * This function saves the collected agents with their longitude and latitude coordinate as well as their uni postcode in a file.
     * */
    public static void saveUsefulAgentsWithHomeLocation(Map<String,List<MobiAgent>> postcodesWithUsefulAgents, Map<String,Map<String,Integer>> postcodePairs,String resultFileName){
        try {
            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            org.json.simple.JSONArray postcode_objects = new org.json.simple.JSONArray();
            int counter = 1;
            for(String postcode : postcodesWithUsefulAgents.keySet()){
                int postcodeCounter = 0;
                Map<String,Integer> destinationPostcodeCounts = postcodePairs.get(postcode);
                List<String> postcodes = new ArrayList<>();
                for(String destinationPostcode : destinationPostcodeCounts.keySet()){
                    postcodes.addAll(Collections.nCopies(destinationPostcodeCounts.get(destinationPostcode), destinationPostcode));
                }
                Collections.shuffle(postcodes);
                JSONObject postcode_object = new JSONObject();
                org.json.simple.JSONArray agent_objects = new org.json.simple.JSONArray();
                List<MobiAgent> agents = postcodesWithUsefulAgents.get(postcode);
                for(MobiAgent agent : agents){
                    JSONObject agent_attributes = new JSONObject();
                    agent_attributes.put("agent id",counter);
                    agent_attributes.put("home location latitude",agent.getHome().getLatlonCoord().getX());
                    agent_attributes.put("home location longitude",agent.getHome().getLatlonCoord().getY());
                    agent_attributes.put("uni location",postcodes.get(postcodeCounter));
                    agent_objects.add(agent_attributes);
                    counter++;
                    postcodeCounter++;
                }
                postcode_object.put("postcode",postcode);
                postcode_object.put("agents",agent_objects);
                postcode_objects.add(postcode_object);
            }
            jsonObject.put("postcodes",postcode_objects);

            FileWriter file = new FileWriter(resultFileName);
            file.write(jsonObject.toJSONString());
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This function created the specified number of agents with the help of omod.
     * */
    public static List<MobiAgent> createOmodAgents(int number, String osmFilePath, String areaFilePath){
        File osmFile = new File("sources\\merged.osm.pbf"); //TODO
        File areaFile = new File("sources\\map.geojson"); //TODO
        Omod omod = Omod.Companion.defaultFactory(areaFile, osmFile);
        List<MobiAgent> agents = omod.run(number, Weekday.UNDEFINED, 1);
        return agents;
    }


    /**
     * This function reads request data from a specified file into a list of maps and returns this list.
     * */
    public static List<Map<String, String>> readRequestData(String filePath) throws IOException {
        File file = new File(filePath);
        List<Map<String, String>> response = new LinkedList<>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(';');
        MappingIterator<Map<String, String>> iterator = mapper.reader(Map.class)
                .with(schema)
                .readValues(file);
        while (iterator.hasNext()) {
            response.add(iterator.next());
        }
        return response;
    }


    /**
     * This function adds the request data to the saved agents.
     * */
    public static void addRequestsToUsefulAgents(String filePathToAgents,String filePathToRequests, String resultFileName) throws IOException {
        List<Map<String, String>> response = readRequestData(filePathToRequests);
        File agentsFile = new File(filePathToAgents);

        try{
            if (agentsFile.exists()) {
                InputStream is = new FileInputStream(filePathToAgents);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                JSONObject json = new JSONObject(jsonTxt);
                JSONArray postcodes = (JSONArray) json.get("postcodes");
                int counter = 0;
                for (Object obj : postcodes) {
                    JSONObject object = (JSONObject) obj;
                    JSONArray agentsInAPostcode = (JSONArray) object.get("agents");
                    for(Object agentObj : agentsInAPostcode){
                        JSONObject agentObject = (JSONObject) agentObj;

                        Map<String, String> map = response.get(counter);
                        agentObject.put("request time",map.get("Request_time"));
                        agentObject.put("arrival time",map.get("Ankunftszeit"));
                        agentObject.put("departure time",map.get("Abfahrtszeit"));

                        counter++;
                    }
                }
                FileWriter file = new FileWriter(resultFileName);
                file.write(json.toString());
                file.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * This function returns a list of two objects.
     * The first object is a map which specifies how many agents are needed per postcode.
     * The second object is a list of all given postcode pairs;
     * */
    public static List<Object> createPostcodeToNeededAgentsMap(String postcodeFilePath){
        Map<String,Integer> studentCountPerPostcode = new HashMap<>();
        Map<String,Map<String,Integer>> postcodePairs = new HashMap<>();
        List<Object> maps = new ArrayList<>();
        File postcodeFile = new File(postcodeFilePath);
        try{
            if (postcodeFile.exists()) {
                InputStream is = new FileInputStream(postcodeFilePath);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                org.json.JSONObject json = new JSONObject(jsonTxt);
                org.json.JSONArray array = (org.json.JSONArray) json.get("postcode pairs");
                for (Object obj : array) {
                    org.json.JSONObject object = (org.json.JSONObject) obj;
                    String postcode_pair = object.getString("postcode pair");
                    String postcodeHome = postcode_pair.split("-")[0];
                    String postcodeDestination = postcode_pair.split("-")[1];
                    Map<String, Integer> destinationPostcodeCounts;
                    if(postcodePairs.containsKey(postcodeHome)){
                        destinationPostcodeCounts = postcodePairs.get(postcodeHome);
                        if(destinationPostcodeCounts.containsKey(postcodeDestination)){
                            destinationPostcodeCounts.put(postcodeDestination,destinationPostcodeCounts.get(postcodeDestination)+object.getInt("student count"));
                        }else{
                            destinationPostcodeCounts.put(postcodeDestination,object.getInt("student count"));
                        }
                    }else{
                        destinationPostcodeCounts = new HashMap<>();
                        destinationPostcodeCounts.put(postcodeDestination,object.getInt("student count"));
                    }
                    postcodePairs.put(postcodeHome,destinationPostcodeCounts);
                    if(studentCountPerPostcode.containsKey(postcodeHome)){
                        studentCountPerPostcode.put(postcodeHome,studentCountPerPostcode.get(postcodeHome)+object.getInt("student count"));
                    }else{
                        studentCountPerPostcode.put(postcodeHome,object.getInt("student count"));
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        maps.add(studentCountPerPostcode);
        maps.add(postcodePairs);
        return maps;
    }


    /**
     * This function checks if a given agent lives in one of the postcodes given in "postcodesWithRemainingNeededAgents";
     * */
    public static List<Object> checkIfAgentHomeInDesiredPostcode(MobiAgent agent, Map<String,Integer> postcodesWithRemainingNeededAgents){
        Coordinate homeCoordinate = new CoordinateXY(agent.getHome().getLatlonCoord().getY(),agent.getHome().getLatlonCoord().getX());
        GeometryFactory gf = new GeometryFactory();
        Point home = gf.createPoint(homeCoordinate);
        try{
            MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, PostcodeManager.coordinateReferenceSystem);
            Geometry targetHome = JTS.transform( home, transform);
            for(String postcode : postcodesWithRemainingNeededAgents.keySet()){
                if(targetHome.within(getPostcodePolygon(postcode))){
                    int remainingNeededStudents = postcodesWithRemainingNeededAgents.get(postcode) - 1;
                    if(remainingNeededStudents == 0){
                        postcodesWithRemainingNeededAgents.remove(postcode);
                    }else{
                        postcodesWithRemainingNeededAgents.put(postcode,remainingNeededStudents);
                    }
                    List<Object> result = new ArrayList<>();
                    result.add(true);
                    result.add(postcode);
                    return result;
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        List<Object> result = new ArrayList<>();
        result.add(false);
        return result;
    }


}
