package kg.toktobaev.nooruzbek.studentapi;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full CRUD flow against an in-memory H2 database (profile "test"), no PostgreSQL needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void infoShowsAuthor() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("Student API"))
                .andExpect(jsonPath("$.author").value("Nooruzbek Toktobaev"));
    }

    @Test
    void fullCrudFlow() throws Exception {
        String body = mockMvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Nooruzbek","lastName":"Toktobaev","email":"crud.flow@student.kg",
                         "groupName":"DevOps-1","age":21}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/students/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nooruzbek"));

        mockMvc.perform(put("/api/students/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Nooruzbek","lastName":"Toktobaev","email":"crud.flow@student.kg",
                         "groupName":"DevOps-2","age":22}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("DevOps-2"))
                .andExpect(jsonPath("$.age").value(22));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("crud.flow@student.kg")));

        mockMvc.perform(delete("/api/students/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/" + id))
                .andExpect(status().isNotFound());
    }
}
