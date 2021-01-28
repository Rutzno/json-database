package server;

import com.google.gson.Gson;
import common.Args;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * @author Mack_TB
 * @version 1.0.7
 * @since 12/27/2020
 */

public class Session implements Callable<Boolean> {

    private final Socket socket;

    public Session(Socket socketForClient) {
        this.socket = socketForClient;
    }

    @Override
    public Boolean call() {
        try (DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            String receivedMsg = input.readUTF();
            Gson gson = new Gson();
            Args command = gson.fromJson(receivedMsg, Args.class);
            if ("exit".equals(command.getType())) {
                output.writeUTF("{\"response\":\"OK\"}");
                return true;
            } else {
                String msg = Main.handleDatabaseV2(command);
                output.writeUTF(msg);

                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
