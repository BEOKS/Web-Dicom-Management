package com.knuipalab.dsmp.machineLearning.MachineLearningInference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knuipalab.dsmp.storage.FileSystemStorageService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;

@Primary
@Service
public class BasicMalignancyServerMessenger implements MalignancyServerMessenger {
    private final String URL = "http://host.docker.internal:8098/predictions/";
    private final String TORCHSERVE_MANAGEMENT_URL="http://host.docker.internal:8099/models";
    String charset = "UTF-8";
    private FileSystemStorageService fileSystemStorageService;

    public BasicMalignancyServerMessenger(FileSystemStorageService fileSystemStorageService) {
        this.fileSystemStorageService = fileSystemStorageService;
    }

    @Override
    public JsonNode requestMalignancyInference(String projectId, String imageName,String modelName) {
        try {
            File imageFile = this.fileSystemStorageService.serveFile(projectId, imageName);
            HttpResponse httpResponse = postImageToServer(URL+modelName, imageFile);
            String json = EntityUtils.toString(httpResponse.getEntity());
            JsonNode jsonNode = parseJson(json);
            storeImageFile(projectId, addMiddleExtension(imageName, ".crop"), jsonNode.get("crop").asText());
            storeImageFile(projectId, addMiddleExtension(imageName, ".cam"), jsonNode.get("cam").asText());
            ((ObjectNode) jsonNode).remove("crop");
            ((ObjectNode) jsonNode).remove("cam");
            return jsonNode;
        } catch (FileNotFoundException e) {
            String strJsonNode = "{\n" +
                    "   \"classification1\": \"none\",\n" +
                    "    \"classification2\": \"none\"\n" +
                    "  }";
            return parseJson(strJsonNode);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public ResponseEntity<String> getRunningModelList(){
        RestTemplate restTemplate=new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(TORCHSERVE_MANAGEMENT_URL,String.class);
        return responseEntity;
    }
    @Override
    public boolean isServerAvailable() {
        return false;
    }

    public HttpResponse sendRequest(File file) {

        return null;
    }

    /**
     * 파일을 지정된 url에 post형식으로 전달하고 수행 결과를 반환합니다.
     * @param url
     * @param file
     * @return
     */
    private HttpResponse postImageToServer(String url, File file) {
        HttpEntity entity = MultipartEntityBuilder.create()
                .addPart("data", new FileBody(file))
                .build();
        HttpPost request = new HttpPost(url);
        request.setEntity(entity);
        HttpClient client = HttpClientBuilder.create().build();
        try {
            return client.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * String으로 반환된 json 결과를 JsonNode로 파싱합니다.
     * @param json
     * @return
     */
    private JsonNode parseJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(json);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonNode;
    }

    private void storeImageFile(String projectId, String imageName, String base64string) {
        byte[] imageBytes = Base64.getDecoder().decode(base64string);
        fileSystemStorageService.uploadFile(projectId, imageName, imageBytes);

    }

    private String addMiddleExtension(String fileName, String extensionName) {
        int index = fileName.indexOf('.');
        return fileName.substring(0, index) + extensionName + fileName.substring(index);
    }
}
