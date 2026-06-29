export interface SlangDTO { phrase: string; meaning?: string; example?: string }
export interface ExampleDTO { sentence: string; translation?: string; scene?: string }
export interface WordCardDTO {
  id?: number; word: string; phonetic?: string; partOfSpeech?: string;
  englishDefinition: string; chineseMeaning?: string; usageNote?: string;
  tags?: string[]; slangs?: SlangDTO[]; examples?: ExampleDTO[];
}
export interface WordSearchPageDTO {
  total: number; page: number; size: number; items: WordCardDTO[];
}
