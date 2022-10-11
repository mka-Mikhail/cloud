package com.mka.common;

public enum Command {
    SEND_FILE_COMMAND("file");
    private final String command;

    Command(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
