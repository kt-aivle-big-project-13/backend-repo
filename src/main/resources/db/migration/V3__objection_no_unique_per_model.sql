--
-- 이의제기 번호의 유일성 범위를 전역에서 모델 단위로 좁힌다.
-- 같은 번호라도 다른 AI 모델의 심사 건이면 별개 이의제기로 등록할 수 있어야 한다.
--
-- 기존 전역 제약을 이름으로 지우지 않고 구조로 찾아 지운다. V1 은 이 제약을
-- objections_objection_no_key 로 정의하지만, 운영 DB 는 Flyway 도입 전에
-- Hibernate ddl-auto 로 만들어진 뒤 baseline 된 탓에 실제 이름이
-- uk52txfhvaec38b7jj0o1n4b9cm 처럼 Hibernate 가 생성한 값이다. 이름을 고정해서
-- 지우면 환경에 따라 조용히 아무것도 지우지 않고 넘어가, 전역 제약이 남은 채로
-- 앱만 모델 단위 검사를 하게 된다 — 그 상태에서 다른 모델에 같은 번호를 올리면
-- 앱 검사는 통과하고 INSERT 가 제약에 걸려 500 이 난다.
--
DO $$
DECLARE
    target_constraint text;
BEGIN
    SELECT con.conname
      INTO target_constraint
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
     WHERE nsp.nspname = 'public'
       AND rel.relname = 'objections'
       AND con.contype = 'u'
       -- objection_no 단일 컬럼만 덮는 unique 제약. 이미 (model_id, objection_no)
       -- 로 좁혀진 제약은 컬럼 구성이 달라 여기 걸리지 않는다.
       AND con.conkey = ARRAY[
               (SELECT att.attnum
                  FROM pg_attribute att
                 WHERE att.attrelid = rel.oid
                   AND att.attname = 'objection_no')
           ]::smallint[];

    IF target_constraint IS NOT NULL THEN
        EXECUTE format(
            'ALTER TABLE public.objections DROP CONSTRAINT %I',
            target_constraint
        );
    END IF;
END $$;

-- 이름이 같은 제약이 이미 있으면(개발 환경의 ddl-auto=update 등) 먼저 지운다.
ALTER TABLE public.objections
    DROP CONSTRAINT IF EXISTS uk_objections_model_id_objection_no;

ALTER TABLE public.objections
    ADD CONSTRAINT uk_objections_model_id_objection_no UNIQUE (model_id, objection_no);
