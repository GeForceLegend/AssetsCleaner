package none.geforcelegend.assetcleaner;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.URLDecoder;
import java.util.Map;
import java.util.TreeSet;

public class Main {
    public static void main(String[] args) throws Exception {
        String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        jarPath = URLDecoder.decode(jarPath, "UTF-8");
        jarPath = new File(jarPath).getParent();

        if (!new File(jarPath).getName().equals(".minecraft")) {
            System.out.println("Curr folder is " + new File(jarPath).getName());
            throw new FileNotFoundException("minecraft files not found, put this jar in .minecraft");
        }
        Gson gson = new Gson();

        /*===================================== Parsing version files ======================================*/
        String versionPath = jarPath + File.separator + "versions";

        TreeSet<String> indexes = new TreeSet<String>();
        TreeSet<String> libraries = new TreeSet<String>();
        for (File versionName : new File(versionPath).listFiles()) {
            if (versionName.isFile()) {
                continue;
            }
            String version = versionName.getName();
            FileReader versionDataReader = new FileReader(new File(versionPath + File.separator + version + File.separator + version + ".json"));
            JsonObject versionData = gson.fromJson(versionDataReader, JsonObject.class);
            System.out.println("Scanning version file " + version + "...");
            if (versionData.get("assetIndex") != null) {
                String index = versionData.get("assetIndex").getAsJsonObject().get("id").getAsString();
                indexes.add(index + ".json");
                System.out.println("Added index " + index + " to index list");
            }
            for (JsonElement jsonElement : versionData.get("libraries").getAsJsonArray()) {
                JsonObject arrayObject = jsonElement.getAsJsonObject();
                if (arrayObject.get("name") != null) {
                    String rawLibraryPath = arrayObject.get("name").getAsString();
                    String[] libraryData = rawLibraryPath.split(":");
                    String library = libraryData[0].replace(".", "/") + "/" + libraryData[1] + "/" + libraryData[2] + "/" + libraryData[1] + "-" + libraryData[2];
                    if (libraryData.length > 3) {
                        library += "-" + libraryData[3];
                    }
                    library += ".jar";
                    libraries.add(library);
                }
                else if (arrayObject.get("downloads") != null) {
                    JsonObject downloads = arrayObject.get("downloads").getAsJsonObject();
                    JsonObject artifact = downloads.get("artifact").getAsJsonObject();
                    String library = artifact.get("path").getAsString();
                    libraries.add(library);
                }
            }
            versionDataReader.close();
        }


        /*=================================== Cleaning .minecraft/assets ===================================*/
        String assetsPath = jarPath + File.separator + "assets";

        TreeSet<String> assets = new TreeSet<String>();
        File indexesPath = new File(assetsPath + File.separator + "indexes");
        File[] indexFiles = indexesPath.listFiles();
        for (File index : indexFiles) {
            if (indexes.contains(index.getName())) {
                FileReader indexReader = new FileReader(index);
                JsonObject indexData = gson.fromJson(indexReader, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : indexData.getAsJsonObject("objects").entrySet()) {
                    String hash = entry.getValue().getAsJsonObject().get("hash").getAsString();
                    assets.add(hash);
                }
                indexReader.close();
            }
        }

        File objectsPath = new File(assetsPath + File.separator + "objects");
        for (File objectsSubPath : objectsPath.listFiles()) {
            for (File object : objectsSubPath.listFiles()) {
                if (!assets.contains(object.getName())) {
                    object.delete();
                    System.out.println("Deleted file assets/objects/" + object.getParent() + "/" + object.getName());
                }
            }
        }

        /*================================= Cleaning .minecraft/libraries ==================================*/
        File librariesPath = new File(jarPath + File.separator + "libraries");
        for (File file : librariesPath.listFiles()) {
            cleanLibraries(file, file.getName(), libraries);
        }
    }
    public static void cleanLibraries(File path, String currPath, TreeSet<String> libraries) {
        for (File file : path.listFiles()) {
            String libraryPath = currPath + "/" + file.getName();
            if (file.isFile()) {
                if (!libraries.contains(libraryPath)) {
                    file.delete();
                    System.out.println("Deleted file libraries/" + libraryPath);
                }
            }
            else {
                cleanLibraries(file, libraryPath, libraries);
            }
        }
    }
}
