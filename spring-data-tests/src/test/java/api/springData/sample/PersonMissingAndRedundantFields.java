package api.springData.sample;

import lombok.Data;

@Data
public class PersonMissingAndRedundantFields {

    private String firstName;
    private String lastName;
    private String missingField;
    // Not annotated with @Field("email") therefore should not be recognized as target field
    // when mapped from a Person class which has @Field("email")
    private String emailAddress;
}
