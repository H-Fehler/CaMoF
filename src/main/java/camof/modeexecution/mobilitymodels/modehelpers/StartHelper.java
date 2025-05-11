package camof.modeexecution.mobilitymodels.modehelpers;

import camof.modeexecution.ModeExecutionManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class StartHelper {

    public static Map<String,Object> readInConfigForModes(JSONObject config, JSONObject modejson, String[] modes){
        Map<String,Object> configValues = new HashMap<>();
        if(modejson.has("global") && modejson.get("global") instanceof JSONObject && modejson.getJSONObject("global").has("required")){
            JSONArray requiredModeAttributeArray = modejson.getJSONObject("global").getJSONArray("required");
            String[] requiredModeAttributes = new String[requiredModeAttributeArray.length()];
            for(int i=0; i<requiredModeAttributeArray.length(); i++){
                if(requiredModeAttributeArray.get(i) instanceof String){
                    requiredModeAttributes[i] = (String) requiredModeAttributeArray.get(i);
                }else throw new RuntimeException("'modes' parameter not a list of Strings.");
            }

            if(modejson.getJSONObject("global").has("optional")){
                JSONArray optionalModeAttributeArray = modejson.getJSONObject("global").getJSONArray("optional");
                String[] optionalModeAttributes = new String[optionalModeAttributeArray.length()];
                for(int i=0; i<optionalModeAttributeArray.length(); i++){
                    if(optionalModeAttributeArray.get(i) instanceof String){
                        optionalModeAttributes[i] = (String) optionalModeAttributeArray.get(i);
                    }else throw new RuntimeException("'modes' parameter not a list of Strings.");
                }
            }

            for(String mode : modes){
                checkThatModeIsFine(modejson,mode,requiredModeAttributes);
                readInConfigDataForMode(configValues,config,modejson,mode);
            }

            return configValues;

        }else throw new RuntimeException("object 'global' missing or attribute 'required' of 'global' missing.");
    }


    public static void checkThatModeIsFine(JSONObject modejson, String modeName, String[] requiredModeAttributes){
        if(modejson.has(modeName) && modejson.get(modeName) instanceof JSONObject){
            JSONObject modeObject = modejson.getJSONObject(modeName);
            ModeExecutionManager.modeValues.put(modeName,modeObject);
            for(String required : requiredModeAttributes){
                if(!modeObject.keySet().contains(required)){
                    throw new RuntimeException("mode lacks required attribute '" + required + "'.");
                }
                if(required.equals("resultsFolder") && !new File(modeObject.getString("resultsFolder")).isDirectory()){
                    throw new RuntimeException("the result folder of mode " + modeName + " does not exist.");
                }
                if(required.equals("comparing") && modeObject.getBoolean("comparing")){
                    if(!modeObject.keySet().contains("compare to")){
                        throw new RuntimeException("mode is comparing but lacks attribute 'compare to'.");
                    }
                }
            }
        }else throw new RuntimeException("mode with name " + modeName + " is missing or not a valid JSONObject.");
    }


    public static void readInConfigDataForMode(Map<String,Object> configValues, JSONObject config, JSONObject modejson, String modeName){
        JSONObject modeObject = modejson.getJSONObject(modeName);
        if(modeObject.getBoolean("comparing")){
            ModeExecutionManager.compareModes.put(modeName,modeObject.getString("compare to"));
        }
        if(!modeObject.has("config")){
            throw new RuntimeException("mode with name " + modeName + "lacks attribute 'config'.");
        }
        JSONObject keyMeta = modeObject.getJSONObject("config");
        for(String key : keyMeta.keySet()){
            keyMeta.get(key);
            if(config.has(key)){
                assignParameter(keyMeta,key,config,configValues);
            }else throw new RuntimeException("parameter '" + key + "' is missing in config json, needed for mode " + modeName + ".");
        }
    }


    public static void assignParameter(JSONObject keyMeta, String key, JSONObject config, Map<String,Object> configValues){
        if(keyMeta.get(key) instanceof String){
            String type = keyMeta.getString(key);
            try{
                switch (type) {
                    case "String" : configValues.put(key,config.getString(key)); break;
                    case "boolean" : configValues.put(key,config.getBoolean(key)); break;
                    case "integer" : configValues.put(key,config.getInt(key)); break;
                    case "long" : configValues.put(key,config.getBigDecimal(key).longValue()); break;
                    case "double" : configValues.put(key,config.getBigDecimal(key).doubleValue()); break;
                    case "float" : configValues.put(key,config.getBigDecimal(key).floatValue()); break;
                    case "JSONObject" : configValues.put(key,config.getJSONObject(key)); break;
                    case "JSONArray" : configValues.put(key,config.getJSONArray(key)); break;
                    default: throw new RuntimeException("The config parameter " + key + " meta data in modes json should be one of ['String','integer','long','double','float','JSONObject','JSONArray'].");
                }
            }catch (Exception e){
                throw new RuntimeException("The config parameter " + key + " in config json is not correctly set. Should be of type " + type + ".");
            }
        }else{
            throw new RuntimeException("The config parameter " + key + " meta data in modes json should be a String with one of ['String','integer','long','double','float','JSONObject','JSONArray'].");
        }
    }


}
