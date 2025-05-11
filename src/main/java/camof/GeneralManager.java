package camof;

import camof.mobilitydemand.AgentCollector;
import camof.mobilitydemand.PostcodeManager;
import camof.mobilitydemand.RequestDataAdjuster;
import camof.modeexecution.*;
import camof.modeexecution.groupings.Match;
import org.opengis.referencing.operation.TransformException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

public class GeneralManager {

    public static Random random = new Random(1234);
    public static Request compareRequest = null; //TODO
    public static Match compareMatch = null; //TODO
    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static boolean useGraphhopperForTests = true;


    public static void main(String[] args) throws Exception {
        Map<Coordinate,String> stops = new HashMap<>();
        stops.put(new Coordinate(9.950588768110487,49.800803730211925),"01.01.2000 13:12:00-01.01.2000 13:18:00");
        //Match match = new Match(new ArrayList<>(),stops,null,null,new Coordinate(9.95217857027435,49.78715064067901),new Coordinate(9.971629846615247,49.78358231731071),Requesttype.DRIVETOUNI,);

        
        /*MobilityMode mode = new EverybodyDrives();
        ModeExecutionManager.testMode(mode);
        mode.writeResultsToFile();*/

        if(args.length<2){
            System.out.println("Es fehlen Argumente.");
        }else if(args[0].equals("startModes")){
            File configFile = new File(args[1]);
            if (configFile.exists()) {
                ModeExecutionManager.startModes(args[1]);
            }else throw new RuntimeException("config json file not found.");
        }else start(args);
    }


    public static void start(String[] args) throws Exception {
        String resultFile;

        switch (args[0]){
            case "createPostcodePairs":
                //args[1] has to be a csv File with the fields "fb_str;city;postcode;Number_of_students;faculty_plz". The separator should be ";".
                if(args.length<3){
                    resultFile = "sources\\all_postcode_pairs.json";
                }else{
                    resultFile = args[2];
                }
                GeneralManager.createPostcodePairFile(args[1],resultFile);
                break;
            case "createRadiusFile":
                if(args.length<3){
                    System.out.println("Es fehlen Argumente (postcodePairFile, upperRadius).");
                }else{
                    File postcodePairFile = new File(args[1]);
                    if(!args[1].endsWith(".json")){
                        System.out.println("Die angegebene postcodePairFile hat ein falsches Format.");
                    }else{
                        if(!postcodePairFile.exists()){
                            System.out.println("Die angegebene postcodePairFile existiert nicht.");
                        }else{
                            if(args.length<4){
                                try {
                                    Double.parseDouble(args[2]);
                                }catch (NumberFormatException nfe) {
                                    System.out.println("Es muss eine Zahl als Radius eingegeben werden.");
                                    break;
                                }
                                resultFile = "sources\\radius" + Double.parseDouble(args[2]) + ".json";
                            }else{
                                resultFile = args[3];
                            }
                            Map<String,Integer> postcodes = PostcodeManager.getPostcodesWithinDistance(Double.parseDouble(args[2]),args[1]);
                            PostcodeManager.putOutPostcodeJson(postcodes,resultFile);
                        }
                    }
                }
                break;
            case "createAgents":
                if(args.length<5){
                    System.out.println("Es fehlen Argumente (radiusFile, geojsonFile, numberOfAgents).");
                }else{
                    //args[1] has to be a radiusFile that is supposed to be the biggest possible radius
                    //args[2] has to be the osmFile and args[3] has to be the geojson file
                    //args[4] has to be the number of agents to create
                    try{
                        int number = Integer.parseInt(args[4]);
                        if(number<1){
                            System.out.println("Es muss eine ganze positive Zahl für die Anzahl der Agenten eingegeben werden.");
                            break;
                        }
                    }catch(NumberFormatException nfe){
                        System.out.println("Es muss eine ganze positive Zahl für die Anzahl der Agenten eingegeben werden.");
                        break;
                    }
                    if(args.length<6){
                        resultFile = "sources\\agentDistribution\\usefulAgents" + Integer.parseInt(args[4]) + ".json";
                    }else{
                        resultFile = args[5];
                    }
                    AgentCollector.checkIfEnoughUsefulAgents(args[1],resultFile,args[2],args[3],Integer.parseInt(args[4]));
                }
                break;
            case "adjustRequestData":
                if(args.length<2){
                    System.out.println("Die zu ändernde Request Datei wurde nicht angegeben.");
                }else{
                    if(args.length<3){
                        String[] s = args[1].split("\\\\");
                        s[s.length-1] = "";
                        String st = String.join("\\",s);
                        resultFile = st + "requestData.csv";
                    }else{
                        resultFile = args[2];
                    }
                    RequestDataAdjuster.adjustRequestData(args[1],resultFile);
                }
                break;
            case "addRequestData":
                if(args.length<3){
                    System.out.println("Es fehlen Argumente (usefulAgentsFile, requestDataFile).");
                }else{
                    if(args.length<4){
                        resultFile = "sources\\agentDistribution\\agentsWithRequests"+new SimpleDateFormat("yyyyMMddHHmm").format(new Date())+".json";
                    }else{
                        resultFile = args[3];
                    }
                    AgentCollector.addRequestsToUsefulAgents(args[1],args[2],resultFile);
                }
                break;
            default:
                System.out.println("Die Anweisung ist nicht gültig.");

        }
    }


    public static void createPostcodePairFile(String filePath, String resultFileName) throws IOException, TransformException {
        Map<String,Integer> plzs = PostcodeManager.getAllPostcodes(filePath);
        PostcodeManager.writePostcodePairDistances(plzs,resultFileName);
    }


    public static Stream<LocalDateTime> generateTimeScale(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return Stream.iterate(startDateTime, d -> d.plusMinutes(1L))
                .limit(ChronoUnit.MINUTES.between(startDateTime, endDateTime) + 1);
    }




}
