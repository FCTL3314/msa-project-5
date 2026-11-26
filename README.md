# Пакетная обработка данных и телеметрия

| Задание | Результат | Демонстрация |
|---|---|---|
| [1. Маркетинговый конвейер](task-1/results/README.md) | Apache Airflow, ветвление, повторы, локальная SMTP-доставка | [Граф DAG](task-1/results/evidence/airflow.png) · [Письма](task-1/results/evidence/mail.png) |
| [2. B2B-прайсы к 06:00](task-2/results/README.md) | Сравнение четырёх технологий, контекст C4 и план внедрения | [Целевая архитектура](task-2/results/architecture.svg) |
| [3. Экспорт грузоперевозок](task-3/results/README.md) | Kubernetes CronJob, PostgreSQL COPY, PVC | [Запуск в Minikube](task-3/results/evidence/minikube.png) |
| [4. Импорт остатков TradeWare](task-4/results/README.md) | ADR, C4 и Spring Batch worker, обогащение по SKU | [Данные в PostgreSQL](task-4/results/evidence/import.png) |
| [5. Мониторинг и оповещения](task-5/results/README.md) | JMX Exporter, Prometheus, Grafana, Alertmanager, Filebeat и ELK | [Дашборд](task-5/results/evidence/grafana.png) · [Сработавший алерт](task-5/results/evidence/notifications.png) |
| [6. HTTP-запуск и трейсинг](task-6/results/README.md) | API, клиент, корреляция запроса с шагами импорта | [Трасса в Kibana](task-6/results/evidence/kibana.png) |

Во второй части используется единый образ TradeWare. Базовый Compose описан в задании 4. Задание 5 добавляет мониторинг; задание 6 включает HTTP-запуск. Команды запуска, ожидаемые результаты и ссылки на исходные данные приведены в README каждого задания. Учебные пароли предназначены для локального стенда. Интерфейсы привязаны к loopback.

## Полный стенд TradeWare

Из корня репозитория при работающем Docker:

```sh
docker compose -f task-4/results/compose.yaml -f task-5/results/observe.yaml -f task-6/results/api.yaml up -d --build
sh task-6/results/client.sh
```

Java и Gradle находятся в сборочном контейнере. Дождитесь готовности Kibana и HTTP API. В ответе клиента будет traceId для поиска в Discover. Стек поднимался на Docker Desktop с 8 ГБ выделенной памяти. Minikube и Airflow можно запускать отдельно.

Для удаления контейнеров, сетей, данных PostgreSQL и индексов **этого локального стенда**:

```sh
docker compose -f task-4/results/compose.yaml -f task-5/results/observe.yaml -f task-6/results/api.yaml down -v
```

Исходные материалы заданий 4 и 5 находятся в папках initial и complete официального шаблона. Решение и демонстрации размещены только в папках results.
