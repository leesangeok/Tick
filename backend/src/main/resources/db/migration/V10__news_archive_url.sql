-- 뉴스 원본 body 를 S3 등 외부 스토리지에 아카이빙한 URL. 미아카이빙은 NULL.
-- ai-server retrieval 은 news.body 를 계속 사용 (짧고 검색 최적화된 사본).
-- archive_url 은 프론트가 사용자에게 원문 딥링크 제공 / 감사 목적으로 사용.
ALTER TABLE news
    ADD COLUMN archive_url VARCHAR(500);
