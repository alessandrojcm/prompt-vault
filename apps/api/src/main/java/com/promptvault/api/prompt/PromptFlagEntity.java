package com.promptvault.api.prompt;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prompt_flags")
public class PromptFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_id", nullable = false, updatable = false)
    private PromptEntity prompt;

    @Column(name = "flagged_at", nullable = false)
    private Instant flaggedAt;

    @OneToMany(mappedBy = "promptFlag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PromptFlagKeywordSnapshotEntity> keywordSnapshots = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public PromptEntity getPrompt() {
        return prompt;
    }

    public void setPrompt(PromptEntity prompt) {
        this.prompt = prompt;
    }

    public Instant getFlaggedAt() {
        return flaggedAt;
    }

    public List<PromptFlagKeywordSnapshotEntity> getKeywordSnapshots() {
        return keywordSnapshots;
    }

    public void replaceKeywordSnapshots(List<String> keywordTexts) {
        flaggedAt = Instant.now();
        keywordSnapshots.clear();
        keywordTexts.forEach(this::addKeywordSnapshot);
    }

    public void addKeywordSnapshot(String keywordText) {
        PromptFlagKeywordSnapshotEntity snapshot = new PromptFlagKeywordSnapshotEntity();
        snapshot.setPromptFlag(this);
        snapshot.setKeywordText(keywordText);
        keywordSnapshots.add(snapshot);
    }

    @PrePersist
    void onCreate() {
        if (flaggedAt == null) {
            flaggedAt = Instant.now();
        }
    }
}
