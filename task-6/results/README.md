# Запуск ETL по HTTP и трейсинг

Конфигурация API выключает автозапуск Spring Batch и включает POST /imports. Клиент — [shell-скрипт](client.sh). В POC клиент синхронно получает итог небольшого импорта. Spring Boot Actuator и Micrometer Tracing с Brave создают контекст HTTP-запроса. Контроллер открывает дочерний span для обработки и помещает URI в MDC. Поэтому структурированные события приложения и Spring Batch сохраняют traceId и URI на всём пути. У API, вызова ETL и внутренних шагов собственные spanId.

Из корня репозитория:

```sh
docker compose -f task-4/results/compose.yaml -f task-5/results/observe.yaml -f task-6/results/api.yaml up -d --build
sh task-6/results/client.sh
sh task-6/results/client.sh invalid
```

Первый вызов возвращает HTTP 200, executionId и traceId. Второй подаёт CSV с отрицательным остатком. Он возвращает HTTP 422 с JSON и служит для проверки сбоев (curl заканчивает работу с ненулевым кодом). В Kibana Discover выбрать warehouse-batch-*, добавить поля message, traceId, spanId, URI и выполнить поиск `traceId : "<значение из ответа>"`.

[Трасса в Kibana](evidence/kibana.png) · [События из Elasticsearch](evidence/trace.json) · [Ответ API](evidence/http-success.json) · [Ответ при ошибке](evidence/http-invalid.json).

Перед первым вызовом API в БД оставались [два выполнения из заданий 4 и 5](evidence/before-api.txt). Успешный запрос создал выполнение 3. В Elasticsearch по его traceId найдены семь событий от api.received до завершения задания. В import.finished записаны пять строк и статус COMPLETED. Ошибочный запрос создал выполнение 4 со статусом FAILED.
