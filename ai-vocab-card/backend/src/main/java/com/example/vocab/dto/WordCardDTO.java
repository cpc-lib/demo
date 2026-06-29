package com.example.vocab.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.*;
@Data
public class WordCardDTO {
  private Long id;
  @NotBlank private String word;
  private String phonetic;
  private String partOfSpeech;
  @NotBlank private String englishDefinition;
  private String chineseMeaning;
  private String usageNote;
  private List<String> tags = new ArrayList<>();
  @Valid private List<SlangDTO> slangs = new ArrayList<>();
  @Valid private List<ExampleDTO> examples = new ArrayList<>();
}
