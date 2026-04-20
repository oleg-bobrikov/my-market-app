package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
public class SortSelectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSortSelectionPersists() throws Exception {
        mockMvc.perform(get("/items").param("sort", "ALPHA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"ALPHA\" selected=\"selected\">по алфавиту</option>")));

        mockMvc.perform(get("/items").param("sort", "PRICE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"PRICE\" selected=\"selected\">по цене</option>")));
    }

    @Test
    public void testUpdateItemCountPreservesSort() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/items")
                        .param("id", "1")
                        .param("action", "PLUS")
                        .param("sort", "PRICE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/items?search=&sort=PRICE&pageSize=5&pageNumber=1"));
    }

    @Test
    public void testRootPathReturnsItems() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Витрина магазина")));
    }
}
