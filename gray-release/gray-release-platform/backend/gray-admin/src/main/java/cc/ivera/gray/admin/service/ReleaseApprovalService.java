package cc.ivera.gray.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.gray.admin.entity.ReleaseApproval;
import cc.ivera.gray.admin.entity.ReleaseTask;
import cc.ivera.gray.admin.mapper.ReleaseApprovalMapper;
import cc.ivera.gray.admin.mapper.ReleaseTaskMapper;
import cc.ivera.gray.common.GrayEnums.ApprovalStatus;
import cc.ivera.gray.common.GrayEnums.ReleaseStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseApprovalService {
    private final ReleaseApprovalMapper releaseApprovalMapper;
    private final ReleaseTaskMapper releaseTaskMapper;
    private final AuditService auditService;

    public ReleaseApprovalService(ReleaseApprovalMapper releaseApprovalMapper,
                                  ReleaseTaskMapper releaseTaskMapper,
                                  AuditService auditService) {
        this.releaseApprovalMapper = releaseApprovalMapper;
        this.releaseTaskMapper = releaseTaskMapper;
        this.auditService = auditService;
    }

    public List<ReleaseApproval> list(Long taskId) {
        LambdaQueryWrapper<ReleaseApproval> wrapper = new LambdaQueryWrapper<ReleaseApproval>()
                .orderByDesc(ReleaseApproval::getUpdateTime);
        if (taskId != null) {
            wrapper.eq(ReleaseApproval::getTaskId, taskId);
        }
        return releaseApprovalMapper.selectList(wrapper);
    }

    @Transactional
    public ReleaseApproval createPending(Long taskId, String applicant) {
        ReleaseApproval approval = new ReleaseApproval();
        approval.setTaskId(taskId);
        approval.setApplicant(applicant);
        approval.setStatus(ApprovalStatus.PENDING.name());
        approval.setComment("等待审批");
        releaseApprovalMapper.insert(approval);
        auditService.record(applicant, "SUBMIT_APPROVAL", "RELEASE_APPROVAL", String.valueOf(approval.getId()), null, "PENDING");
        return approval;
    }

    @Transactional
    public ReleaseApproval approve(Long approvalId, String approver, String comment) {
        ReleaseApproval approval = requireApproval(approvalId);
        if (!ApprovalStatus.PENDING.name().equals(approval.getStatus())) {
            throw new IllegalArgumentException("审批单不是待审批状态");
        }
        approval.setStatus(ApprovalStatus.APPROVED.name());
        approval.setApprover(approver);
        approval.setComment(comment);
        releaseApprovalMapper.updateById(approval);

        ReleaseTask task = releaseTaskMapper.selectById(approval.getTaskId());
        if (task != null && ReleaseStatus.WAITING_APPROVAL.name().equals(task.getStatus())) {
            task.setStatus(ReleaseStatus.DRAFT.name());
            releaseTaskMapper.updateById(task);
        }
        auditService.record(approver, "APPROVE", "RELEASE_APPROVAL", String.valueOf(approvalId), "PENDING", "APPROVED");
        return releaseApprovalMapper.selectById(approvalId);
    }

    @Transactional
    public ReleaseApproval reject(Long approvalId, String approver, String comment) {
        ReleaseApproval approval = requireApproval(approvalId);
        if (!ApprovalStatus.PENDING.name().equals(approval.getStatus())) {
            throw new IllegalArgumentException("审批单不是待审批状态");
        }
        approval.setStatus(ApprovalStatus.REJECTED.name());
        approval.setApprover(approver);
        approval.setComment(comment);
        releaseApprovalMapper.updateById(approval);

        ReleaseTask task = releaseTaskMapper.selectById(approval.getTaskId());
        if (task != null) {
            task.setStatus(ReleaseStatus.REJECTED.name());
            releaseTaskMapper.updateById(task);
        }
        auditService.record(approver, "REJECT", "RELEASE_APPROVAL", String.valueOf(approvalId), "PENDING", "REJECTED");
        return releaseApprovalMapper.selectById(approvalId);
    }

    public boolean hasApproved(Long taskId) {
        Long count = releaseApprovalMapper.selectCount(new LambdaQueryWrapper<ReleaseApproval>()
                .eq(ReleaseApproval::getTaskId, taskId)
                .eq(ReleaseApproval::getStatus, ApprovalStatus.APPROVED.name()));
        return count != null && count > 0;
    }

    private ReleaseApproval requireApproval(Long approvalId) {
        ReleaseApproval approval = releaseApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("审批单不存在");
        }
        return approval;
    }
}

