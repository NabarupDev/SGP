package com.nabarup.college;

import org.json.JSONException;
import org.json.JSONObject;

public class UploadedFile {
    private String url;
    private String fileType;
    private String fileName; // Added fileName field

    public UploadedFile(String url, String fileType, String fileName) {
        this.url = url;
        this.fileType = fileType;
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", url);
        jsonObject.put("fileType", fileType);
        jsonObject.put("fileName", fileName); // Serialize fileName
        return jsonObject;
    }

    public static UploadedFile fromJson(JSONObject jsonObject) throws JSONException {
        String url = jsonObject.getString("url");
        String fileType = jsonObject.getString("fileType");
        String fileName = jsonObject.getString("fileName"); // Deserialize fileName
        return new UploadedFile(url, fileType, fileName);
    }
}
