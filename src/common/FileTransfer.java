package src.common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileTransfer {
    public static byte[] fileToBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }
    
    public static boolean saveFile(byte[] data, String fileName, String directory) {
        try {
            // Create directory if it doesn't exist
            Files.createDirectories(Paths.get(directory));
            
            // Write the file
            try (FileOutputStream fos = new FileOutputStream(directory + File.separator + fileName)) {
                fos.write(data);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}