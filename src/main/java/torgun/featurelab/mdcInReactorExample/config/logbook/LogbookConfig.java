package torgun.featurelab.mdcInReactorExample.config.logbook;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.HeaderFilter;
import org.zalando.logbook.core.HeaderFilters;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Configuration
public class LogbookConfig {
    private static final List<String> ALLOWED_HEADERS = Stream.of(
            "requestId"
    ).map(String::toLowerCase).toList();

    @Bean
    public HeaderFilter headerFilter() {
        return HeaderFilters.removeHeaders(
                Predicate.not(header -> ALLOWED_HEADERS.contains(header.toLowerCase()))
        );
    }


}
