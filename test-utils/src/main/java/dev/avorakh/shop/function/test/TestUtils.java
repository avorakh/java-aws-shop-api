package dev.avorakh.shop.function.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {
    public String readFile(ClassLoader classLoader, String file) throws IOException {
        String path = classLoader.getResource(file).getPath();
        return Files.readString(Paths.get(path));
    }
}
