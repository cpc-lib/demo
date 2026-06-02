package cc.ivera.mail.support.attachment;

import cc.ivera.mail.model.AttachmentSource;
import cc.ivera.mail.model.ResolvedAttachment;

import java.io.IOException;

public interface AttachmentResolver {

    ResolvedAttachment resolve(AttachmentSource attachmentSource) throws IOException;
}
