package de.haug.sensor_location;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please supply at least one argument");
            return;
        }

        var l = new Locator();

	    try (Stream<String> stream = Files.lines(Paths.get(args[0]))) {
	        stream.forEach(line -> {
	            if (line.charAt(0) != '{') return;
                JSONObject obj = new JSONObject(line);
                if (!obj.getString("sensor").equals("beacon")) return;

                var contacts = obj.getJSONArray("value");
                Package p;
                if (contacts.length() >= 2) {
                    var contact = new WirelessContact(contacts.getLong(0), contacts.getFloat(1));
                    p = new Package(1, obj.getLong("time"), contact);
                } else {
                    p = new Package(1, obj.getLong("time"));
                }



                List<Package> result = l.feed(p);

                for (var r : result) {
                    System.out.printf(Locale.US, "{\"estimated_position\": %f, \"timestamp\": %d}\n",
                            r.getPosition().getPositionInBetween(), r.getTimestamp());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
