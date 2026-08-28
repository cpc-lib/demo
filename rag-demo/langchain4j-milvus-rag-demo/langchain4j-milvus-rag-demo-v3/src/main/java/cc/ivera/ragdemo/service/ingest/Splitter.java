package cc.ivera.ragdemo.service.ingest;


import cc.ivera.ragdemo.config.RagProperties;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class Splitter {

    private final RagProperties props;

    public List<TextSegment> split(Document document) {
        DocumentSplitter splitter = DocumentSplitters.recursive(
                props.getSplitter().getChunkSize(),
                props.getSplitter().getOverlap()
        );
        return splitter.split(document);
    }
}
