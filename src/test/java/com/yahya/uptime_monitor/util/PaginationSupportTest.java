package com.yahya.uptime_monitor.util;

import com.yahya.uptime_monitor.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationSupportTest {

    @Test
    void createsUsefulPageMetadata() {
        PageResponse<String> response = PageResponse.from(new PageImpl<>(
                List.of("third", "fourth"),
                PageRequest.of(1, 2),
                5
        ));

        assertEquals(List.of("third", "fourth"), response.content());
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(2, response.numberOfElements());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertFalse(response.first());
        assertFalse(response.last());
    }

    @Test
    void rejectsInvalidPageAndPageSizeValues() {
        assertEquals(20, PaginationSupport.DEFAULT_PAGE_SIZE);
        assertEquals(100, PaginationSupport.MAX_PAGE_SIZE);
        assertDoesNotThrow(() -> PaginationSupport.validate(0, 100));

        ResponseStatusException negativePage = assertThrows(
                ResponseStatusException.class,
                () -> PaginationSupport.validate(-1, 20)
        );
        ResponseStatusException zeroSize = assertThrows(
                ResponseStatusException.class,
                () -> PaginationSupport.validate(0, 0)
        );
        ResponseStatusException oversized = assertThrows(
                ResponseStatusException.class,
                () -> PaginationSupport.validate(0, 101)
        );

        assertEquals(400, negativePage.getStatusCode().value());
        assertEquals(400, zeroSize.getStatusCode().value());
        assertEquals(400, oversized.getStatusCode().value());
        assertTrue(PaginationSupport.isRequested(0, null));
        assertTrue(PaginationSupport.isRequested(null, 20));
        assertFalse(PaginationSupport.isRequested(null, null));
    }
}
