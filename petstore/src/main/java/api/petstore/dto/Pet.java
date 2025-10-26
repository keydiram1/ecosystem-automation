// Pet.java
package api.petstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder(toBuilder = true)
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pet {
    private Long id;
    private Category category;
    private String name;
    @Singular("photoUrl") private List<String> photoUrls;
    @Singular private List<Tag> tags;
    private String status; // available | pending | sold
}
