package de.haug.gral;

import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {
        CommandLine commandLine;
        Option optionCheckpoints = new Option("c", "checkpoints", false,
                "Use checkpointing for mobile node encounters");

        Option optionRectification = new Option("r", "pathRectification", false,
                "Use graph-based path rectification");

        Option optionFile = new Option("f", "file", true,
                "File containing package JSONs");

        Option optionExample = new Option(null, "example", false,
                "Populate with an example environment graph for demos");

        Option optionHelp = new Option(null, "help", false,
                "Print this message and quit");

        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        options.addOption(optionCheckpoints);
        options.addOption(optionRectification);
        options.addOption(optionFile);
        options.addOption(optionExample);
        options.addOption(optionHelp);

        HelpFormatter formatter = new HelpFormatter();

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelpMessage(formatter, options, 1);
            return;
        }

        if (commandLine.hasOption(optionHelp.getLongOpt())) {
            printHelpMessage(formatter, options, 0);
            return;
        }

        Locator l;
        List<String> argsList = commandLine.getArgList();

        if (commandLine.hasOption(optionExample.getLongOpt())) {
            l = new Locator(commandLine.hasOption(optionCheckpoints.getOpt()),
                    commandLine.hasOption(optionRectification.getOpt()));
        } else if (argsList.size() > 0) {
            TopologyAnalyzer t = new TopologyAnalyzer();
            AtomicBoolean isArray = new AtomicBoolean(false);
            List<JSONObject> objects = new LinkedList<>();
            try (Stream<String> stream = Files.lines(Paths.get(argsList.get(0)))) {
                AtomicBoolean firstObject = new AtomicBoolean(false);
                stream.forEach(line -> {
                    try {
                        if (Pattern.matches("^\\s*\\{.*", line)) {
                            firstObject.set(true);
                        } else if (Pattern.matches("^\\s*\\[", line)) {
                            isArray.set(true);
                        } else if (!isArray.get()) {
                            System.err.printf("Skipping invalid line %s\n", line);
                            return;
                        }

                        if (isArray.get()) return;

                        objects.add(new JSONObject(line));
                    } catch (JSONException e) {
                        System.err.printf("Ignoring malformed line %s\n", line);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(3);
                return;
            }

            if (isArray.get()) {
                String fileContent;
                try {
                    File file = new File(argsList.get(0));
                    FileInputStream fis = new FileInputStream(file);
                    byte[] data = new byte[(int) file.length()];
                    fis.read(data);
                    fis.close();
                    fileContent = new String(data);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(3);
                    return;
                }

                JSONArray fileArray = new JSONArray(fileContent);
                for (Object o : fileArray) {
                    if (!(o instanceof JSONObject)) {
                        System.err.println("Array contains non-object data. Ignoring.");
                        continue;
                    }

                    objects.add((JSONObject)o);
                }
            }

            Set<Long> relayIds = new HashSet<>();
            int successes = 0;
            for(JSONObject graphEdge : objects) {
                try {
                    long start = graphEdge.getLong("start");
                    long destination = graphEdge.getLong("destination");
                    float weight = graphEdge.getFloat("weight");
                    Float startRadius = null;
                    Float destRadius = null;

                    if (graphEdge.has("startRadius")) {
                        startRadius = graphEdge.getFloat("startRadius");
                    }

                    if (graphEdge.has("destinationRadius")) {
                        destRadius = graphEdge.getFloat("destinationRadius");
                    }

                    if (!relayIds.contains(start)) {
                        if (startRadius != null) {
                            t.addRelay(start, startRadius);
                        } else {
                            t.addRelay(start);
                        }
                        relayIds.add(start);
                    }
                    if (!relayIds.contains(destination)) {
                        if (destRadius != null) {
                            t.addRelay(destination, destRadius);
                        } else {
                            t.addRelay(destination);
                        }
                        relayIds.add(destination);
                    }
                    t.addEdge(start, destination, weight);
                    successes++;
                } catch (JSONException e) {
                    System.err.printf("Object %s does not contain all necessary properties. Skipping.\n",
                            graphEdge.toString());
                }
            }

            if (successes < 1) {
                System.err.println("Environment graph JSON contains no valid elements.");
                System.exit(4);
                return;
            } else if (!commandLine.hasOption(optionFile.getOpt())) {
                System.out.printf("Successfully loaded %d links into environment graph\n", successes);
            }

            l = new Locator(t, commandLine.hasOption(optionCheckpoints.getOpt()),
                    commandLine.hasOption(optionRectification.getOpt()));

        } else {
            System.err.println("Specify either a JSON file describing the environment graph or the --example flag");
            System.exit(2);
            return;
        }


        if (commandLine.hasOption(optionFile.getOpt())) {
            try (Stream<String> stream = Files.lines(Paths.get(commandLine.getOptionValue(optionFile.getOpt())))) {
                stream.forEach(line -> {
                    try {
                        parseJsonLine(line, l);
                    } catch (JSONException e) {
                        System.err.printf("Ignoring malformed line %s\n", line);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(3);
                return;
            }
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String userInput = "";
            System.out.println("Please enter your JSON packages (one package per line)");
            System.out.println("Example package: { \"deviceId\": 10, \"timestamp\": 50, \"contacts\": [{ \"deviceId\": 11, \"strength\": 0.7 }] }\n");
            while (true) {
                userInput = reader.readLine();
                if (!Pattern.matches("^\\s*\\{.*", userInput)) {
                    System.err.println("Please enter a valid JSON object");
                    continue;
                }
                while (!CliUtilities.areParenthesisBalanced(userInput)) {
                    System.out.print("... ");
                    userInput += reader.readLine();
                }
                try {
                    parseJsonLine(userInput, l);
                } catch (JSONException e) {
                    System.err.println("Ignoring malformed line");
                }
            }
        }
    }

    static void parseJsonLine(String line, Locator l) {
        JSONObject obj = new JSONObject(line);

        JSONArray contacts = obj.getJSONArray("contacts");
        long sid = 1L;
        try {
            sid = obj.getLong("deviceId");
        } catch (JSONException e) {
            // Var stays with its initial value
        }

        Package p;
        if (contacts.length() > 0) {
            HashSet<WirelessContact> contactSet = new HashSet<>();
            for (int i = 0; i < contacts.length(); i += 2) {
                JSONObject jsonContact = contacts.getJSONObject(i);
                contactSet.add(new WirelessContact(jsonContact.getInt("deviceId"), jsonContact.getFloat("strength")));
            }

            p = new Package(sid, obj.getLong("timestamp"), contactSet);
        } else {
            p = new Package(sid, obj.getLong("timestamp"));
        }

        List<Package> result = l.feed(p);

        for (Package r : result) {
            System.out.print(r.toJsonString());
        }
    }

    static void printHelpMessage(HelpFormatter formatter, Options options, int status) {
        formatter.printHelp("gral envgraph-json [options]", options);
        System.out.println(" envgraph-json             " +
                "JSON file specifying the environment graph in the arguments.");

        System.exit(status);
    }
}
