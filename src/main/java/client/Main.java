package client;

import com.beust.jcommander.JCommander;
import com.google.gson.*;
import common.Args;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mack_TB
 * @version 1.0.7
 * @since 12/27/2020
 */

/**
 * To launch this class, provide some arguments
 * for instance : -t set -k 1 -v "Hello Mack!
 * or to test with a json file : -in setFile.json
 */

public class Main {

    public static void main(String[] argv) {
        String address = "127.0.0.1";
        int port = 8888;

        try (Socket socket = new Socket(InetAddress.getByName(address), port);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("Client started!");

            Args args = new Args();
            JCommander.newBuilder()
                    .addObject(args)
                    .build()
                    .parse(argv);
            Map<String, Object> commands = new HashMap<>();
            Gson gson = new Gson(); // Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if (args == null) {
                System.out.println("Please, enter the arguments in command line");
                return;
            }

            if (args.getFileName() != null) {
                File file = new File("src/main/java/client/data/" + args.getFileName());
//                File file = new File("src/client/data/" + args.getFileName());
                try (Reader reader = new BufferedReader(new FileReader(file))) {
//                    args = gson.fromJson(reader, Args.class);
                    JsonElement tree = JsonParser.parseReader(reader);
                    JsonObject object = tree.getAsJsonObject();
                    args = new Args(object.get("type").getAsString(), object.get("key"));
                    if (object.get("value") != null) {
                        if (object.get("value").isJsonObject()) {
                            args.setValue(object.get("value").getAsJsonObject());
                        } else {
                            args.setValue(object.get("value").getAsString());
                        }
                    }
                }
            } else {
                JsonObject object = gson.toJsonTree(args).getAsJsonObject();
                if (object.get("key") != null) {
                    args.setKey(object.get("key").getAsString());
                    if (object.get("value") != null) {
                        args.setValue(object.get("value").getAsString());
                    }
                }
            }
            switch (args.getType()) {
                case "exit":
                    commands.put("type", args.getType());
                    break;
                case "set":
                    commands.put("type", args.getType());
                    commands.put("key", args.getKey());
                    commands.put("value", args.getValue());
                    break;
                default:
                    commands.put("type", args.getType());
                    commands.put("key", args.getKey());
                    break;
            }

            String outputJson = gson.toJson(commands);

            output.writeUTF(outputJson);
            System.out.printf("Sent: %s\n", outputJson);

            String receivedMsg = input.readUTF();
            System.out.printf("Received: %s\n", receivedMsg);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
