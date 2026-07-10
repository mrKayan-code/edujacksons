package dev.edujacksons.courses.service;

import dev.edujacksons.common.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Материал не найден (или не принадлежит указанному курсу). → HTTP 404. */
public class MaterialNotFoundException extends ApiException {

    public MaterialNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "material_not_found", "Материал не найден: " + id);
    }
}
