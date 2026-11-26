#!/bin/sh
set -eu
umask 022
# PostgreSQL evaluates both boundaries in the business time zone.
export PGTZ=Europe/Moscow
day=${REPORT_DATE:-$(psql -XtA -v ON_ERROR_STOP=1 -c "select current_date - 1")}
case "$day" in ????-??-??) ;; *) echo "Expected YYYY-MM-DD" >&2; exit 2;; esac
out="/exports/shipments-$day.csv"
partial=$(mktemp /exports/.shipments.XXXXXXXX)
trap 'rm -f "$partial"' EXIT
psql -Xq --set=ON_ERROR_STOP=1 --set=report_day="$day" > "$partial" <<'SQL'
COPY (
  SELECT id, shipped_at, destination, weight_kg FROM shipments
  WHERE shipped_at >= :'report_day'::date
    AND shipped_at < (:'report_day'::date + 1)
  ORDER BY shipped_at, id
) TO STDOUT WITH (FORMAT CSV, HEADER);
SQL
mv "$partial" "$out"
echo "Exported day=$day path=$out bytes=$(wc -c < "$out")"
cat "$out" | head -4
