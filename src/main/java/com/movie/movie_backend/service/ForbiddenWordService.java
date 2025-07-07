package com.movie.movie_backend.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ForbiddenWordService {
    private static final List<String> FORBIDDEN_WORDS = List.of(
        "씨발","시발", "병신", "개새끼", "미친", "바보", "멍청이", "돌아이", "등신", "호구", "찌질이",
        "fuck", "shit", "bitch", "asshole", "damn", "hell", "bastard", "dick", "pussy", "cock",
        "씨발놈", "씨발년", "씨팔", "씨빨", "씨바", "ㅆㅂ",
        "좆", "좃", "존나", "개년", "개같", "미친놈", "미친년",
        "ㅈㄴ", "ㅈ같", "븅신", "병쉰", "ㅂㅅ",
        "씹", "씹새끼", "씹년", "씹할", "쌍놈", "쌍년", "죽어버려",
        "꺼져", "좇같", "좇같이", "좇같은", "개씨발", "애미", "애비",
        "좆같", "좃같", "좆빠", "좃빠", "좃빨", "좆빨",
        "빨아", "걸레", "보지", "보짓", "보져", "보전",
        "애미뒤진", "애비뒤진", "엿같", "엿머",
        "닥쳐", "지랄", "지럴", "ㅈㄹ", "몰라씨발",
        "헐좃", "지같", "후장", "뒈져", "뒤져",
        "니미", "니미럴", "니애미", "니애비",
        "개노답", "좆노답", "썅", "ㅅㅂ", "ㅄ",
        "꺼지라", "개지랄", "대가리깨져", "꺼지라고", "개빡쳐",
        "씨댕", "시댕", "씨댕이", "시댕이",
        "똥같", "지랄맞", "개도살", "개패듯", "졸라",
        "지옥가라", "개후려", "후려패", "싸가지", "개망나니",
        "지랄발광", "미친개", "개지옥", "좇밥", "좃밥",
        "개털려", "개처맞", "처맞는다", "처발린다",
        "개쳐맞", "쳐죽일", "좆빨아", "좇빨아", "개한심", "극혐"
    );

    public boolean containsForbiddenWords(String text) {
        System.out.println("욕설 검사: " + text);
        if (text == null || text.trim().isEmpty()) return false;
        String lower = text.toLowerCase();
        for (String word : FORBIDDEN_WORDS) {
            System.out.println("비교: " + lower + " vs " + word.toLowerCase());
            if (lower.contains(word.toLowerCase())) {
                System.out.println("욕설 감지됨: " + word);
                return true;
            }
        }
        return false;
    }
} 