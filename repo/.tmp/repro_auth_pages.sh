#!/usr/bin/env bash
set -euo pipefail

BASE="${1:-http://localhost:8081}"
AJAR="/tmp/ajar.txt"
RJAR="/tmp/rjar.txt"
FJAR="/tmp/fjar.txt"

csrf_from() {
  curl -s -c "$2" "$1/login" | grep -o 'value="[^"]*"' | head -1 | sed 's/value="//;s/"//'
}

login() {
  local user="$1" pass="$2" jar="$3"
  local csrf
  csrf=$(csrf_from "$BASE" "$jar")
  curl -s -b "$jar" -c "$jar" -o /dev/null \
    -X POST "$BASE/login" \
    --data-urlencode "username=$user" \
    --data-urlencode "password=$pass" \
    --data-urlencode "_csrf=$csrf"
}

post_form() {
  local path="$1" data="$2" jar="$3" page="$4"
  local csrf
  csrf=$(curl -s -b "$jar" "$BASE$page" | grep -o 'value="[^"]*"' | head -1 | sed 's/value="//;s/"//')
  curl -s -L -b "$jar" -c "$jar" -o /dev/null \
    -X POST "$BASE$path" --data-urlencode "_csrf=$csrf" $data
}

rm -f "$AJAR" "$RJAR" "$FJAR"

login admin admin123 "$AJAR"
post_form "/admin/users/new" "--data-urlencode username=testrecruiter --data-urlencode password=test1234 --data-urlencode displayName=TestRecruiter --data-urlencode email=recruiter@test.local --data-urlencode roles=RECRUITER" "$AJAR" "/admin/users/new"
post_form "/admin/users/new" "--data-urlencode username=testfinance --data-urlencode password=test1234 --data-urlencode displayName=TestFinance --data-urlencode email=finance@test.local --data-urlencode roles=FINANCE_CLERK" "$AJAR" "/admin/users/new"

login testrecruiter test1234 "$RJAR"
login testfinance test1234 "$FJAR"

for label in "RECRUITER:/search/unified:$RJAR" "RECRUITER:/exports/list:$RJAR" "RECRUITER:/imports/list:$RJAR" "FINANCE:/search/unified:$FJAR" "FINANCE:/exports/list:$FJAR" "FINANCE:/imports/list:$FJAR"; do
  role="${label%%:*}"
  rest="${label#*:}"
  path="${rest%%:*}"
  jar="${rest##*:}"
  code=$(curl -s -b "$jar" -o /tmp/page.out -w "%{http_code}" "$BASE$path")
  echo "$role $path => $code"
  if [ "$code" != "200" ]; then
    sed -n '1,60p' /tmp/page.out
  fi
done
