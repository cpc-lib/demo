package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.model.knowledge.RagDocumentVersionDiffLine;
import cc.ivera.ragdemo.model.knowledge.RagDocumentVersionDiffResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DocumentTextDiff {

    @Autowired
    public DocumentTextDiff() {
    }

    public static final int DEFAULT_MAX_LINES = 2000;

    public RagDocumentVersionDiffResponse diff(Long documentId,
                                               Integer leftVersionNo,
                                               Integer rightVersionNo,
                                               String left,
                                               String right) {
        List<String> leftLines = lines(left);
        List<String> rightLines = lines(right);
        boolean truncated = leftLines.size() > DEFAULT_MAX_LINES || rightLines.size() > DEFAULT_MAX_LINES;
        if (truncated) {
            leftLines = leftLines.subList(0, Math.min(leftLines.size(), DEFAULT_MAX_LINES));
            rightLines = rightLines.subList(0, Math.min(rightLines.size(), DEFAULT_MAX_LINES));
        }
        List<RagDocumentVersionDiffLine> diffLines = diffLines(leftLines, rightLines);
        int added = 0;
        int deleted = 0;
        int unchanged = 0;
        for (RagDocumentVersionDiffLine line : diffLines) {
            switch (line.type()) {
                case "ADDED" -> added++;
                case "DELETED" -> deleted++;
                case "UNCHANGED" -> unchanged++;
                default -> {
                }
            }
        }
        return new RagDocumentVersionDiffResponse(
                documentId,
                leftVersionNo,
                rightVersionNo,
                added == 0 && deleted == 0 && !truncated,
                truncated,
                added,
                deleted,
                unchanged,
                diffLines
        );
    }

    private List<RagDocumentVersionDiffLine> diffLines(List<String> left, List<String> right) {
        int[][] lcs = lcs(left, right);
        List<RagDocumentVersionDiffLine> result = new ArrayList<>();
        int i = left.size();
        int j = right.size();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && left.get(i - 1).equals(right.get(j - 1))) {
                result.add(new RagDocumentVersionDiffLine("UNCHANGED", i, j, left.get(i - 1)));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                result.add(new RagDocumentVersionDiffLine("ADDED", null, j, right.get(j - 1)));
                j--;
            } else if (i > 0) {
                result.add(new RagDocumentVersionDiffLine("DELETED", i, null, left.get(i - 1)));
                i--;
            }
        }
        Collections.reverse(result);
        return result;
    }

    private int[][] lcs(List<String> left, List<String> right) {
        int[][] dp = new int[left.size() + 1][right.size() + 1];
        for (int i = 1; i <= left.size(); i++) {
            for (int j = 1; j <= right.size(); j++) {
                if (left.get(i - 1).equals(right.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp;
    }

    private List<String> lines(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return value.replace("\r\n", "\n").replace('\r', '\n').lines().toList();
    }
}
