package com.yahya.uptime_monitor.controller;

import com.yahya.uptime_monitor.dto.PageResponse;
import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.service.WebsiteCheckCoordinator;
import com.yahya.uptime_monitor.service.WebsiteService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebsiteControllerPaginationTest {

    @Test
    void returnsOneWebsiteWithoutLoadingTheWholeCollection() {
        WebsiteService websiteService = mock(WebsiteService.class);
        WebsiteController controller = new WebsiteController(
                websiteService,
                mock(WebsiteCheckCoordinator.class)
        );
        Website website = new Website("API", "https://example.com");
        when(websiteService.getWebsite(7L)).thenReturn(website);

        assertEquals(website, controller.getWebsite(7L));
        verify(websiteService).getWebsite(7L);
    }

    @Test
    void preservesTheLegacyListAndReturnsMetadataWhenPaginationIsRequested() {
        WebsiteService websiteService = mock(WebsiteService.class);
        WebsiteCheckCoordinator coordinator = mock(WebsiteCheckCoordinator.class);
        WebsiteController controller = new WebsiteController(websiteService, coordinator);
        Website website = new Website("API", "https://example.com");
        PageRequest pageable = PageRequest.of(0, 20);
        when(websiteService.getAllWebsites()).thenReturn(List.of(website));
        when(websiteService.getAllWebsites(pageable)).thenReturn(new PageImpl<>(
                List.of(website),
                pageable,
                41
        ));

        Object legacyResponse = controller.getAllWebsites(
                null, null, null, null, null, null, null, null, pageable
        );
        Object paginatedResponse = controller.getAllWebsites(
                0, 20, null, null, null, null, null, null, pageable
        );

        assertEquals(List.of(website), legacyResponse);
        PageResponse<?> page = assertInstanceOf(PageResponse.class, paginatedResponse);
        assertEquals(41, page.totalElements());
        assertEquals(3, page.totalPages());
        assertEquals(1, page.numberOfElements());
    }

    @Test
    void acceptsSpringDataSortSyntaxForWebsiteQueries() {
        WebsiteService websiteService = mock(WebsiteService.class);
        WebsiteCheckCoordinator coordinator = mock(WebsiteCheckCoordinator.class);
        WebsiteController controller = new WebsiteController(websiteService, coordinator);
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "status")
        );
        when(websiteService.findWebsites(
                null, null, null, null, "status", "DESC"
        )).thenReturn(List.of());

        controller.getAllWebsites(
                null, null, null, null, null, null, null, null, pageable
        );

        verify(websiteService).findWebsites(
                null, null, null, null, "status", "DESC"
        );
    }
}
