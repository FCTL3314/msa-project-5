# Ежедневная выгрузка перевозок

CronJob запускает контейнер в 20:00 по Москве. Для дневного отчёта выбран последний **завершённый календарный день**. Скрипт берёт shipments за полуинтервал от полуночи до полуночи по бизнес-времени и пишет результат PostgreSQL COPY в CSV. Дата в имени файла и атомарный rename позволяют безопасно повторить выгрузку. PVC сохраняет результат после завершения Pod. В облаке аналитики получают файл из подключённого корпоративного хранилища.

[Манифесты](k8s) · [Экспортёр](export.sh) · [Dockerfile](Dockerfile) · [Скриншот фактического прогона](evidence/minikube.png) · [Вывод команд](evidence/minikube.txt)

## Повторить в Minikube

Команды выполняются из `task-3/results`. Для прогона на Mac использовался временный исполняемый файл Minikube 1.36.0 для arm64, сверенный по SHA-256. Профиль и kubeconfig хранились во временном каталоге, драйвером был Docker Desktop. Демонстрационная БД создаёт три перевозки при инициализации. Две относятся к предыдущему дню, третья проверяет верхнюю границу окна.

```sh
minikube start -p msa5-review --driver=docker
minikube -p msa5-review image build -t freight-export:sprint5 .
kubectl --context=msa5-review apply -f k8s/namespace.yaml
kubectl --context=msa5-review apply -f k8s/database.yaml -f k8s/export.yaml
kubectl --context=msa5-review -n freight-demo rollout status deployment/db
kubectl --context=msa5-review -n freight-demo create job --from=cronjob/shipment-export export-review
kubectl --context=msa5-review -n freight-demo wait --for=condition=complete job/export-review --timeout=180s
kubectl --context=msa5-review -n freight-demo logs job/export-review
kubectl --context=msa5-review -n freight-demo get cronjob,job,pvc
```

Ручной запуск создаёт Job из того же шаблона, что использует расписание. Проверены статус Complete, CSV с заголовком и двумя строками. Для повторной проверки на том же кластере задайте новое имя Job. Временный кластер целиком удаляется командой `minikube delete -p msa5-review`.
