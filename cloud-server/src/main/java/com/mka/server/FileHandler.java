package com.mka.server;

import java.io.*;
import java.net.Socket;

public class FileHandler implements Runnable {

    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;

    private static final String SERVER_DIR = "server_file";
    private static final Integer BATCH_SIZE = 256;
    private static final String currentDirectory = SERVER_DIR;
    private byte[] batch;

    private static final String ACCEPT_COMMAND_SEND_FILE = "file";
    private static final String ACCEPT_COMMAND_DOWNLOAD_FILE = "download file";
    private static final String SEND_COMMAND_FILES_ON_SERVER = "files on server";
    private static final String SEND_COMMAND_HOLD_FILE = "hold file";

    public FileHandler(Socket socket) throws IOException {
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        batch = new byte[BATCH_SIZE];
        File file = new File(SERVER_DIR);
        if (!file.exists()) {
            file.mkdir();
        }
        System.out.println("Client accepted...");
    }

    @Override
    public void run() {
        try {
            System.out.println("Start listening...");
            sendFileNamesToClient(currentDirectory);
            while (true) {
                String command = dis.readUTF();
                if (command.equals(SEND_FILE_COMMAND.getCommand())) {
                    acceptFileFromClient();
                    sendFileNamesToClient(currentDirectory);
                } else if (command.startsWith(ACCEPT_COMMAND_DOWNLOAD_FILE)) {
                    String[] msg = command.split("\n");
                    String fileName = msg[1];
                    sendFileToClient(fileName);
                } else {
                    System.out.println("Unknown command received: " + command);
                }
            }
        } catch (Exception ignored) {
            System.out.println("Client disconnected...");
        }
    }

    private void acceptFileFromClient() throws IOException {
        String fileName = dis.readUTF();
        long size = dis.readLong();
        try (FileOutputStream fos = new FileOutputStream(SERVER_DIR + "/" + fileName)) {
            for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                int read = dis.read(batch);
                fos.write(batch, 0, read);
            }
        } catch (Exception ignored) {}
    }

    private void sendFileNamesToClient(String directory) throws IOException {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                StringBuilder files = new StringBuilder();
                files.append(SEND_COMMAND_FILES_ON_SERVER + "\n");
                for (String file : list) {
                    files.append(file + "\n");
                }
                dos.writeUTF(files.toString());
            }
        }
    }
    private void sendFileToClient(String fileName) throws IOException {
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            dos.writeUTF(SEND_COMMAND_HOLD_FILE);
            dos.writeUTF(fileName);
            dos.writeLong(file.length());
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] bytes = fis.readAllBytes();
                dos.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

