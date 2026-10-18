#!/usr/bin/env python3
"""
院校分省录取分抓取脚本。

数据来源：掌上高考静态数据 static-data.gaokao.cn
  - 院校详情 {school_id}/info.json 的 `pro_type_min` 字段：
      按「省份 → 逐年 → 科类」给出最低分。云南新高考物理类 = 2073、历史类 = 2074；
      2024 及以前为旧高考理科 = 2、文科 = 1。
  - 该数据源**不提供位次（rank）**，故 rank 一律留空（严禁编造）。

输出：app/src/main/assets/school_admission_scores.json
  结构：{ version, source, data: { <schoolId>: { <省份名>: { physics:{min,rank?}, history:{min,rank?} } } } }
  与外层 SchoolAdmissionData 模型一致（data[院校id][省份名][科类]）。

用法：
  python3 tools/fetch_school_admission.py            # 抓取 C9 + 国防七子（约 15 所）
  python3 tools/fetch_school_admission.py --test 140 # 只抓 1 所，打印结果到 stdout 不写库
  python3 tools/fetch_school_admission.py --all      # 抓取 schools.json 全部院校（耗时长）

约束：抓取间隔 ≥ 1 秒；失败留空不编造；院校 id 与 schools.json 一致。
"""
import argparse
import json
import os
import sys
import time
import urllib.request

BASE = "https://static-data.gaokao.cn/www/2.0/school"
INFO_URL = f"{BASE}/{{sid}}/info.json"
UA = {"User-Agent": "Mozilla/5.0"}

PROVINCE_YUNNAN = "53"          # 掌上高考内部省份编码：云南
PROVINCE_NAME = "云南"

# 科类编码（掌上高考）：新高考物理类 / 历史类；旧高考理科 / 文科
PHYSICS_TYPES = ("2073", "2")
HISTORY_TYPES = ("2074", "1")

# C9 联盟 + 国防七子（与 tools/fetch_school_data.py 保持同一口径）
C9_SCHOOLS = {
    "清华大学", "北京大学", "复旦大学", "上海交通大学", "浙江大学",
    "中国科学技术大学", "南京大学", "西安交通大学", "哈尔滨工业大学",
}
DEFENSE_SEVEN_SCHOOLS = {
    "北京航空航天大学", "北京理工大学", "哈尔滨工业大学", "西北工业大学",
    "南京航空航天大学", "南京理工大学", "哈尔滨工程大学",
}
TARGET_SCHOOLS = C9_SCHOOLS | DEFENSE_SEVEN_SCHOOLS


def fetch(url, retries=3):
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=20) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception as exc:
            if attempt < retries - 1:
                time.sleep(1.0)
            else:
                print(f"    ! {url} -> {exc}", file=sys.stderr)
                return None


def pick_min(info, type_codes):
    """从 pro_type_min 的云南条目取给定科类的最新一年最低分。"""
    rows = (info.get("pro_type_min") or {}).get(PROVINCE_YUNNAN) or []
    best = None
    for row in rows:
        try:
            year = int(row.get("year"))
        except (TypeError, ValueError):
            continue
        for code in type_codes:
            value = (row.get("type") or {}).get(code)
            if value is None:
                continue
            try:
                score = int(float(value))
            except (TypeError, ValueError):
                continue
            # 取最新年份；同年多编码时取较小者（覆盖更全的招生口径）
            if best is None or year > best[0] or (year == best[0] and score < best[1]):
                best = (year, score)
    return best[1] if best else None


def build_entry(info):
    """构造该校在云南的录取分条目；无任何数据返回 None。"""
    physics = pick_min(info or {}, PHYSICS_TYPES)
    history = pick_min(info or {}, HISTORY_TYPES)
    entry = {}
    if physics is not None:
        # 数据源无位次：只写 min，不写 rank（Gson 反序列化时 rank 回落 null）
        entry["physics"] = {"min": physics}
    if history is not None:
        entry["history"] = {"min": history}
    return entry or None


def load_schools():
    path = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "schools.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    return data.get("schools", [])


def fetch_one(school):
    sid = str(school.get("id", ""))
    data = fetch(INFO_URL.format(sid=sid))
    info = data.get("data") if data and "data" in data else None
    return sid, build_entry(info)


def run(schools, args):
    result = {"version": "2026.10", "source": "", "data": {}}
    ok, fail = [], []
    for index, school in enumerate(schools, 1):
        sid = str(school.get("id", ""))
        name = school.get("name", "")
        entry = None
        try:
            sid, entry = fetch_one(school)
        except Exception as exc:  # noqa: BLE001
            print(f"  x {name}({sid}): {exc}", file=sys.stderr)
        if entry:
            result["data"][sid] = {PROVINCE_NAME: entry}
            ok.append(f"{name}({sid}) min={entry.get('physics', {}).get('min')}")
            print(f"  ok {name}({sid}) -> {entry}")
        else:
            fail.append(f"{name}({sid})")
            print(f"  -- {name}({sid}) 无云南录取数据", file=sys.stderr)
        if index < len(schools):
            time.sleep(1.0)  # 控速：间隔 ≥ 1 秒

    if args.test:
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    result["source"] = (
        "掌上高考 static-data.gaokao.cn 院校详情 info.json（pro_type_min）；"
        "仅最低分，无位次；抓取日期见提交"
    )
    out_path = os.path.join(
        os.path.dirname(__file__), "..", "app", "src", "main", "assets", "school_admission_scores.json"
    )
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(f"\n已写入 {os.path.normpath(out_path)}：成功 {len(ok)} / 失败 {len(fail)}")
    if fail:
        print("失败清单：" + "、".join(fail))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--test", metavar="SCHOOL_ID", help="只抓指定院校 id，输出到 stdout")
    parser.add_argument("--all", action="store_true", help="抓取 schools.json 全部院校")
    args = parser.parse_args()

    schools = load_schools()
    if args.test:
        targets = [s for s in schools if str(s.get("id")) == str(args.test)]
        if not targets:
            sys.exit(f"未在 schools.json 找到 id={args.test}")
    elif args.all:
        targets = schools
    else:
        targets = [s for s in schools if s.get("name") in TARGET_SCHOOLS]
        print(f"目标院校：C9 + 国防七子，共 {len(targets)} 所")
    run(targets, args)


if __name__ == "__main__":
    main()
