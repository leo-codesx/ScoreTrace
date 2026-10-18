#!/usr/bin/env python3
"""
专业选科要求抓取脚本（v6.1.6）。

数据源：掌上高考 api.zjzw.cn `apidata/api/gk/score/special`（云南 2025 物理类）。
接口返回的 `sg_info` 形如「首选物理，再选化学」，`info/spname` 可能列出包含专业。
脚本按专业名称匹配 `majors_v2.json`，把解析出的 requiredSubjects 写入每个专业；
匹配不到的专业不写入、不猜测、不编造。

用法：
  python3 tools/fetch_major_requirements.py --test 140
  python3 tools/fetch_major_requirements.py --limit 30
  python3 tools/fetch_major_requirements.py --schools 140,143,30 --limit 3

约束：请求间隔 >= 1 秒；接口失败跳过并记录；只使用接口真实返回的选科文字。
"""
import argparse
import json
import os
import sys
import time
import urllib.request
from collections import Counter

UA = {"User-Agent": "Mozilla/5.0", "Referer": "https://www.gaokao.cn/"}
API = (
    "https://api.zjzw.cn/web/api/?local_batch_id=0&local_province_id=53"
    "&local_type_id=2073&page={page}&school_id={school_id}&size=20"
    "&uri=apidata/api/gk/score/special&year=2025"
)
SUBJECTS = ("物理", "历史", "化学", "生物", "政治", "地理")


def fetch_json(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))


def parse_requirements(text):
    if not text or "不限" in text:
        return []
    return [s for s in SUBJECTS if s in text]


def load_schools(root):
    path = os.path.join(root, "app", "src", "main", "assets", "schools.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    return data.get("schools", [])


def load_major_names(root):
    path = os.path.join(root, "app", "src", "main", "assets", "majors_v2.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    names = []
    for category in data.get("categories", []):
        for sub in category.get("subCategories", []):
            for major in sub.get("majors", []):
                if major.get("name"):
                    names.append(major["name"])
    return data, sorted(set(names), key=len, reverse=True)


def fetch_school(school_id):
    items = []
    page = 1
    while True:
        data = fetch_json(API.format(school_id=school_id, page=page))
        payload = (data or {}).get("data") or {}
        batch = payload.get("item") or []
        items.extend(batch)
        total = payload.get("numFound") or 0
        if not batch or len(items) >= total:
            break
        page += 1
        time.sleep(1)
    return items


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--test", metavar="SCHOOL_ID", help="只抓一所学校并打印解析结果")
    parser.add_argument("--schools", help="逗号分隔的学校 ID，覆盖默认学校集")
    parser.add_argument("--limit", type=int, default=30, help="批量抓取学校数量上限")
    args = parser.parse_args()
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    if args.test:
        items = fetch_school(args.test)
        print(f"school={args.test} items={len(items)}")
        for item in items[:5]:
            info = " ".join(str(item.get(k, "")) for k in ("spname", "info"))
            req = item.get("sp_info") or item.get("sg_info") or ""
            print(json.dumps({
                "spname": item.get("spname"),
                "info": item.get("info", "")[:120],
                "sg_info": item.get("sg_info"),
                "sp_info": item.get("sp_info"),
                "parsed": parse_requirements(req),
            }, ensure_ascii=False))
        return

    schools = load_schools(root)
    if args.schools:
        ids = [s.strip() for s in args.schools.split(",") if s.strip()]
        targets = [s for s in schools if str(s.get("id")) in ids]
    else:
        preferred = [s for s in schools if any(t in s.get("tags", []) for t in ("C9", "985", "211"))]
        targets = preferred[: args.limit]

    data, major_names = load_major_names(root)
    # 每个专业收集出现过的选科组合，最终取出现次数最多的一组
    votes = {name: Counter() for name in major_names}
    matched_rows = 0
    failed = []

    for index, school in enumerate(targets, 1):
        sid = str(school.get("id", ""))
        try:
            items = fetch_school(sid)
        except Exception as exc:  # noqa: BLE001
            failed.append(f"{school.get('name')}({sid}): {exc}")
            print(f"  x {school.get('name')}({sid}): {exc}", file=sys.stderr)
            time.sleep(1)
            continue
        for item in items:
            req_text = item.get("sp_info") or item.get("sg_info") or ""
            requirements = parse_requirements(req_text)
            combined = " ".join(str(item.get(k, "")) for k in ("spname", "info", "sp_info", "sg_info"))
            hit = False
            for name in major_names:
                if name and name in combined:
                    votes[name][tuple(requirements)] += 1
                    hit = True
            if hit:
                matched_rows += 1
        print(f"  {index}/{len(targets)} {school.get('name')} -> {len(items)} 条")
        time.sleep(1)

    assigned = 0
    for category in data.get("categories", []):
        for sub in category.get("subCategories", []):
            for major in sub.get("majors", []):
                name = major.get("name")
                choices = votes.get(name)
                if not choices:
                    continue
                requirements, _ = choices.most_common(1)[0]
                if requirements:
                    major["requiredSubjects"] = list(requirements)
                else:
                    major["requiredSubjects"] = []
                assigned += 1

    out_path = os.path.join(root, "app", "src", "main", "assets", "majors_v2.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"匹配行 {matched_rows}，写入专业 {assigned}/{len(major_names)}，失败学校 {len(failed)}")
    if failed:
        print("失败：" + "；".join(failed), file=sys.stderr)


if __name__ == "__main__":
    main()
