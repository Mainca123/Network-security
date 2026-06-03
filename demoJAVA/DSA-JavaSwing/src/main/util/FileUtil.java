package main.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtil {
	public static byte[] readBytes(File file) throws IOException {
		return Files.readAllBytes(file.toPath());
	}

	public static void writeText(File file, String content) throws IOException {
		Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
	}

	public static String readText(File file) throws IOException {
		return Files.readString(file.toPath(), StandardCharsets.UTF_8);
	}
}
