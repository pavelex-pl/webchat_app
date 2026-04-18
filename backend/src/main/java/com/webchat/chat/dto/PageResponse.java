package com.webchat.chat.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> items, int page, int size, long total) {
    public static <S, T> PageResponse<T> of(Page<S> p, Function<S, T> mapper) {
        return new PageResponse<>(p.map(mapper).toList(), p.getNumber(), p.getSize(), p.getTotalElements());
    }
}
