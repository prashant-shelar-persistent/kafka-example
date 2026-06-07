package itx.examples.kafka.dto;

public record ServiceRequest(String taskId, String clientId, String data) {
}
