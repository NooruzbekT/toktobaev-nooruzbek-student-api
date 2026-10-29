package kg.toktobaev.nooruzbek.studentapi;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Search of students by last name (feature/toktobaev-nooruzbek-crud).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentSearchTest {

    @Autowired
    private MockMvc mockMvc;

    private void create(String firstName, String lastName, String email) throws Exception {
        mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"%s","lastName":"%s","email":"%s","groupName":"DevOps-1","age":21}
                        """.formatted(firstName, lastName, email)))
                .andExpect(status().isCreated());
    }

    @Test
    void searchByLastNameIsCaseInsensitiveAndPartial() throws Exception {
        create("Nooruzbek", "Toktobaev", "search.nooruzbek@student.kg");
        create("Aibek", "Toktogulov", "search.aibek@student.kg");
        create("Asan", "Abaev", "search.asan@student.kg");

        mockMvc.perform(get("/api/students").param("lastName", "TOKTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItems("search.nooruzbek@student.kg", "search.aibek@student.kg")))
                .andExpect(jsonPath("$[*].email", not(hasItem("search.asan@student.kg"))))
                .andExpect(jsonPath("$[*].lastName", everyItem(containsStringIgnoringCase("tokto"))));
    }

    @Test
    void blankLastNameReturnsAllStudents() throws Exception {
        create("Bermet", "Asanova", "search.bermet@student.kg");

        mockMvc.perform(get("/api/students").param("lastName", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("search.bermet@student.kg")));
    }
}
