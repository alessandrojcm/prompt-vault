package com.promptvault.api.prompt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompt_flags")
public class PromptFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_id", nullable = false, updatable = false)
    private PromptEntity prompt;

    @Column(name = "flagged_at", nullable = false, updatable = false)
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
