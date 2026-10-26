package dev.cotai.giftlist;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stock photo on create, upload, public read, replacement, removal and the rules on who and when. */
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
        bearer = organizer();
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
        listId = JsonPath.read(body, "$.id");
        itemId = String.valueOf((Integer) JsonPath.read(body, "$.items[0].id"));
    }

    @Test
    void uploadReplacesStockPhotoAndIsPublic() throws Exception {
        String url = imageUrl(upload(PNG).andExpect(status().isOk()));

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));

        String replaced = imageUrl(upload(PNG).andExpect(status().isOk()));
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
    void rejectsPhotosAboveTheLimit() throws Exception {
        byte[] big = Arrays.copyOf(PNG, PhotoFormat.MAX_BYTES + 1);
        upload(big)
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.code").value("PHOTO_TOO_LARGE"));
    }

    @Test
    void requiresAToken() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, photoPath()).file(new MockMultipartFile("file", PNG)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anotherOrganizerCannotChangeThePhoto() throws Exception {
        String other = organizer();
        mvc.perform(multipart(HttpMethod.PUT, photoPath())
                        .file(new MockMultipartFile("file", "foto.png", "image/png", PNG))
                        .header("Authorization", other))
                .andExpect(status().isNotFound());
        mvc.perform(delete(photoPath()).header("Authorization", other))
                .andExpect(status().isNotFound());
    }

    @Test
    void closedListPhotosCannotChange() throws Exception {
        mvc.perform(post("/api/gift-lists/" + listId + "/publish").header("Authorization", bearer))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/gift-lists/" + listId + "/close").header("Authorization", bearer))
                .andExpect(status().is2xxSuccessful());

        upload(PNG).andExpect(status().isConflict());
        mvc.perform(delete(photoPath()).header("Authorization", bearer))
                .andExpect(status().isConflict());
    }

    @Test
    void archivedItemPhotoCannotChange() throws Exception {
        mvc.perform(delete("/api/gift-lists/" + listId + "/items/" + itemId).header("Authorization", bearer))
                .andExpect(status().isNoContent());

        upload(PNG).andExpect(status().isNotFound());
    }

    private String organizer() throws Exception {
        String email = "photo-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"segredo123\"}"))
                .andExpect(status().is2xxSuccessful());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"segredo123\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    private ResultActions upload(byte[] data) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, photoPath())
                .file(new MockMultipartFile("file", "foto.png", "image/png", data))
                .header("Authorization", bearer));
    }

    private static String imageUrl(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.imageUrl");
    }

    private String photoPath() {
        return "/api/gift-lists/" + listId + "/items/" + itemId + "/photo";
    }
}
