package torgun.featurelab.mdcInReactorExample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/someMocks")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MockController {

    private final MockService mockService;

    @GetMapping(value = "/all")
    public ResponseEntity<List<MockEntity>> getAllMocks(@RequestHeader(value = "requestId") String requestId) {
        MDC.put("requestId", requestId);
        log.info("look at the MDC parameters at the beginning request");
        try {
            List<MockEntity> allMocks = null;
            try {
                allMocks = mockService.getAllMocks(requestId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return ResponseEntity.ok()
                    .header("requestId", requestId)
                    .body(allMocks);
        } finally {
            log.info("look at the MDC parameters at the beginning request");
            MDC.clear();
        }
    }
}
