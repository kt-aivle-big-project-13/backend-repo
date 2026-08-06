-- law_articles.embedding 이 vector(1536) 이라 확장이 없으면 테이블 생성이 실패한다.
-- Hibernate 는 스키마 생성 실패를 로그만 남기고 넘어가므로, 확장 없이 뜨면
-- "relation law_articles does not exist" 로 뒤늦게 드러난다.
CREATE EXTENSION IF NOT EXISTS vector;
