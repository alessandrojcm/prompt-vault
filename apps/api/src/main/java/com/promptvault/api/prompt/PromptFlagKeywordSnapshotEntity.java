package com.promptvault.api.prompt;

import jakarta.persistence.*;

@Entity
@Table(name = "prompt_flag_keyword_snapshots")
public class PromptFlagKeywordSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_flag_id", nullable = false, updatable = false)
    private PromptFlagEntity promptFlag;

    @Column(name = "keyword_text", nullable = false, length = 100)
    private String keywordText;

    public Long getId() {
        return id;
    }

    public PromptFlagEntity getPromptFlag() {
        return promptFlag;
    }

    public void setPromptFlag(PromptFlagEntity promptFlag) {
        this.promptFlag = promptFlag;
    }

    public String getKeywordText() {
        return keywordText;
    }

    public void setKeywordText(String keywordText) {
        this.keywordText = keywordText;
    }
}
