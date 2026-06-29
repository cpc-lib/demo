package com.example.vocab.infrastructure.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnkiExportMessage {
    private Long taskId;
    private Long userId;
}
