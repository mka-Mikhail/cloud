package com.mka.client;

import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

public class CloudMainController implements Initializable {
    private DataInputStream dis;
    private DataOutputStream dos;

    private static final Integer BATCH_SIZE = 256;
    public ListView<String> clientView;
    public ListView<String> serverView;
    private String currentDirectory;
    private byte[] batch;

    private static final String SEND_COMMAND_SEND_FILE = "file";
    private static final String SEND_COMMAND_DOWNLOAD_FILE = "download file";
    private static final String ACCEPT_COMMAND_FILES_ON_SERVER = "files on server";
    private static final String ACCEPT_COMMAND_HOLD_FILE_ = "hold file";

    private void initNetwork() {
        try {
            Socket socket = new Socket("localhost", 8189);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception ignored) {}
    }

    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, getFilesOnClient(currentDirectory));
    }

    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().addAll(data);
    }

    private void listenCommands() {
        new Thread(() -> {
            try {
                while (true) {
                    String command = dis.readUTF();
                    if (command.startsWith(ACCEPT_COMMAND_FILES_ON_SERVER)) {
                        String[] files = command.split("\n");
                        getFilesOnServer(files);
                    }
                    if (command.equals(ACCEPT_COMMAND_HOLD_FILE_)) {
                        acceptFileFromServer();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initNetwork();
        listenCommands();
        setCurrentDirectory(System.getProperty("user.home"));
        fillView(clientView, getFilesOnClient(currentDirectory));
        clientView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = clientView.getSelectionModel().getSelectedItem();
                File selectedFile = new File(currentDirectory + "/" + selected);
                if (selectedFile.isDirectory()) {
                    setCurrentDirectory(currentDirectory + "/" + selected);
                }
            }
        });
    }

    public void sendFileToServer() {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                dos.writeUTF(SEND_COMMAND_SEND_FILE);
                dos.writeUTF(fileName);
                dos.writeLong(file.length());
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bytes = fis.readAllBytes();
                    dos.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                System.out.println("e" + e.getMessage());
            }
        }
    }

    public void requestToDownloadFile() throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        dos.writeUTF(SEND_COMMAND_DOWNLOAD_FILE + "\n" + fileName);
    }

    private void acceptFileFromServer() throws IOException {
        String fileName = dis.readUTF();
        long size = dis.readLong();
        try (FileOutputStream fos = new FileOutputStream(System.getProperty("user.home") + "/Desktop/" + fileName)) {
            for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                int read = dis.read(batch);
                fos.write(batch, 0, read);
            }
        } catch (Exception ignored) {}
    }

    private void getFilesOnServer(String[] files) throws IOException {
        if (files != null) {
            List<String> listFilesOnServer = new ArrayList<>();
            listFilesOnServer.addAll(Arrays.asList(files));
            listFilesOnServer.set(0, "..");
            Platform.runLater(() -> {
                fillView(serverView, listFilesOnServer);
            });
        }
    }

    private List<String> getFilesOnClient(String directory) {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                List<String> files = new ArrayList<>(Arrays.asList(list));
                files.add(0, "..");
                return files;
            }
        }
        return List.of();
    }
}