ALTER TABLE ai.prompt_templates
    ADD COLUMN max_tokens INTEGER,
    ADD CONSTRAINT prompt_templates_max_tokens_ck CHECK (max_tokens BETWEEN 1 AND 32768);
