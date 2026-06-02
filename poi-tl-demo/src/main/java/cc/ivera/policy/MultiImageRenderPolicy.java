package cc.ivera.policy;

import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.policy.AbstractRenderPolicy;
import com.deepoove.poi.policy.PictureRenderPolicy;
import com.deepoove.poi.render.RenderContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.xwpf.usermodel.IRunBody;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.util.Collection;

public class MultiImageRenderPolicy extends AbstractRenderPolicy<Collection<PictureRenderData>> {
 
    @Override
    protected void afterRender(RenderContext<Collection<PictureRenderData>> context) {
        clearPlaceholder(context, true);
    }
 
    @Override
    public void doRender(RenderContext<Collection<PictureRenderData>> context) throws Exception {
        IRunBody iRunBody = context.getRun().getParent();
        if (iRunBody instanceof XWPFParagraph) {
            XWPFParagraph p = (XWPFParagraph) iRunBody;
            Collection<PictureRenderData> data = context.getData();
            if (CollectionUtils.isNotEmpty(data)) {
                for (PictureRenderData pic : data) {
                    PictureRenderPolicy.Helper.renderPicture(p.createRun(), pic);
                }
            }
        }
    }
}