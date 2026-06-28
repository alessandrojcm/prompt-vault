package com.promptvault.api.prompt;

import com.promptvault.api.promptcategory.PromptCategoryEntity;
import com.promptvault.api.user.UserEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "prompts")
public class PromptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromptVisibility visibility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false, updatable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private PromptCategoryEntity category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "prompt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PromptFlagEntity flag;


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "prompt", cascade = CascadeType.ALL)
    private List<PromptSubmissionHistoryEntity> submissions;

    public List<PromptSubmissionHistoryEntity> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(PromptSubmissionHistoryEntity submissions) {
        this.submissions.add(submissions);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public PromptVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PromptVisibility visibility) {
        this.visibility = visibility;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public PromptCategoryEntity getCategory() {
        return category;
    }

    public void setCategory(PromptCategoryEntity category) {
        this.category = category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public PromptFlagEntity getFlag() {
        return flag;
    }

    public void setFlag(PromptFlagEntity flag) {
        if (this.flag != null && this.flag != flag) {
            this.flag.setPrompt(null);
        }
        this.flag = flag;
        if (flag != null) {
            flag.setPrompt(this);
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (visibility == null) {
            visibility = PromptVisibility.PRIVATE;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
