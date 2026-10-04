#!/bin/bash
# HDMS 端到端业务流验证脚本
set -e
BASE=http://localhost:8080/api
TODAY=$(date +%F)
TOMORROW=$(date -d "+1 day" +%F)
PASS=0; FAIL=0

ok()   { PASS=$((PASS+1)); echo "  ✅ $1"; }
bad()  { FAIL=$((FAIL+1)); echo "  ❌ $1"; }
check(){ if [ "$2" = "$3" ]; then ok "$1"; else bad "$1 (期望[$3] 实际[$2])"; fi; }

login() { curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("token",""))'; }

echo "== 1. 多角色登录 =="
ADMIN=$(login admin admin123);   [ -n "$ADMIN" ] && ok "admin 登录" || bad "admin 登录"
HEAD=$(login headnurse head123); [ -n "$HEAD" ] && ok "headnurse 登录" || bad "headnurse 登录"
NURSE=$(login nurse1 nurse123);  [ -n "$NURSE" ] && ok "nurse1 登录" || bad "nurse1 登录"
ENG=$(login eng1 eng123);        [ -n "$ENG" ] && ok "eng1 登录" || bad "eng1 登录"
FIN=$(login fin1 fin123);        [ -n "$FIN" ] && ok "fin1 登录" || bad "fin1 登录"
AUTH_H="Authorization: Bearer $HEAD"; AUTH_N="Authorization: Bearer $NURSE"
AUTH_E="Authorization: Bearer $ENG"; AUTH_F="Authorization: Bearer $FIN"; AUTH_A="Authorization: Bearer $ADMIN"
CT='Content-Type: application/json'

echo "== 2. 排班看板 =="
BOARD=$(curl -s "$BASE/schedules/board?date=$TODAY" -H "$AUTH_H")
MCOUNT=$(echo "$BOARD" | python3 -c 'import sys,json;print(len(json.load(sys.stdin)["machines"]))')
check "看板机器数=10" "$MCOUNT" "10"

echo "== 3. 自动排班（明日早班）=="
AUTO=$(curl -s -X POST $BASE/schedules/auto -H "$AUTH_H" -H "$CT" -d "{\"date\":\"$TOMORROW\",\"shift\":\"MORNING\"}")
ASSIGNED=$(echo "$AUTO" | python3 -c 'import sys,json;print(len(json.load(sys.stdin)["assigned"]))')
[ "$ASSIGNED" -ge 1 ] && ok "自动排班分配 $ASSIGNED 人" || bad "自动排班无分配"

echo "== 4. 调班影响预览 + 执行 =="
# 取明日 M01 早班第一个排班
SID=$(curl -s "$BASE/schedules/board?date=$TOMORROW" -H "$AUTH_H" | python3 -c '
import sys,json
d=json.load(sys.stdin)
for m in d["machines"]:
    for c in m["cells"]["MORNING"]:
        print(c["id"]); exit()')
PREV=$(curl -s -X POST $BASE/schedules/$SID/move/preview -H "$AUTH_H" -H "$CT" -d '{"machineId":5,"shift":"MORNING"}')
HAS_KEYS=$(echo "$PREV" | python3 -c 'import sys,json;d=json.load(sys.stdin);print("targetQueue" in d and "timelineIssues" in d and "preDrawnConsumables" in d and "affectedPatients" in d)')
check "预览含影响分析字段" "$HAS_KEYS" "True"
curl -s -X POST $BASE/schedules/$SID/move -H "$AUTH_H" -H "$CT" -d '{"machineId":5,"shift":"MORNING"}' > /dev/null
MOVED=$(curl -s "$BASE/schedules/board?date=$TOMORROW" -H "$AUTH_H" | python3 -c '
import sys,json
d=json.load(sys.stdin)
for m in d["machines"]:
    if m["machine"]["code"]=="M05":
        print(len(m["cells"]["MORNING"]))')
check "调班后 M05 早班有排班" "$MOVED" "1"

echo "== 5. 分区不匹配应被拒绝 =="
# 患者 P0004 是 HBV 区，M01 是清洁区
ERR=$(curl -s -X POST $BASE/schedules -H "$AUTH_H" -H "$CT" \
  -d "{\"patientId\":4,\"machineId\":1,\"date\":\"$TOMORROW\",\"shift\":\"EVENING\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin).get("error",""))')
echo "$ERR" | grep -q "分区" && ok "分区校验拦截: $ERR" || bad "分区校验未拦截"

echo "== 6. 透析全流程（评估→上机→监测→低血压异常→下机）=="
# 取今日 PLANNED 且已有评估的排班 s4（杨丽娟）
TID=$(curl -s "$BASE/schedules/board?date=$TODAY" -H "$AUTH_N" | python3 -c '
import sys,json
d=json.load(sys.stdin)
for m in d["machines"]:
    for cells in m["cells"].values():
        for c in cells:
            if c.get("treatmentStatus")=="PRE_RECORDED":
                                print(c["treatmentId"]); exit()')
[ -n "$TID" ] && ok "找到待上机治疗记录 #$TID" || bad "无待上机记录"
curl -s -X POST $BASE/treatments/$TID/start -H "$AUTH_N" > /dev/null
ST=$(curl -s $BASE/treatments/$TID -H "$AUTH_N" | python3 -c 'import sys,json;print(json.load(sys.stdin)["treatment"]["status"])')
check "上机后状态=ON_DIALYSIS" "$ST" "ON_DIALYSIS"
# 录入正常生命体征
curl -s -X POST $BASE/treatments/$TID/entries -H "$AUTH_N" -H "$CT" \
  -d '{"type":"VITALS","bpSys":135,"bpDia":82,"heartRate":76}' > /dev/null
# 录入低血压 → 应自动生成异常
curl -s -X POST $BASE/treatments/$TID/entries -H "$AUTH_N" -H "$CT" \
  -d '{"type":"VITALS","bpSys":85,"bpDia":50,"heartRate":98,"note":"头晕出冷汗"}' > /dev/null
HYPO=$(curl -s "$BASE/exceptions?status=OPEN" -H "$AUTH_N" | python3 -c '
import sys,json
print(sum(1 for e in json.load(sys.stdin) if e["type"]=="HYPOTENSION"))')
[ "$HYPO" -ge 1 ] && ok "低血压自动上报异常" || bad "低血压未生成异常"
# 医嘱变更 → 异常
curl -s -X POST $BASE/treatments/$TID/entries -H "$AUTH_N" -H "$CT" \
  -d '{"type":"ORDER_CHANGE","note":"医生调整脱水量至1500ml"}' > /dev/null
# 下机
curl -s -X POST $BASE/treatments/$TID/complete -H "$AUTH_N" -H "$CT" \
  -d '{"postWeightKg":52.4,"hemostasisOk":true,"departureMethod":"TRANSPORT","nextScheduleNote":"按规律下次早班"}' > /dev/null
ST2=$(curl -s $BASE/treatments/$TID -H "$AUTH_N" | python3 -c 'import sys,json;print(json.load(sys.stdin)["treatment"]["status"])')
check "下机后状态=COMPLETED" "$ST2" "COMPLETED"

echo "== 7. 下机联动：机器待消毒 → 消毒合格 → 空闲；账单生成 =="
M03ST=$(curl -s $BASE/engineering/machines -H "$AUTH_E" | python3 -c '
import sys,json
print([m["status"] for m in json.load(sys.stdin) if m["code"]=="M03"][0])')
check "M03 状态=DISINFECTING" "$M03ST" "DISINFECTING"
M03ID=$(curl -s $BASE/engineering/machines -H "$AUTH_E" | python3 -c '
import sys,json
print([m["id"] for m in json.load(sys.stdin) if m["code"]=="M03"][0])')
curl -s -X POST $BASE/engineering/records -H "$AUTH_E" -H "$CT" \
  -d "{\"machineId\":$M03ID,\"type\":\"DISINFECTION\",\"result\":\"PASS\",\"note\":\"热消毒\"}" > /dev/null
M03ST2=$(curl -s $BASE/engineering/machines -H "$AUTH_E" | python3 -c '
import sys,json
print([m["status"] for m in json.load(sys.stdin) if m["code"]=="M03"][0])')
check "消毒合格后 M03=IDLE" "$M03ST2" "IDLE"
BILL=$(curl -s "$BASE/billing?status=PENDING" -H "$AUTH_F" | python3 -c '
import sys,json
rows=json.load(sys.stdin)
b=[b for b in rows if b["treatment"]["id"]=='"$TID"']
print(b[0]["id"] if b else "")')
[ -n "$BILL" ] && ok "下机自动生成账单 #$BILL" || bad "未生成账单"

echo "== 8. 财务：补收费用 + 核对 =="
curl -s -X POST $BASE/billing/$BILL/adjust -H "$AUTH_F" -H "$CT" \
  -d '{"extraAmount":50.00,"extraNote":"加做血滤"}' > /dev/null
curl -s -X POST $BASE/billing/$BILL/confirm -H "$AUTH_F" > /dev/null
BST=$(curl -s "$BASE/billing?status=CONFIRMED" -H "$AUTH_F" | python3 -c '
import sys,json
print(any(b["id"]=='"$BILL"' for b in json.load(sys.stdin)))')
check "账单核对完成" "$BST" "True"

echo "== 9. 耗材：预领→取消自动退回；损耗；低库存异常 =="
# 新建明日排班并预领
NS=$(curl -s -X POST $BASE/schedules -H "$AUTH_H" -H "$CT" \
  -d "{\"patientId\":9,\"machineId\":2,\"date\":\"$TOMORROW\",\"shift\":\"AFTERNOON\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
STOCK0=$(curl -s $BASE/consumables -H "$AUTH_N" | python3 -c 'import sys,json;print([c["stock"] for c in json.load(sys.stdin) if c["code"]=="HC003"][0])')
curl -s -X POST $BASE/consumables/txns -H "$AUTH_N" -H "$CT" \
  -d "{\"consumableId\":3,\"scheduleId\":$NS,\"type\":\"PRE_DRAW\",\"quantity\":2}" > /dev/null
STOCK1=$(curl -s $BASE/consumables -H "$AUTH_N" | python3 -c 'import sys,json;print([c["stock"] for c in json.load(sys.stdin) if c["code"]=="HC003"][0])')
check "预领2件后库存-2" "$((STOCK0-STOCK1))" "2"
curl -s -X POST $BASE/schedules/$NS/cancel -H "$AUTH_H" -H "$CT" -d '{"reason":"患者改期"}' > /dev/null
STOCK2=$(curl -s $BASE/consumables -H "$AUTH_N" | python3 -c 'import sys,json;print([c["stock"] for c in json.load(sys.stdin) if c["code"]=="HC003"][0])')
check "取消排班后预领退回" "$STOCK2" "$STOCK0"
# 损耗登记触发低库存异常（护理包库存8<安全15，损耗1件再触发）
curl -s -X POST $BASE/consumables/txns -H "$AUTH_N" -H "$CT" \
  -d '{"consumableId":7,"type":"LOSS","quantity":1,"note":"包装破损"}' > /dev/null
SHORT=$(curl -s "$BASE/exceptions?status=OPEN" -H "$AUTH_N" | python3 -c '
import sys,json
print(sum(1 for e in json.load(sys.stdin) if e["type"]=="CONSUMABLE_SHORTAGE"))')
[ "$SHORT" -ge 1 ] && ok "低库存自动上报异常" || bad "低库存未生成异常"

echo "== 10. 机器故障 → 异常 =="
curl -s -X PUT $BASE/engineering/machines/6/status -H "$AUTH_E" -H "$CT" \
  -d '{"status":"FAULT","note":"血泵异响"}' > /dev/null
FAULT=$(curl -s "$BASE/exceptions?status=OPEN" -H "$AUTH_E" | python3 -c '
import sys,json
print(sum(1 for e in json.load(sys.stdin) if e["type"]=="MACHINE_FAULT"))')
[ "$FAULT" -ge 1 ] && ok "故障自动上报异常" || bad "故障未生成异常"
curl -s -X PUT $BASE/engineering/machines/6/status -H "$AUTH_E" -H "$CT" -d '{"status":"IDLE"}' > /dev/null

echo "== 11. 异常处理流转 =="
EID=$(curl -s "$BASE/exceptions?status=OPEN" -H "$AUTH_H" | python3 -c 'import sys,json;print(json.load(sys.stdin)[0]["id"])')
curl -s -X POST $BASE/exceptions/$EID/handle -H "$AUTH_H" -H "$CT" -d '{"status":"ACKNOWLEDGED","handleNote":"已知晓"}' > /dev/null
curl -s -X POST $BASE/exceptions/$EID/handle -H "$AUTH_H" -H "$CT" -d '{"status":"RESOLVED","handleNote":"已处理"}' > /dev/null
EST=$(curl -s "$BASE/exceptions?status=RESOLVED" -H "$AUTH_H" | python3 -c '
import sys,json
print(any(e["id"]=='"$EID"' for e in json.load(sys.stdin)))')
check "异常已解决" "$EST" "True"

echo "== 12. 管理报表 =="
REP=$(curl -s "$BASE/reports/overview?from=$(date -d '-6 days' +%F)&to=$TODAY" -H "$AUTH_A")
echo "$REP" | python3 -c '
import sys,json
d=json.load(sys.stdin)
u,e,c,dis=d["utilization"],d["exceptions"],d["consumableLoss"],d["disinfectionCompliance"]
rate, cnt = u["rate"], u["sessionCount"]
erate, etotal = e["rate"], e["total"]
print("  机位利用率 %s%%（%s台次） 异常率 %s%%（%s起） 耗材损耗 ¥%s 消毒合规 %s%%" % (
    rate, cnt, erate, etotal, c["amount"], dis["rate"]))
assert cnt > 0 and etotal > 0' && ok "报表四项指标齐全" || bad "报表指标缺失"

echo "== 13. 权限隔离 =="
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/billing" -H "$AUTH_N")
check "护士访问财务接口被拒(403)" "$CODE" "403"
CODE2=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/reports/overview?from=$TODAY&to=$TODAY" -H "$AUTH_N")
check "护士访问管理报表被拒(403)" "$CODE2" "403"

echo ""
echo "==================================="
echo "验证结果: 通过 $PASS 项, 失败 $FAIL 项"
[ $FAIL -eq 0 ] && echo "ALL CHECKS PASSED" || exit 1
