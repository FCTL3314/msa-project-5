import csv
from datetime import timedelta
from pathlib import Path

import pendulum
from airflow.decorators import dag, task
from airflow.utils.email import send_email
from airflow.utils.trigger_rule import TriggerRule


def notify_failure(context):
    run_id = context["dag_run"].run_id
    send_email("reviewer@example.test", f"FAILED marketing_report {run_id}",
               "Исчерпаны попытки. Проверьте журнал задачи в Airflow.")


@dag(dag_id="marketing_report", schedule=None,
     start_date=pendulum.datetime(2025, 1, 1, tz="UTC"), catchup=False,
     default_args={"retries": 1, "retry_delay": timedelta(seconds=8)},
     on_failure_callback=notify_failure)
def marketing_report():
    @task
    def read_deliveries(**context):
        mode = (context["dag_run"].conf or {}).get("sample", "normal")
        files = {"normal": "deliveries.csv", "empty": "empty.csv", "invalid": "invalid.csv"}
        with (Path("/opt/airflow/input") / files[mode]).open() as source:
            rows = csv.DictReader(source)
            delivered = 0
            for row in rows:
                if row["status"] not in {"delivered", "in_transit"}:
                    raise ValueError("Unknown delivery status: " + row["status"])
                delivered += row["status"] == "delivered"
            return delivered

    @task.branch
    def choose_report(count):
        return "publish_report" if count else "record_empty"

    @task
    def publish_report(count):
        print(f"Delivered orders: {count}")

    @task
    def record_empty():
        print("No deliveries for this period")

    @task(trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS)
    def notify_success(**context):
        run_id = context["dag_run"].run_id
        send_email("reviewer@example.test", f"SUCCESS marketing_report {run_id}",
                   "Обработка завершена. Результат доступен в журнале выбранной ветки.")

    count = read_deliveries()
    route = choose_report(count)
    report = publish_report(count)
    empty = record_empty()
    route >> [report, empty]
    [report, empty] >> notify_success()


marketing_report()
