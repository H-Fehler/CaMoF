package camof.mobilitydemand;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.io.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

public class PostcodeManager {

    public static CoordinateReferenceSystem coordinateReferenceSystem;

    static Map<String,MultiPolygon> postcodePolygons = new HashMap<>();


    public static CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return coordinateReferenceSystem;
    }


    public static void setCoordinateReferenceSystem(CoordinateReferenceSystem coordinateReferenceSystem) {
        PostcodeManager.coordinateReferenceSystem = coordinateReferenceSystem;
    }


    public static Map<String, MultiPolygon> getPostcodePolygons() {
        return postcodePolygons;
    }


    public static void setPostcodePolygons(Map<String, MultiPolygon> postcodePolygons) {
        PostcodeManager.postcodePolygons = postcodePolygons;
    }


    /**
     * This function fills the static postcodePolygon map of this class with the corresponding polygon to each postcode given in the OSM file
     * */
    public static void makePostcodePolygonMap() throws IOException {
        File file = new File("sources\\Postleitzahlengebiete_-_OSM\\OSM_PLZ.shp");
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source =
                dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE;

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                postcodePolygons.put((String)feature.getAttribute(2),(MultiPolygon)feature.getAttribute(0));
            }
        }
    }


    public static MultiPolygon getPostcodePolygon(String postcode) {
        return postcodePolygons.getOrDefault(postcode, null);
    }


    /**
     * This function returns all postcode pairs of students given in the file.
     * The format of the postcode pair is "*student_home_postcode*-*university_postcode*".
     * In order to differentiate between the places Wittelsbacherplatz and Hubland, their common postcode "97074" was extended with "97074A" for Wittelsbacherplatz and "97074B" for Hubland.
     * */
    public static Map<String,Integer> getAllPostcodes(String filePath) throws IOException {
        Map<String,Integer> plzsWithStudents = new HashMap<>();
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

        int undefinedCounter = 0;
        for(Map<String, String> map : response){
            if(map.get("postcode").equals("Undefined")){
                undefinedCounter++;
            }else{
                String faculty_plz = map.get("faculty_plz");
                if(faculty_plz.equals("97074")){
                    if(map.get("fb_str").contains("Humanwissenschaften")){
                        faculty_plz = faculty_plz + "A";
                    }else{
                        faculty_plz = faculty_plz + "B";
                    }
                }
                if(plzsWithStudents.containsKey(map.get("postcode") + "-" + faculty_plz)){
                    plzsWithStudents.put(map.get("postcode") + "-" + faculty_plz,plzsWithStudents.get(map.get("postcode") + "-" + faculty_plz)+Integer.parseInt(map.get("Number_of_students")));
                }else{
                    plzsWithStudents.put(map.get("postcode") + "-" + faculty_plz,Integer.parseInt(map.get("Number_of_students")));
                }
            }
        }
        return plzsWithStudents;
    }


    /**
     * This function returns a map with all postcode pairs that satisfy the given distance specifications.
     * */
    public static Map<String,Integer> getPostcodesWithinDistance(double upperDistance, String postcodePairFile) throws Exception {

        Map<String,Integer> postcodeDistributionInRadius = new HashMap<>();
        int studentCount = 0;


        List<Object> twoMaps = PostcodeManager.readPostcodePairDistances(postcodePairFile);
        Map<String,Double> postcodeDistances = (Map<String, Double>) twoMaps.get(0);
        Map<String,Integer> studentCounts = (Map<String, Integer>) twoMaps.get(1);
        for(String plz : postcodeDistances.keySet()){
            double distance = postcodeDistances.get(plz);

            //if(distance<=upperDistance && distance>lowerDistance){
            if(distance<=upperDistance){
                postcodeDistributionInRadius.put(plz,studentCounts.get(plz));
                studentCount = studentCount + studentCounts.get(plz);
            }
        }

        return postcodeDistributionInRadius;

    }


    /**
     * This function saves the postcode pairs in the given radius with their corresponding student count
     * */
    public static void putOutPostcodeJson(Map<String,Integer> postcodeDistributionInRadius, String resultFileName){
        try {
            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            org.json.simple.JSONArray pairObjects = new org.json.simple.JSONArray();
            int all_students_count = 0;
            for(String postcodePair : postcodeDistributionInRadius.keySet()){
                JSONObject innerPairObject = new JSONObject();
                innerPairObject.put("postcode pair",postcodePair);
                innerPairObject.put("student count",postcodeDistributionInRadius.get(postcodePair));
                all_students_count = all_students_count + postcodeDistributionInRadius.get(postcodePair);
                pairObjects.add(innerPairObject);
            }
            jsonObject.put("number of all students",all_students_count);
            jsonObject.put("postcode pairs",pairObjects);
            FileWriter file = new FileWriter(resultFileName);
            file.write(jsonObject.toJSONString());
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This function extracts postcode pair distances from a file and returns them.
     * If this file does not exist, all postcode pair distances are calculated and another function is called for saving the distances in a file.
     * */
    public static List<Object> readPostcodePairDistances(String postcodePairFile) throws IOException, TransformException {
        List<Object> twoMaps = new ArrayList<>();
        Map<String,Double> distanceMap = new HashMap<>();
        Map<String,Integer> studentCountMap = new HashMap<>();
        if (new File(postcodePairFile).exists()){
            InputStream is = new FileInputStream(postcodePairFile);
            String jsonTxt = IOUtils.toString(is, "UTF-8");
            org.json.JSONObject json = new JSONObject(jsonTxt);
            org.json.JSONArray array = (org.json.JSONArray) json.get("postcode pairs");
            for(Object obj : array){
                org.json.JSONObject object = (org.json.JSONObject) obj;
                if(object.get("distance") instanceof BigDecimal dec){
                    distanceMap.put((String) object.get("postcode pair"),dec.doubleValue());
                }else{
                    double d = (int) object.get("distance");
                    distanceMap.put((String) object.get("postcode pair"),d);
                }
                studentCountMap.put(object.getString("postcode pair"),object.getInt("student count"));
            }
        }
        twoMaps.add(distanceMap);
        twoMaps.add(studentCountMap);
        return twoMaps;
    }


    public static void writePostcodePairDistances(Map<String,Integer> plzs, String resultFileName) throws TransformException {
        Map<String,Double> distanceMap = new HashMap<>();
        Map<String,Integer> studentCountMap = new HashMap<>();

        for(String plz : plzs.keySet()){
            MultiPolygon plz1 = getPostcodePolygon(plz.split("-")[0]);
            String plz2String = plz.split("-")[1];
            if(plz2String.endsWith("A") || plz2String.endsWith("B")){
                plz2String = plz2String.substring(0,plz2String.length()-1);
            }
            MultiPolygon plz2 = getPostcodePolygon(plz2String);
            if(plz1 != null && plz2 != null){
                Coordinate[] cos = DistanceOp.nearestPoints(plz1,plz2);
                double distance = JTS.orthodromicDistance(cos[0],cos[1],coordinateReferenceSystem);
                //double distance = JTS.orthodromicDistance(plz1.getCentroid().getCoordinate(),plz2.getCentroid().getCoordinate(),coordinateReferenceSystem);
                distance = distance/1000;

                distanceMap.put(plz,distance);
                studentCountMap.put(plz,plzs.get(plz));
            }
        }
        PostcodeManager.putOutPostcodeDistanceJson(distanceMap,studentCountMap,resultFileName);
    }


    /**
     * This function saves given postcode pair distances in a file.
     * */
    public static void putOutPostcodeDistanceJson(Map<String,Double> postcodePairsDistance, Map<String,Integer> studentCountMap, String resultFileName){
        try {
            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            org.json.simple.JSONArray pairObjects = new org.json.simple.JSONArray();
            for(String postcodePair : postcodePairsDistance.keySet()){
                JSONObject innerPairObject = new JSONObject();
                innerPairObject.put("postcode pair",postcodePair);
                innerPairObject.put("distance",postcodePairsDistance.get(postcodePair));
                innerPairObject.put("student count",studentCountMap.get(postcodePair));
                pairObjects.add(innerPairObject);
            }
            jsonObject.put("postcode pairs",pairObjects);
            FileWriter file = new FileWriter(resultFileName);
            file.write(jsonObject.toJSONString());
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
