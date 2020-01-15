package server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class JsonManager {
    /**
     * Restituisce l'oggetto JSON contenente gli users registrati
     *
     * @return
     */
    public static JSONArray getJSON(String name) {

        //JSONObject JSONUtenti;
        try/*(FileReader fr=new FileReader("banca.json"))*/ {
            FileChannel inChannel = FileChannel.open(Paths.get(name), StandardOpenOption.READ);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            String json = "";
            while (inChannel.read(buffer) > 0) {
                json += buffer.toString();
                buffer.clear();
            }
            return (JSONArray) (new JSONParser().parse(json));
            //System.out.println(JSONbanca);
        } catch (Exception e) {
            return null;
        }


    }
}
