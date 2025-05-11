package camof.mobilitydemand;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import camof.GeneralManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.*;

public class RequestDataAdjuster {

    public static void adjustRequestData(String filePath, String resultFilePath) throws IOException {
        File file = new File(filePath);
        List<Map<String, String>> response = new LinkedList<>();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(',');
        MappingIterator<Map<String, String>> iterator = mapper.reader(Map.class)
                .with(schema)
                .readValues(file);
        while (iterator.hasNext()) {
            Map<String, String> rowData = iterator.next();
            calculateNewMap(rowData);
            response.add(rowData);
        }

        printNewData(resultFilePath,response);
    }


    public static Map<String, String> calculateNewMap(Map<String, String> rowData){
        rowData.put("old",rowData.get("Ankunftszeit"));
        rowData.put("Ankunftszeit",getTimeString(calculateTimeOutOfMinutes(Long.parseLong(rowData.get("Ankunftszeit")))));
        rowData.put("Abfahrtszeit",getTimeString(getDepartureTime(Long.parseLong(rowData.get("old")),Long.parseLong(rowData.get("Verweilzeit")))));
        rowData.put("Request_time",getTimeString(getRequestTime(Long.parseLong(rowData.get("old")),Long.parseLong(rowData.get("Entscheidungsdauer")))));
        rowData.remove("old");
        rowData.remove("Verweilzeit");
        rowData.remove("Entscheidungsdauer");
        return rowData;
    }


    public static String getTimeString(LocalTime time){
        return time.format(GeneralManager.timeFormatter);
    }


    public static LocalTime calculateTimeOutOfMinutes(long minutes){
        return LocalTime.of((int) Math.floor(minutes /60.0), (int) (minutes%60.0),0);
    }


    public static LocalTime getDepartureTime(long arrivalTime, long timeStayed){
        //=WENN([@Ankunftszeit]+[@Verweilzeit]<1440;[@Ankunftszeit]+[@Verweilzeit];([@Ankunftszeit]+[@Verweilzeit])-1440)/60/24
        long departureTime = arrivalTime + timeStayed < 1440 ? arrivalTime + timeStayed : (arrivalTime + timeStayed)-1440;
        return calculateTimeOutOfMinutes(departureTime);
    }


    public static LocalTime getRequestTime(long arrivalTime, long decisionTime){
        //=WENN([@Ankunftszeit]-[@Entscheidungsdauer]>=0;[@Ankunftszeit]-[@Entscheidungsdauer];1440-(([@Ankunftszeit]-[@Entscheidungsdauer])*(-1)))/60/24
        long requestTime = arrivalTime - decisionTime >= 0 ? arrivalTime - decisionTime : 1440 - (Math.abs(arrivalTime - decisionTime));
        return calculateTimeOutOfMinutes(requestTime);
    }


    public static void printNewData(String resultFilePath, List<Map<String, String>> dataLines){
        File csvOutputFile = new File(resultFilePath);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            pw.println("Ankunftszeit;Abfahrtszeit;Request_time");
            for(Map<String,String> data : dataLines){
                pw.println(data.get("Ankunftszeit")+";"+data.get("Abfahrtszeit")+";"+data.get("Request_time"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
