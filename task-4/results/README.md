# Импорт товарных остатков

[ADR с альтернативами](ADR.md) · [Схема архитектуры](architecture.svg) · [Исходник Draw.io](architecture.drawio) · [Java-проект](app) · [Конфигурация запуска](compose.yaml)

Входные CSV и схема взяты из шаблона Практикума. Контейнер Gradle собирает Java-приложение. PostgreSQL при создании тома исполняет schema-all.sql и загружает четыре строки справочника. При старте приложения Spring Batch обрабатывает пять товаров. SKU 20001 получает Loyality_on; у SKU 40001 сохраняется исходное значение.

Из корня репозитория:

```sh
docker compose -f task-4/results/compose.yaml up -d --build
docker compose -f task-4/results/compose.yaml logs app
docker compose -f task-4/results/compose.yaml exec db psql -U warehouse -c 'TABLE products'
```

[Скриншот исходных данных и результата](evidence/import.png) · [Вывод SQL и состояние шага](evidence/import.txt) · [Лог выполнения](evidence/batch-events.txt). Следующие задания используют тот же образ. Dockerfile собирает код импорта из этого каталога, слушатель метрик из task-5 и условно включаемый HTTP-контроллер из task-6.
