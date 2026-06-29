package com.example.vocab.entity.search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "vocab-word-card")
public class WordSearchDocument {
    @Id
    private String id;
    @Field(type = FieldType.Keyword)
    private String word;
    @Field(type = FieldType.Text, analyzer = "standard")
    private String englishDefinition;
    @Field(type = FieldType.Text, analyzer = "standard")
    private String chineseMeaning;
    @Field(type = FieldType.Text, analyzer = "standard")
    private String tags;
}
