package kg.toktobaev.nooruzbek.studentapi.student;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    private static final String NOORUZBEK_JSON = """
            {"firstName":"Nooruzbek","lastName":"Toktobaev","email":"nooruzbek.toktobaev@student.kg",
             "groupName":"DevOps-1","age":21}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService service;

    private static Student nooruzbek() {
        Student student = new Student("Nooruzbek", "Toktobaev", "nooruzbek.toktobaev@student.kg", "DevOps-1", 21);
        ReflectionTestUtils.setField(student, "id", 1L);
        return student;
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        given(service.create(any(StudentRequest.class))).willReturn(nooruzbek());

        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(NOORUZBEK_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/students/1"))
                .andExpect(jsonPath("$.firstName").value("Nooruzbek"))
                .andExpect(jsonPath("$.lastName").value("Toktobaev"));
    }

    @Test
    void getNonexistentStudentReturns404WithErrorBody() throws Exception {
        given(service.findById(9999L)).willThrow(new StudentNotFoundException(9999L));

        mockMvc.perform(get("/api/students/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Student with id 9999 not found"))
                .andExpect(jsonPath("$.path").value("/api/students/9999"));
    }

    @Test
    void createWithInvalidDataReturns400() throws Exception {
        String invalid = """
                {"firstName":"","lastName":"Toktobaev","email":"not-an-email","age":5}
                """;

        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.age").exists());
    }

    @Test
    void updateNonexistentStudentReturns404() throws Exception {
        given(service.update(eq(42L), any(StudentRequest.class))).willThrow(new StudentNotFoundException(42L));

        mockMvc.perform(put("/api/students/42").contentType(MediaType.APPLICATION_JSON).content(NOORUZBEK_JSON))
                .andExpect(status().isNotFound());
    }
}
