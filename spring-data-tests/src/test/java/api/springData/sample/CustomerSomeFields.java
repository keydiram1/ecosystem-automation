package api.springData.sample;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSomeFields {

    private String firstName;
    private String lastName;
}
