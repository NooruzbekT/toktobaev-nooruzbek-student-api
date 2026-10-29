package kg.toktobaev.nooruzbek.studentapi.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST and PUT /api/students.
 */
public record StudentRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 50) String groupName,
        @Min(14) @Max(100) Integer age
) {
}
