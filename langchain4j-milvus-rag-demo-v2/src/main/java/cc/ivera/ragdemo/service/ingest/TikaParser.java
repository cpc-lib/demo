package cc.ivera.ragdemo.service.ingest;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class TikaParser {
    private final DocumentParser parser = new ApacheTikaDocumentParser();

    public Document parse(InputStream in) {
        return parser.parse(in);
    }
}
