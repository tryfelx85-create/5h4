package com.arena.spawn;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Appends timestamped lines to files in the plugin folder (combat.log, anticheat.log). */
public class FileLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static File dir;

    public static void init(File dataFolder) {
        dir = dataFolder;
        dir.mkdirs();
    }

    public static void write(String fileName, String line) {
        if (dir == null) return;
        try {
            Files.writeString(new File(dir, fileName).toPath(),
                    LocalDateTime.now().format(TIME) + " " + line + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // logging must never break gameplay
        }
    }
}
