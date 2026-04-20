package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import ru.yandex.practicum.mymarket.BaseWebMvcTest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ItemControllerTest extends BaseWebMvcTest {

    @Test
    public void testSortSelectionPersists() throws Exception {
        verifyNoInteractions(cartService);
        when(itemService.getItems(anyString(), anyString(), any())).thenReturn(Page.empty());

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
                .andExpect(redirectedUrl("/items?search=&sort=PRICE&pageSize=5&pageNumber=1#item-1"));
    }

    @Test
    public void testRootPathReturnsItems() throws Exception {
        when(itemService.getItems(anyString(), anyString(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Витрина магазина")));
    }
}
