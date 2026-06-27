package com.promptvault.api.prompt;
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "prompt_submission_history", schema = "prompt_vault")
public class PromptSubmissionHistory {
@jakarta.persistence.Id
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "prompt_id", nullable = false)
private com.promptvault.api.prompt.PromptEntity prompt;

@jakarta.validation.constraints.NotNull
@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;

@jakarta.validation.constraints.NotNull
@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "llm_response", nullable = false)
private java.lang.String llmResponse;

public java.lang.Long getId() {
  return id;
}public void setId(java.lang.Long id) {
  this.id = id;
}

public com.promptvault.api.prompt.PromptEntity getPrompt() {
  return prompt;
}public void setPrompt(com.promptvault.api.prompt.PromptEntity prompt) {
  this.prompt = prompt;
}

public java.time.Instant getCreatedAt() {
  return createdAt;
}public void setCreatedAt(java.time.Instant createdAt) {
  this.createdAt = createdAt;
}

public java.lang.String getLlmResponse() {
  return llmResponse;
}public void setLlmResponse(java.lang.String llmResponse) {
  this.llmResponse = llmResponse;
}

}