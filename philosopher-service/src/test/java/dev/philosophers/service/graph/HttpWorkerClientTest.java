package dev.philosophers.service.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.philosophers.core.graph.DegreeResult;
import dev.philosophers.core.graph.DegreeTask;
import dev.philosophers.core.graph.Edge;
import dev.philosophers.core.graph.WorkerException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpWorkerClientTest {

    private final DegreeTask task = new DegreeTask("j1", 0, List.of(new Edge(1, 2)));

    @Test
    void returnsTheWorkerResult() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://worker");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://worker/graph/tasks"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                "{\"jobId\":\"j1\",\"taskId\":0,\"partialDegrees\":{\"1\":1,\"2\":1}}",
                                MediaType.APPLICATION_JSON));

        DegreeResult result = new HttpWorkerClient("w", builder.build()).compute(task);

        assertThat(result.partialDegrees()).containsEntry(1, 1).containsEntry(2, 1);
        server.verify();
    }

    @Test
    void serverErrorBecomesWorkerException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://worker");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://worker/graph/tasks")).andRespond(withServerError());

        HttpWorkerClient client = new HttpWorkerClient("w", builder.build());

        assertThatThrownBy(() -> client.compute(task)).isInstanceOf(WorkerException.class);
    }
}
