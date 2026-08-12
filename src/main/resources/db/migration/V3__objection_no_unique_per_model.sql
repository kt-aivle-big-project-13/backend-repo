--
-- 이의제기 번호의 유일성 범위를 전역에서 모델 단위로 좁힌다.
-- 같은 번호라도 다른 AI 모델의 심사 건이면 별개 이의제기로 등록할 수 있어야 한다.
--

ALTER TABLE public.objections
    DROP CONSTRAINT IF EXISTS objections_objection_no_key;

ALTER TABLE public.objections
    ADD CONSTRAINT uk_objections_model_id_objection_no UNIQUE (model_id, objection_no);
