--
-- V3 이 지우려던 전역 unique 제약을 실제로 제거한다.
--
-- V3 은 제약을 objections_objection_no_key 라는 이름으로 지운다. V1 이 그 이름으로
-- 정의하므로 V1 부터 실행된 DB 에서는 맞지만, 운영 DB 는 Flyway 도입 전 Hibernate
-- ddl-auto 로 만들어진 뒤 baseline 된 탓에 실제 이름이 uk52txfhvaec38b7jj0o1n4b9cm
-- 처럼 Hibernate 가 생성한 값이었다. DROP CONSTRAINT IF EXISTS 는 대상이 없으면
-- 조용히 넘어가므로, 운영에서는 전역 제약이 그대로 남고 (model_id, objection_no)
-- 제약만 추가된 상태가 됐다.
--
-- 그 상태에서 앱은 모델 단위로만 중복을 확인하므로, 다른 모델에 같은 이의제기 번호를
-- 올리면 사전 검사는 통과하고 INSERT 가 남아 있던 전역 제약에 걸려 500 이 났다.
--
-- V3 을 고치지 않고 새 버전으로 분리하는 이유는 이미 적용된 마이그레이션이기 때문이다.
-- 내용을 바꾸면 체크섬이 달라져 Flyway 검증에 걸리고 애플리케이션이 기동하지 못한다.
--
-- 이름 대신 구조로 찾는다 — objection_no 한 컬럼만 덮는 unique 제약이면 이름이
-- 무엇이든 대상이다. V3 이 정상 동작한 DB 에는 남아 있지 않으므로 아무 일도 하지 않는다.
--
DO $$
DECLARE
    legacy_constraint text;
BEGIN
    SELECT con.conname
      INTO legacy_constraint
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
     WHERE nsp.nspname = 'public'
       AND rel.relname = 'objections'
       AND con.contype = 'u'
       AND con.conkey = ARRAY[
               (SELECT att.attnum
                  FROM pg_attribute att
                 WHERE att.attrelid = rel.oid
                   AND att.attname = 'objection_no')
           ]::smallint[];

    IF legacy_constraint IS NOT NULL THEN
        EXECUTE format(
            'ALTER TABLE public.objections DROP CONSTRAINT %I',
            legacy_constraint
        );
    END IF;
END $$;

--
-- V3 의 ADD 가 어떤 이유로든 반영되지 않은 DB 를 위해 모델 단위 제약을 보장한다.
-- 이미 있으면 아무 일도 하지 않는다.
--
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conrelid = 'public.objections'::regclass
           AND conname = 'uk_objections_model_id_objection_no'
    ) THEN
        ALTER TABLE public.objections
            ADD CONSTRAINT uk_objections_model_id_objection_no
            UNIQUE (model_id, objection_no);
    END IF;
END $$;
