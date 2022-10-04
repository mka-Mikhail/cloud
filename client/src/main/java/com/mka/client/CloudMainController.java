package com.mka.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;

public class CloudMainController implements Initializable {
    public ListView<String> clientView;
    public ListView<String> serverView;
    private String currentDirectory;

    private DataInputStream dis;
    private DataOutputStream dos;

    private static final String SEND_FILE_COMMAND = "file";
    private static final String FILES_ON_SERVER_COMMAND = "files on server";


    public void sendToServer(ActionEvent actionEvent) {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                dos.writeUTF(SEND_FILE_COMMAND);
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

    private void initNetwork() {
        try {
            Socket socket = new Socket("localhost", 8189);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception ignored) {}
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initNetwork();
        listenCommands();
        setCurrentDirectory(System.getProperty("user.home"));
        fillView(clientView, getFiles(currentDirectory));
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

    private void listenCommands() {
        new Thread(() -> {
            try {
                while (true) {
                    String command = dis.readUTF();
                    if (command.startsWith(FILES_ON_SERVER_COMMAND)) {
                        String[] files = command.split("\n");
                        Platform.runLater(() -> {
                            try {
                                fillView(serverView, getFilesOnServer(files));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private List<String> getFilesOnServer(String[] files) throws IOException {
        if (files != null) {
            List<String> listFilesOnServer = new ArrayList<>();
            for (String file : files) {
                if (!file.equals(FILES_ON_SERVER_COMMAND)) {
                    listFilesOnServer.add(file);
                }
            }
            listFilesOnServer.add(0, "..");
            return listFilesOnServer;
        }
        return List.of();
    }

    private void setCurrentDirectory(String directory) {
        currentDirectory = directory;
        fillView(clientView, getFiles(currentDirectory));
    }

    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().addAll(data);
    }

    private List<String> getFiles(String directory) {
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