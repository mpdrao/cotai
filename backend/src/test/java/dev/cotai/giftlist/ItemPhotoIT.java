package dev.cotai.giftlist;

import dev.cotai.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stock photo on create, upload, public read, replacement and removal of item photos. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class ItemPhotoIT {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    @Autowired
    private MockMvc mvc;

    private String bearer;
    private String listId;
    private String itemId;

    @BeforeEach
    void createListWithStockPhoto() throws Exception {
        String email = "photo-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"segredo123\"}"))
                .andExpect(status().is2xxSuccessful());
        String token = field(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"segredo123\"}"))
                .andReturn().getResponse().getContentAsString(), "accessToken");
        bearer = "Bearer " + token;

        String body = mvc.perform(post("/api/gift-lists").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventTypeCode":"BARBECUE","title":"Churrasco da firma","eventDate":"%s",
                                 "items":[{"name":"Picanha","unit":"kg","unitPriceCents":9000,"totalQuotas":4,
                                           "imageUrl":"/suggestions/barbecue-1.jpg"}]}
                                """.formatted(LocalDate.now().plusDays(30))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].imageUrl").value("/suggestions/barbecue-1.jpg"))
                .andReturn().getResponse().getContentAsString();
        listId = field(body, "id");
        itemId = body.replaceAll("(?s).*\"items\":\\[\\{\"id\":(\\d+).*", "$1");
    }

    @Test
    void uploadReplacesStockPhotoAndIsPublic() throws Exception {
        String url = field(upload(PNG).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "imageUrl");

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));

        String replaced = field(upload(PNG).andReturn().getResponse().getContentAsString(), "imageUrl");
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(replaced)).andExpect(status().isOk());

        mvc.perform(delete(photoPath()).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
        mvc.perform(get(replaced)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsFilesThatAreNotImages() throws Exception {
        upload("<svg onload=alert(1)>".getBytes())
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("PHOTO_INVALID"));
    }

    @Test
    void rejectsImageUrlsOutsideTheSuggestions() throws Exception {
        mvc.perform(post("/api/gift-lists/" + listId + "/items").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gelo\",\"unit\":\"saco\",\"unitPriceCents\":1200,\"totalQuotas\":2,"
                                + "\"imageUrl\":\"https://evil.example/x.jpg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherOrganizerCannotUpload() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, photoPath()).file(new MockMultipartFile("file", PNG)))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions upload(byte[] data) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, photoPath())
                .file(new MockMultipartFile("file", "foto.png", "image/png", data))
                .header("Authorization", bearer));
    }

    private String photoPath() {
        return "/api/gift-lists/" + listId + "/items/" + itemId + "/photo";
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"([^\"]+)\"").matcher(json);
        if (!m.find()) {
            throw new AssertionError(name + " not found in " + json);
        }
        return m.group(1);
    }
}
