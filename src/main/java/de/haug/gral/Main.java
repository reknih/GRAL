package de.haug.gral;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please supply at least one argument");
            return;
        }

        Locator l = new Locator();

	    try (Stream<String> stream = Files.lines(Paths.get(args[0]))) {
	        stream.forEach(line -> {
	            if (line.charAt(0) != '{') return;
                JSONObject obj = new JSONObject(line);
                if (!obj.getString("sensor").equals("beacon")) return;

                JSONArray contacts = obj.getJSONArray("value");
                long sid = 1L;
                try {
                    sid = obj.getLong("id");
                } catch (JSONException e) {
                    // Var stays with its initial value
                }

                Package p;
                if (contacts.length() >= 2) {
                    HashSet<WirelessContact> contactSet = new HashSet<>();
                    for (int i = 0; i < contacts.length(); i += 2) {
                        contactSet.add(new WirelessContact(contacts.getLong(i), contacts.getFloat(i + 1)));
                    }

                    p = new Package(sid, obj.getLong("time"), contactSet);
                } else {
                    p = new Package(sid, obj.getLong("time"));
                }



                List<Package> result = l.feed(p);

                for (Package r : result) {
                    System.out.printf(Locale.US, "{\"estimated_position\": %f, \"id\": %d, \"timestamp\": %d}\n",
                            l.topologyAnalyzer.getTotalRoutePosition(r.getPosition(),
                                    l.topologyAnalyzer.getRelay(args.length >= 2 && sid % 2 != 0 ? 1004 : 1001),
                                    l.topologyAnalyzer.getRelay(1003)).getPositionInBetween(),
                            sid, r.getTimestamp());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
