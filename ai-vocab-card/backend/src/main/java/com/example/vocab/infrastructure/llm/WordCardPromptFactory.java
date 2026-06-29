package com.example.vocab.infrastructure.llm;

public final class WordCardPromptFactory {
    private WordCardPromptFactory() {}

    public static String build(String word) {
        return """
                You are an English vocabulary assistant for Chinese learners.
                Generate a vocabulary card for the English word below.

                Requirements:
                - Explain the meaning in simple English.
                - Provide Chinese meaning.
                - Provide likely slang/informal expressions when appropriate.
                - Provide practical English example sentences.
                - Return valid JSON only. Do not wrap it in markdown.
                - Use this exact JSON schema:
                {
                  "word": "string",
                  "phonetic": "string",
                  "partOfSpeech": "string",
                  "englishDefinition": "string",
                  "chineseMeaning": "string",
                  "usageNote": "string",
                  "tags": ["string"],
                  "slangs": [
                    {"phrase": "string", "meaning": "string", "example": "string"}
                  ],
                  "examples": [
                    {"sentence": "string", "translation": "string", "scene": "string"}
                  ]
                }

                Word: %s
                """.formatted(word);
    }
}
