# Маркетинговые отчёты

Для ежедневного объединения доставок, заказов, платежей и событий выбираем Apache Airflow. Один запуск охватывает около миллиона записей. Airflow отвечает за зависимости, состояние шагов, повторы и уведомления. Большие выборки остаются в PostgreSQL, объектном хранилище или вычислительном движке. Через XCom передаются счётчики и ссылки на разделы данных.

| Источник или операция | Как подключить |
|---|---|
| CSV, данные о заказах и клиентах | Считать объекты по префиксу даты, получить согласованный срез PostgreSQL через PostgresHook. Признак завершения выгрузки передать следующим задачам. |
| Внешний API | HTTP provider, подключение через Airflow Connection. Сохранять страницы ответа и курсор в объектном хранилище. |
| BigQuery | Google provider, BigQueryInsertJobOperator для SQL и загрузки подготовленного раздела. |
| Redshift | Amazon provider, S3ToRedshiftOperator для массового COPY из S3. |
| Kafka | Apache Kafka provider, ConsumeFromTopicOperator или AwaitMessageSensor. Для периодического отчёта фиксировать диапазон offset и границу окна. |
| Spark | Apache Spark provider, SparkSubmitOperator отправляет задание в вычислительный кластер. |

**Управление процессом.** Branch-задача выбирает путь по результату проверки. Trigger rules задают условия объединения веток и позволяют запустить резервную задачу после сбоя; саму резервную операцию описывает автор DAG. Доступны retries, задержка, exponential backoff, SMTP и callbacks. По событию нового набора данных возможен запуск через Dataset, для внешнего события — sensor или запрос к REST API. Альтернатива в виде одного CronJob подходит для линейной выгрузки. Здесь ей пришлось бы самостоятельно хранить состояние зависимостей и повторов. Spark можно подключить для тяжёлой агрегации.

**Развёртывание.** Локально работают Airflow с LocalExecutor, PostgreSQL для метаданных и Mailpit для доставки тестовых писем. В GCP можно перейти на Cloud Composer либо развернуть Airflow в GKE с отдельными workers, внешней PostgreSQL и журналами в GCS. Доступы к внешним источникам вынести в Secret Manager, для шагов задать пулы и лимиты. Показанный локальный POC использует небольшую выборку.

## Проверка

В каталоге `task-1/results` выполнить:

```sh
docker compose up -d
docker compose exec scheduler airflow dags list
for sample in normal empty invalid; do
  docker compose exec -T scheduler airflow dags trigger marketing_report \
    --conf "{\"sample\":\"$sample\"}"
done
```

Airflow доступен по адресу `http://localhost:18480` (review / review), почта — `http://localhost:18425`. После первого запуска scheduler может потребоваться несколько секунд на разбор DAG.

| Вход | Ожидаемый результат |
|---|---|
| normal | Две доставленные позиции. Задача publish_report пишет результат в лог, приходит SUCCESS. |
| empty | Доставленных заказов нет. Выполняется record_empty, приходит SUCCESS. |
| invalid | Неизвестный статус. Через 8 секунд происходит вторая попытка, затем DAG переходит в FAILED и приходит письмо. |

[Код DAG](dags/marketing.py) · [Обзор трёх прогонов](evidence/runs.png) · [Граф успешной ветки](evidence/airflow.png) · [Повторы и пропуски](evidence/attempts.png) · [Почта](evidence/mail.png) · [Состояния из БД Airflow](evidence/task-instances.txt)

Удаление стенда и его учебных данных: `docker compose down -v`.
