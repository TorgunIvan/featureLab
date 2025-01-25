package torgun.featurelab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FeatureLabApplication {

    public static void main(String[] args) {
        /**
         * Если сервис на этапе депалоя упадет, и логирование не будет инициализирвоано,
         * То можно будет увидеть исключение в консоли контейнера
         */
        try {
            SpringApplication.run(FeatureLabApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
