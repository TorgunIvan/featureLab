package torgun.featurelab.mdcInReactorExample;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class MockEntity {
    @NotEmpty
    private String Name;

    private Integer salary;
}
