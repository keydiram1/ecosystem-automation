package api.springData.sample;

import lombok.*;
import org.springframework.data.aerospike.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonSomeFields {

    private String firstName;
    private String lastName;
    @Field("email")
    private String emailAddress;
}
