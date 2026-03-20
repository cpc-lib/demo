package cc.ivera.model.pojo;

/**
 * 投票详情
 */
public class VoteInfo {

    /** 投票人名称 */
    private String voterName;

    /** 投票结果  1、同意  2、续议  3、拒绝  4、回避 */
    private String voteResult;

    /** 投票意见 */
    private String voteOpinion;


    public VoteInfo() {
    }

    public VoteInfo(String voterName, String voteResult, String voteOpinion) {
        this.voterName = voterName;
        this.voteResult = voteResult;
        this.voteOpinion = voteOpinion;
    }

    public String getVoterName() {
        return voterName;
    }

    public void setVoterName(String voterName) {
        this.voterName = voterName;
    }

    public String getVoteResult() {
        return voteResult;
    }

    public void setVoteResult(String voteResult) {
        this.voteResult = voteResult;
    }

    public String getVoteOpinion() {
        return voteOpinion;
    }

    public void setVoteOpinion(String voteOpinion) {
        this.voteOpinion = voteOpinion;
    }
}

