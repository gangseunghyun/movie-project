package com.movie.movie_backend.controller;

import com.movie.movie_backend.dto.SearchResultDto;
import com.movie.movie_backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("")
    public SearchResultDto search(@RequestParam("query") String query,
                                 @RequestParam(value = "type", defaultValue = "all") String type,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size) {
        return searchService.search(query, type, page, size);
    }
} 
