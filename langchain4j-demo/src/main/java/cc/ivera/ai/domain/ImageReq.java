package cc.ivera.ai.domain;

public class ImageReq {

    private String prompt;

    public ImageReq() {
    }

    public ImageReq(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
