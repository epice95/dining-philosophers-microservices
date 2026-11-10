package dev.philosophers.service.graph;

import dev.philosophers.core.graph.DegreeResult;
import dev.philosophers.core.graph.DegreeTask;
import dev.philosophers.core.graph.WorkerClient;
import dev.philosophers.core.graph.WorkerException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Appelle un Worker par HTTP. Toute erreur réseau ou HTTP devient une {@link WorkerException}. */
public final class HttpWorkerClient implements WorkerClient {

    private final String name;
    private final RestClient client;

    public HttpWorkerClient(String name, RestClient client) {
        this.name = name;
        this.client = client;
    }

    public static HttpWorkerClient create(
            String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return new HttpWorkerClient(
                baseUrl, RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public DegreeResult compute(DegreeTask task) throws WorkerException {
        try {
            DegreeResult result =
                    client.post()
                            .uri("/graph/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(task)
                            .retrieve()
                            .body(DegreeResult.class);
            if (result == null) {
                throw new WorkerException("Empty response from " + name);
            }
            return result;
        } catch (RestClientException e) {
            throw new WorkerException("Worker " + name + " failed: " + e.getMessage(), e);
        }
    }
}
