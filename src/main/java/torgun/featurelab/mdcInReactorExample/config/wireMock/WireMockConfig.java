package torgun.featurelab.mdcInReactorExample.config.wireMock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Configuration
public class WireMockConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(
                wireMockConfig().port(1234)
        );
        wireMockServer.start();
        return wireMockServer;
    }
}