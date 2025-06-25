package com.movie.movie_backend.service;

import com.movie.movie_backend.dto.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    public SearchResultDto search(String query) {
        return search(query, "all", 0, 10);
    }

    public SearchResultDto search(String query, String type, int page, int size) {
        // 임시 더미 데이터 (실제 DB 연동 시 Repository로 교체)
        List<MovieSearchResultDto> allMovies = Arrays.asList(
                new MovieSearchResultDto(1L, "기생충", "봉준호", Arrays.asList("스릴러", "드라마"), Arrays.asList("송강호", "이선균"), "https://dummyimage.com/200x300/000/fff&text=기생충"),
                new MovieSearchResultDto(2L, "인터스텔라", "크리스토퍼 놀란", Arrays.asList("SF", "드라마"), Arrays.asList("매튜 맥커너히", "앤 해서웨이"), "https://dummyimage.com/200x300/000/fff&text=인터스텔라"),
                new MovieSearchResultDto(3L, "오펜하이머", "크리스토퍼 놀란", Arrays.asList("드라마", "전쟁"), Arrays.asList("킬리언 머피", "에밀리 블런트"), "https://dummyimage.com/200x300/000/fff&text=오펜하이머"),
                new MovieSearchResultDto(4L, "듄", "드니 빌뇌브", Arrays.asList("SF", "모험"), Arrays.asList("티모시 샬라메", "레베카 퍼거슨"), "https://dummyimage.com/200x300/000/fff&text=듄"),
                new MovieSearchResultDto(5L, "블랙팬서", "라이언 쿠글러", Arrays.asList("액션", "SF"), Arrays.asList("채드윅 보스만", "마이클 B. 조던"), "https://dummyimage.com/200x300/000/fff&text=블랙팬서"),
                new MovieSearchResultDto(6L, "어벤져스", "조스 웨던", Arrays.asList("액션", "SF"), Arrays.asList("로버트 다우니 주니어", "크리스 에반스"), "https://dummyimage.com/200x300/000/fff&text=어벤져스")
        );
        
        List<UserSearchResultDto> allUsers = Arrays.asList(
                new UserSearchResultDto(1L, "user1", "영화마니아"),
                new UserSearchResultDto(2L, "user2", "SF팬"),
                new UserSearchResultDto(3L, "user3", "액션러버"),
                new UserSearchResultDto(4L, "user4", "드라마퀸"),
                new UserSearchResultDto(5L, "user5", "코미디킹"),
                new UserSearchResultDto(6L, "user6", "로맨스퀸")
        );
        
        List<TagSearchResultDto> allTags = Arrays.asList(
                new TagSearchResultDto(1L, "SF"),
                new TagSearchResultDto(2L, "스릴러"),
                new TagSearchResultDto(3L, "드라마"),
                new TagSearchResultDto(4L, "액션"),
                new TagSearchResultDto(5L, "코미디"),
                new TagSearchResultDto(6L, "로맨스"),
                new TagSearchResultDto(7L, "전쟁"),
                new TagSearchResultDto(8L, "모험")
        );

        List<MovieSearchResultDto> filteredMovies = allMovies;
        List<UserSearchResultDto> filteredUsers = allUsers;
        List<TagSearchResultDto> filteredTags = allTags;

        // 검색어가 있으면 필터링
        if (query != null && !query.trim().isEmpty()) {
            if ("all".equals(type) || "movie".equals(type)) {
                filteredMovies = allMovies.stream()
                        .filter(movie -> movie.getTitle().contains(query) || 
                                       movie.getDirectorName().contains(query) ||
                                       movie.getActors().stream().anyMatch(actor -> actor.contains(query)) ||
                                       movie.getTags().stream().anyMatch(tag -> tag.contains(query)))
                        .collect(Collectors.toList());
            }

            if ("all".equals(type) || "user".equals(type)) {
                filteredUsers = allUsers.stream()
                        .filter(user -> user.getUserName().contains(query) || 
                                      user.getNickname().contains(query))
                        .collect(Collectors.toList());
            }

            if ("all".equals(type) || "tag".equals(type)) {
                filteredTags = allTags.stream()
                        .filter(tag -> tag.getTagName().contains(query))
                        .collect(Collectors.toList());
            }
        }

        // 페이징 처리
        int totalElements = 0;
        if ("all".equals(type)) {
            totalElements = filteredMovies.size() + filteredUsers.size() + filteredTags.size();
        } else if ("movie".equals(type)) {
            totalElements = filteredMovies.size();
        } else if ("user".equals(type)) {
            totalElements = filteredUsers.size();
        } else if ("tag".equals(type)) {
            totalElements = filteredTags.size();
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;

        // 페이징 적용 (실제로는 DB에서 처리)
        if ("all".equals(type)) {
            // 통합 검색의 경우 각 타입별로 일부씩 반환
            int movieSize = Math.min(size / 3, filteredMovies.size());
            int userSize = Math.min(size / 3, filteredUsers.size());
            int tagSize = size - movieSize - userSize;
            
            filteredMovies = filteredMovies.stream().limit(movieSize).collect(Collectors.toList());
            filteredUsers = filteredUsers.stream().limit(userSize).collect(Collectors.toList());
            filteredTags = filteredTags.stream().limit(tagSize).collect(Collectors.toList());
        } else {
            // 특정 타입 검색의 경우 페이징 적용
            if ("movie".equals(type)) {
                filteredMovies = filteredMovies.stream()
                        .skip(startIndex).limit(size).collect(Collectors.toList());
                filteredUsers = Arrays.asList();
                filteredTags = Arrays.asList();
            } else if ("user".equals(type)) {
                filteredUsers = filteredUsers.stream()
                        .skip(startIndex).limit(size).collect(Collectors.toList());
                filteredMovies = Arrays.asList();
                filteredTags = Arrays.asList();
            } else if ("tag".equals(type)) {
                filteredTags = filteredTags.stream()
                        .skip(startIndex).limit(size).collect(Collectors.toList());
                filteredMovies = Arrays.asList();
                filteredUsers = Arrays.asList();
            }
        }

        return new SearchResultDto(filteredMovies, filteredUsers, filteredTags, 
                                 totalPages, totalElements, page, size);
    }
} 