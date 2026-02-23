.PHONY: pipeline reindex geocode help

## 전체 파이프라인 (수집 → 지오코딩 → ES 색인)
pipeline:
	./gradlew bootRun --args='--batch.job.name=dealPipelineJob'

## ES 재인덱싱만
reindex:
	./gradlew bootRun --args='--batch.job.name=reindexJob'

## 특정 날짜로 재인덱싱 (예: make reindex-date DATE=2026-01-15)
reindex-date:
	./gradlew bootRun --args='--batch.job.name=reindexJob --batch.runDate=$(DATE)'

## 스케줄러 대기 모드 (매일 02:00 자동 실행)
scheduler:
	./gradlew bootRun

## 테스트 실행
test:
	./gradlew test

## 빌드
build:
	./gradlew build -x test

## 도움말
help:
	@echo "사용 가능한 명령어:"
	@echo "  make pipeline      - 전체 파이프라인 실행"
	@echo "  make reindex       - ES 재인덱싱만"
	@echo "  make reindex-date DATE=2026-01-15 - 특정 날짜로 재인덱싱"
	@echo "  make scheduler     - 스케줄러 대기 모드"
	@echo "  make test          - 테스트 실행"
	@echo "  make build         - 빌드"
