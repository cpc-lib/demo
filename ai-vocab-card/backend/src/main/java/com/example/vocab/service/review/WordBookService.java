package com.example.vocab.service.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.review.ReviewScheduleResponse;
import com.example.vocab.entity.WordCard;
import com.example.vocab.entity.review.UserWordBook;
import com.example.vocab.entity.review.WordReviewLog;
import com.example.vocab.mapper.WordCardMapper;
import com.example.vocab.mapper.review.UserWordBookMapper;
import com.example.vocab.mapper.review.WordReviewLogMapper;
import com.example.vocab.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WordBookService {
    private final UserWordBookMapper userWordBookMapper;
    private final WordReviewLogMapper wordReviewLogMapper;
    private final WordCardMapper wordCardMapper;
    private final WordService wordService;

    @Transactional
    public ReviewScheduleResponse add(Long userId, Long wordCardId) {
        assertWordExists(wordCardId);
        UserWordBook existed = userWordBookMapper.selectOne(new LambdaQueryWrapper<UserWordBook>()
                .eq(UserWordBook::getUserId, userId)
                .eq(UserWordBook::getWordCardId, wordCardId)
                .last("LIMIT 1"));
        if (existed != null) return toResponse(existed);

        LocalDateTime now = LocalDateTime.now();
        UserWordBook book = new UserWordBook();
        book.setUserId(userId);
        book.setWordCardId(wordCardId);
        book.setMasteryLevel(0);
        book.setReviewCount(0);
        book.setEaseFactor(2.5D);
        book.setNextReviewTime(now);
        userWordBookMapper.insert(book);
        return toResponse(book);
    }

    public List<WordCardDTO> due(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        return userWordBookMapper.findDue(userId, LocalDateTime.now(), safeLimit)
                .stream()
                .map(UserWordBook::getWordCardId)
                .map(wordService::detail)
                .toList();
    }

    @Transactional
    public ReviewScheduleResponse submit(Long userId, Long wordCardId, int result) {
        assertWordExists(wordCardId);
        UserWordBook book = userWordBookMapper.selectOne(new LambdaQueryWrapper<UserWordBook>()
                .eq(UserWordBook::getUserId, userId)
                .eq(UserWordBook::getWordCardId, wordCardId)
                .last("LIMIT 1"));
        if (book == null) {
            add(userId, wordCardId);
            book = userWordBookMapper.selectOne(new LambdaQueryWrapper<UserWordBook>()
                    .eq(UserWordBook::getUserId, userId)
                    .eq(UserWordBook::getWordCardId, wordCardId)
                    .last("LIMIT 1"));
        }

        LocalDateTime now = LocalDateTime.now();
        UserWordBook planned = ReviewPlanner.plan(book, result, now);
        userWordBookMapper.updateById(planned);

        WordReviewLog log = new WordReviewLog();
        log.setUserId(userId);
        log.setWordCardId(wordCardId);
        log.setResult(result);
        log.setEaseFactor(planned.getEaseFactor());
        log.setIntervalDays(ReviewPlanner.intervalDays(planned, now));
        log.setReviewTime(now);
        log.setNextReviewTime(planned.getNextReviewTime());
        wordReviewLogMapper.insert(log);
        return toResponse(planned);
    }

    private void assertWordExists(Long wordCardId) {
        WordCard word = wordCardMapper.selectById(wordCardId);
        if (word == null || Integer.valueOf(0).equals(word.getStatus())) {
            throw new IllegalArgumentException("word card not found");
        }
    }

    private ReviewScheduleResponse toResponse(UserWordBook book) {
        return ReviewScheduleResponse.builder()
                .userId(book.getUserId())
                .wordCardId(book.getWordCardId())
                .masteryLevel(book.getMasteryLevel())
                .reviewCount(book.getReviewCount())
                .easeFactor(book.getEaseFactor())
                .nextReviewTime(book.getNextReviewTime())
                .build();
    }
}
