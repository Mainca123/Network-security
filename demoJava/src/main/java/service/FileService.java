package service;

import util.JsonUtil;

import java.io.File;
import java.io.IOException;

public class FileService {
    public <T> T readJson(File file, Class<T> type) throws IOException {
        return JsonUtil.read(file.toPath(), type);
    }

    public void writeJson(File file, Object value) throws IOException {
        JsonUtil.write(file.toPath(), value);
    }
}
