package torgun.featurelab.mdcInReactorExample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockService {

    private final WebClient webClient;

    @Value(value = "${app.all-mock.url}")
    private String mockUrl;

    @Value(value = "${app.all-mock.timeout}")
    private long mockTimeout;

    @Value(value = "${app.string-mock.url}")
    private String mockUrl2;

    @Value(value = "${app.string-mock.timeout}")
    private long mockTimeout2;

    public List<MockEntity> getAllMocks(String requestId) {

        Mono<List<MockEntity>> mocksList = webClient.get()
                .uri(mockUrl)
                .header("requestId", requestId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MockEntity>>() {})
                .timeout(Duration.ofMillis(mockTimeout))
                .doFinally(c -> MDC.clear());

        Mono<String> mockString = webClient.get()
                .uri(mockUrl2)
                .header("requestId", UUID.randomUUID().toString().replace("-", ""))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(mockTimeout2))
                .doFinally(c -> MDC.clear());

        log.info("look at the MDC parameters in Service ThreadLocal: {}", mockString);

        return Mono.zip(mocksList, mockString, MockService::aggregate).doFinally(c -> MDC.clear()).block();
    }

    public static List<MockEntity> aggregate(List<MockEntity> mocks, String mockString) {
        log.info("look at the MDC parameters in Reactive Thread: {}", mockString);
        return mocks;
    }
}
