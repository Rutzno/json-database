package server;

import com.google.gson.*;
import common.Args;
import common.ArgsModel;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Mack_TB
 * @version 1.0.7
 * @since 12/27/2020
 */

public class Main {

    private static final int CELL_NUMBERS = 1000;
//    private static Map<String, Object> dbMap = new HashMap<>();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static OutputStream outputStream;
    public static Reader reader;
    private static final File file = new File("src/main/java/server/data/db.json");     //System.getProperty("user.dir") + "/
//    private static final File file = new File("./src/server/data/db.json");     //System.getProperty("user.dir") + "/

    private static Lock readLock;
    private static Lock writeLock;

    public static void main(String[] args) {

        String address = "127.0.0.1";
        int port = 8888;

        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address))) {

            System.out.println("Server started!");

            ExecutorService executor = Executors.newFixedThreadPool(4);
            ReadWriteLock lock = new ReentrantReadWriteLock();
            readLock = lock.readLock();
            writeLock = lock.writeLock();

            while (true) {
                Socket socket = server.accept();
                Session session = new Session(socket); // This thread will handle the client separately
                Future<Boolean> future = executor.submit(session);
                if (future.get()) {
                    executor.shutdown();
                    break;
                }
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static String handleDatabaseV2(Args command) throws IOException {
        Map<String, Object> result = new TreeMap<>();
        switch (command.getType()) {
            case "get":
                get(result, command);
                break;

            case "set":
                set(result, command);
                break;

            case "delete":
                delete(result, command);
                break;

            default:
                result.put("response", "ERROR");
                result.put("reason", "No such type");
        }

        return gson.toJson(result);
    }

    private static void get(Map<String, Object> result, Args command) throws IOException {
        String key = command.getKey().toString().replaceAll("[\\[\\]\"]", "");
        String[] keys = key.split(",\\s*");
        readLock.lock();
        reader = new BufferedReader(new FileReader(file));
//        dbMap = gson.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
        JsonElement tree = JsonParser.parseReader(reader);
        reader.close();
        readLock.unlock();

        JsonObject jsonObject = tree.getAsJsonObject();
        for (String k : keys) {
            if (jsonObject.get(k).isJsonObject()) {
                if (k.equals(keys[keys.length - 1])) {
                    result.put("response", "OK");
                    result.put("value", jsonObject.get(k).getAsJsonObject());
                    return;
                }
                jsonObject = jsonObject.get(k).getAsJsonObject();
            } else if (k.equals(keys[keys.length - 1]) && jsonObject.get(k) != null) {
                result.put("response", "OK");
                result.put("value", jsonObject.get(k).getAsString());
                return;
            }
        }
        result.put("response", "ERROR");
        result.put("reason", "No such key");
    }

    private static void set(Map<String, Object> result, Args command) throws IOException {
        String key = command.getKey().toString().replaceAll("[\\[\\]\"]", "");
        String[] keys = key.split(",\\s*");
        readLock.lock();
        reader = new BufferedReader(new FileReader(file));
        JsonElement tree = JsonParser.parseReader(reader);
        reader.close();
        readLock.unlock();

        JsonObject jsonObject = tree.getAsJsonObject();
        for (String k : keys) {
            if (jsonObject.get(k) == null) { // create
                if (command.getValue() instanceof String) {
                    jsonObject.addProperty(k, command.getValue().toString());
                } else {
                    jsonObject.add(k, gson.toJsonTree(command.getValue()));
                }
            } else { // update
                if (jsonObject.get(k).isJsonObject()) {
                    if (k.equals(keys[keys.length - 1])) {
                        jsonObject.add(k, gson.toJsonTree(command.getValue()));
                        break;
                    }
                    jsonObject = jsonObject.get(k).getAsJsonObject();
                } else if (k.equals(keys[keys.length - 1])) {
                    jsonObject.addProperty(k, command.getValue().toString());
                    break;
                }
            }
        }
//        dbMap.put(command.getKey(), command.getValue());
        writeLock.lock();
        Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
        new GsonBuilder().setPrettyPrinting().create().toJson(tree, writer);
        writer.close();
        writeLock.unlock();
        result.put("response", "OK");
    }

    private static void delete(Map<String, Object> result, Args command) throws IOException {
        String key = command.getKey().toString().replaceAll("[\\[\\]\"]", "");
        String[] keys = key.split(",\\s*");
        readLock.lock();
        reader = new BufferedReader(new FileReader(file));
        JsonElement tree = JsonParser.parseReader(reader);
        reader.close();
        readLock.unlock();
        JsonObject jsonObject = tree.getAsJsonObject();
        boolean removed = false;
        for (String k : keys) {
            if (k.equals(keys[keys.length - 1]) && jsonObject.get(k) != null) {
                jsonObject.remove(k);
                removed = true;
                break;
            }
            if (jsonObject.get(k).isJsonObject()) {
                jsonObject = jsonObject.get(k).getAsJsonObject();
            }
        }
        if (removed) {
            writeLock.lock();
            Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
            new GsonBuilder().setPrettyPrinting().create().toJson(tree, writer);
            writer.close();
            writeLock.unlock();
            result.put("response", "OK");
        } else {
            result.put("response", "ERROR");
            result.put("reason", "No such key");
        }
    }


    public static String handleDatabase(String jsonString) throws IOException {
        Map<String, Object> result = new HashMap<>();
        Gson gson = new Gson();
        Args command = gson.fromJson(jsonString, Args.class);
        boolean found = false;

        switch (command.getType()) {
            case "get":
                readLock.lock();
                reader = new BufferedReader(new FileReader(file));
                ArgsModel argsModel = gson.fromJson(reader, ArgsModel.class);
                    /*List<Args> objects = gson.fromJson(reader,
                            new TypeToken<List<Args>>(){}.getType());*/
                readLock.unlock();
                if (argsModel.getObjects() != null) {
                    for (Args args : argsModel.getObjects()) {
                        if (args.getKey().equals(command.getKey())) {
                            found = true;
                            result.put("response", "OK");
                            result.put("value", args.getValue());
                            break;
                        }
                    }
                }
                if (!found) {
                    result.put("reason", "No such key");
                    result.put("response", "ERROR");
                }
                break;

            case "set":
                readLock.lock();
                reader = new BufferedReader(new FileReader(file));
                argsModel = gson.fromJson(reader, ArgsModel.class);
                readLock.unlock();
                command.setType(null);
                if (argsModel.getObjects() != null) {
                    for (Args args : argsModel.getObjects()) {
                        if (args.getKey().equals(command.getKey()) &&
                                !args.getValue().equals(command.getValue())) {
                            found = true;
                            args.setValue(command.getValue());
                            writeLock.lock();
                            outputStream = new FileOutputStream(file);
                            outputStream.write(gson.toJson(argsModel).getBytes());
                            outputStream.flush();
                            writeLock.unlock();
                            break;
                        } else if (args.getKey().equals(command.getKey())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    if (argsModel.getObjects() == null) argsModel.setObjects(new ArrayList<>());
                    argsModel.getObjects().add(command);
                    writeLock.lock();
                    outputStream = new FileOutputStream(file);
                    outputStream.write(gson.toJson(argsModel).getBytes());
                    outputStream.flush();
                    writeLock.unlock();
                }
                result.put("response", "OK");
                break;

            case "delete":
//                synchronized (Main.class) {
                readLock.lock();
                reader = new BufferedReader(new FileReader(file));
                argsModel = gson.fromJson(reader, ArgsModel.class);
                readLock.unlock();
                // modify the json object
                for (Args object : argsModel.getObjects()) {
                    if (object.getKey().equals(command.getKey())) {
                        found = true;
//                        database.remove(command.getKey());
                        argsModel.getObjects().remove(object);
                        writeLock.lock();
                        outputStream = new FileOutputStream(file);
                        outputStream.write(gson.toJson(argsModel).getBytes());
                        outputStream.flush();
                        writeLock.unlock();
                        result.put("response", "OK");
                        break;
                    }
                }

                if (!found) {
                    result.put("response", "ERROR");
                    result.put("reason", "No such key");
                }
//                }
                break;

 /*           case "exit":
                exit = true;
                result.put("response", "OK");
                break;*/

            default:
                result.put("response", "ERROR");
                result.put("reason", "No such type");
                break;
        }

        return gson.toJson(result);
    }

    public static boolean isValidCell(int number) {
        return number >= 0 && number < CELL_NUMBERS;
    }
}
