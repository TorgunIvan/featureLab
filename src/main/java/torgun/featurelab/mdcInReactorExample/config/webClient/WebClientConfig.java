package torgun.featurelab.mdcInReactorExample.config.webClient;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.spring.webflux.LogbookExchangeFilterFunction;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.Map;

@Configuration
@Slf4j
public class WebClientConfig {

    private ExchangeFilterFunction mdcCopyFilter = (request, next) -> {
        Map<String, String> context = MDC.getCopyOfContextMap();
        if (context == null) {
            log.trace("There is no MDC context!");
        }
        return next.exchange(request)
                .doOnNext(c -> {
                    Map<String, String> localContext = MDC.getCopyOfContextMap();
                    if (localContext == null || localContext.isEmpty()) {
                        log.trace("Empty!");
                        MDC.setContextMap(context);
                    }
                });
    };

    private ExchangeFilterFunction mdcClearFilter = (request, next) -> next.exchange(request).doFinally(c -> MDC.clear());

    @Bean
    public WebClient webClient(@Value("${app.http.max-connections}") int maxConnections,
                               WebClient.Builder builder,
                               Logbook logbook) {

        ConnectionProvider connectionProvider = ConnectionProvider.builder("default-connection-pool")
                .maxConnections(maxConnections)
                .metrics(false)
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider);

        /**
         * mdcClearFilter exchange(...) {
         *  return Filter LogbookExchangeFilterFunction: exchange(...) {
         *     return Filter mdcCopyFilter: exchange(...) {
         *         return realExchange(...)
         *             .doOnNext(...)  <-- doOnNext from mdcCopyFilter
         *     }
         *     .flatMap(...)         <-- flatMap from LogbookExchangeFilterFunction (logging the request and response)
         * }
         * .doFinally(...)            // <-- doFinally from mdcClearFilter
         */
        return builder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(mdcClearFilter)
                .filter(new LogbookExchangeFilterFunction(logbook))
                .filter(mdcCopyFilter)
                .build();
    }
}
