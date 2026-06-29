package com.example.vocab.service.review;

import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.review.AnkiExportResponse;
import com.example.vocab.entity.review.UserWordBook;
import com.example.vocab.mapper.review.UserWordBookMapper;
import com.example.vocab.service.WordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnkiExportService {
    private final UserWordBookMapper userWordBookMapper;
    private final WordService wordService;

    public AnkiExportResponse export(Long userId) {
        String header = "Front\tBack\tTags";
        String rows = userWordBookMapper.selectList(new LambdaQueryWrapper<UserWordBook>()
                        .eq(UserWordBook::getUserId, userId)
                        .orderByDesc(UserWordBook::getUpdatedAt))
                .stream()
                .map(UserWordBook::getWordCardId)
                .map(wordService::detail)
                .map(this::toAnkiRow)
                .collect(Collectors.joining("\n"));
        String content = rows.isBlank() ? header : header + "\n" + rows;
        return AnkiExportResponse.builder()
                .fileName("anki-vocab-" + userId + "-" + LocalDate.now() + ".tsv")
                .contentType("text/tab-separated-values;charset=UTF-8")
                .tsvContent(content)
                .build();
    }

    private String toAnkiRow(WordCardDTO card) {
        String front = clean(card.getWord()) + "<br/>" + clean(card.getPhonetic());
        String examples = card.getExamples() == null ? "" : card.getExamples().stream()
                .limit(2)
                .map(e -> clean(e.getSentence()) + "<br/>" + clean(e.getTranslation()))
                .collect(Collectors.joining("<br/>"));
        String back = clean(card.getChineseMeaning()) + "<br/>" + clean(card.getEnglishDefinition()) + "<br/>" + examples;
        String tags = card.getTags() == null ? "" : String.join(" ", card.getTags());
        return front + "\t" + back + "\t" + clean(tags);
    }

    private String clean(String s) {
        return s == null ? "" : s.replace("\t", " ").replace("\n", " ").replace("\r", " ").trim();
    }
}
