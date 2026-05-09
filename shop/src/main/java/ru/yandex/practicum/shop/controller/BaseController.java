package ru.yandex.practicum.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;

@Controller
public class BaseController {

    protected String getParam(MultiValueMap<String, String> formData, MultiValueMap<String, String> queryParams, String name) {
        String value = formData.getFirst(name);
        return value != null ? value : queryParams.getFirst(name);
    }
}
