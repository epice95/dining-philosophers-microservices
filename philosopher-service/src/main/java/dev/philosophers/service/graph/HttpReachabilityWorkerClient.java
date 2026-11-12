package dev.philosophers.service.graph;

import dev.philosophers.core.graph.Edge;
import dev.philosophers.core.graph.ReachabilityWorkerClient;
import dev.philosophers.core.graph.WorkerException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Appelle un Worker par HTTP. Toute erreur réseau ou HTTP devient une {@link WorkerException}. */
public final class HttpReachabilityWorkerClient implements ReachabilityWorkerClient {

    private final String name;
    private final RestClient client;

    public HttpReachabilityWorkerClient(
            String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        this.name = baseUrl;
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void load(String jobId, List<Edge> edges) throws WorkerException {
        try {
            client.post()
                    .uri("/graph/reach/{jobId}/load", jobId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReachabilityWorkerController.LoadRequest(edges))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new WorkerException("Worker " + name + " failed to load: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Integer> expand(String jobId, List<Integer> nodes) throws WorkerException {
        try {
            ReachabilityWorkerController.ExpandResponse response =
                    client.post()
                            .uri("/graph/reach/{jobId}/expand", jobId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new ReachabilityWorkerController.ExpandRequest(nodes))
                            .retrieve()
                            .body(ReachabilityWorkerController.ExpandResponse.class);
            if (response == null) {
                throw new WorkerException("Empty response from " + name);
            }
            return response.neighbors();
        } catch (RestClientException e) {
            throw new WorkerException("Worker " + name + " failed to expand: " + e.getMessage(), e);
        }
    }

    @Override
    public void release(String jobId) throws WorkerException {
        try {
            client.delete().uri("/graph/reach/{jobId}", jobId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new WorkerException(
                    "Worker " + name + " failed to release: " + e.getMessage(), e);
        }
    }
}
