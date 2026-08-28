package cc.ivera.ai.domain;

public class ImageResp {

    private String url;
    private String note;

    public ImageResp() {
    }

    public ImageResp(String url, String note) {
        this.url = url;
        this.note = note;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
