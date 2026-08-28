package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.model.knowledge.MultimodalCollectionStatus;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.service.vector.MultimodalVectorStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/multimodal")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "RAG multimodal operations", description = "Native multimodal vector collection management APIs.")
public class RagMultimodalController {

    private final MultimodalVectorStore multimodalVectorStore;

    @PostMapping("/collections/ensure")
    @Operation(summary = "Ensure native multimodal collection", description = "Create and load the Milvus collection with text_vector and image_vector fields when enabled.")
    public RagApiResponse<MultimodalCollectionStatus> ensureCollection() {
        return RagApiResponse.ok(multimodalVectorStore.ensureCollection());
    }
}
