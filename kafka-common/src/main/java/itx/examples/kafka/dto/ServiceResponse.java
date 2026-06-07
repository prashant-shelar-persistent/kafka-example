package itx.examples.kafka.dto;

public record ServiceResponse(String taskId, String clientId, String data, String response) {
}
