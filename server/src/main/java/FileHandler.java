import java.io.*;
import java.net.Socket;

public class FileHandler implements Runnable {

    private static final String SERVER_DIR = "server_file";
    private static final String currentDirectory = SERVER_DIR;
    private static final String SEND_FILE_COMMAND = "file";
    private static final String FILES_ON_SERVER_COMMAND = "files on server";
    private static final Integer BATCH_SIZE = 256;

    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;

    private byte[] batch;

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
            sendFileNames(currentDirectory);
            while (true) {
                String command = dis.readUTF();
                if (command.equals(SEND_FILE_COMMAND)) {
                    String fileName = dis.readUTF();
                    long size = dis.readLong();
                    try (FileOutputStream fos = new FileOutputStream(SERVER_DIR + "/" + fileName)) {
                        for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                            int read = dis.read(batch);
                            fos.write(batch, 0, read);
                        }
                    } catch (Exception ignored) {}
                    sendFileNames(currentDirectory);
                } else {
                    System.out.println("Unknown command received: " + command);
                }
            }
        } catch (Exception ignored) {
            System.out.println("Client disconnected...");
        }
    }

    private void sendFileNames(String directory) throws IOException {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                StringBuilder files = new StringBuilder();
                files.append(FILES_ON_SERVER_COMMAND + "\n");
                for (String file : list) {
                    files.append(file + "\n");
                }
                dos.writeUTF(files.toString());
            }
        }
    }
}

