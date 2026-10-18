#!/usr/bin/env python3
"""
云南一分一段表抓取脚本（v6.1.6）。

数据源说明：
1) 优先尝试掌上高考 api.zjzw.cn 的 score section 接口（不同时期 uri 会变，失败自动跳过）。
2) 掌上高考公开接口对一分一段未返回数据时，回退到 gaokao-pro 项目已从省考试院/中国教育在线
   整理出的云南 2025 物理类/历史类全表（JSON 内含 source/source_url，可追溯）。
3) 抓取/解析失败直接跳过并返回非 0，不编造任何行。

输出：
  app/src/main/assets/score_section_yunnan.json               （App 使用：物理+历史+再选科目估算）
  app/src/main/assets/score_section_yunnan_2025_physics.json  （2025 物理原始表）
  app/src/main/assets/score_section_yunnan_2025_history.json  （2025 历史原始表）

注意：官方一分一段只有总分表，没有再选科目表；subjects 保留现有估算表并标记
subjectsEstimated=true，赋分界面需明确提示“估算”。
"""
import argparse
import json
import os
import sys
import time
import urllib.request

UA = {"User-Agent": "Mozilla/5.0", "Referer": "https://www.gaokao.cn/"}
API_CANDIDATES = [
    "https://api.zjzw.cn/web/api/?local_province_id=53&local_type_id={type_id}&year={year}&uri=apidata/api/gk/score/section",
    "https://api.zjzw.cn/web/api/?local_province_id=53&local_type_id={type_id}&year={year}&uri=apidata/api/gkv3/score/section",
]
GAOKAO_PRO_BASE = "https://raw.githubusercontent.com/HA7CH/gaokao-pro/main/cli/data/yifenyiduan"
TRACKS = {
    "physics": ("2073", "物理类"),
    "history": ("2074", "历史类"),
}


def fetch_json(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))


def try_api(year, track):
    type_id = TRACKS[track][0]
    for template in API_CANDIDATES:
        url = template.format(type_id=type_id, year=year)
        try:
            data = fetch_json(url)
            # 掌上高考通用返回：data.item，字段名可能是 score/count/cumulative
            items = ((data or {}).get("data") or {}).get("item") or []
            rows = []
            for item in items:
                score = int(item.get("score"))
                count = int(item.get("count", 0))
                cumulative = int(item.get("cumulative", 0))
                rows.append({"score": score, "count": count, "cumulative": cumulative})
            if rows:
                return rows, url
        except Exception as exc:  # noqa: BLE001
            print(f"  ! API 失败 {url}: {exc}", file=sys.stderr)
        time.sleep(1)
    return None, None


def fetch_gaokao_pro(year, track):
    url = f"{GAOKAO_PRO_BASE}/yunnan-{year}-{track}.json"
    data = fetch_json(url)
    rows = []
    for item in data.get("rows") or []:
        rows.append({
            "score": int(item["score"]),
            "count": int(item.get("count", 0)),
            "cumulative": int(item.get("cumulative", 0)),
        })
    return rows, data, url


def validate(rows, label):
    if not rows:
        raise ValueError(f"{label} 无数据")
    scores = [r["score"] for r in rows]
    if scores != sorted(scores, reverse=True):
        raise ValueError(f"{label} 分数未按降序排列")
    last = -1
    for row in rows:
        if row["count"] < 0 or row["cumulative"] < last:
            raise ValueError(f"{label} 本段/累计人数异常: {row}")
        last = row["cumulative"]
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, default=2025)
    args = parser.parse_args()
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out_dir = os.path.join(root, "app", "src", "main", "assets")
    os.makedirs(out_dir, exist_ok=True)

    combined_path = os.path.join(out_dir, "score_section_yunnan.json")
    existing = {}
    if os.path.exists(combined_path):
        try:
            with open(combined_path, encoding="utf-8") as f:
                existing = json.load(f)
        except Exception:  # noqa: BLE001
            existing = {}

    tables = {}
    sources = {}
    for track in ("physics", "history"):
        rows, meta = try_api(args.year, track)
        if rows:
            source = "掌上高考 api.zjzw.cn score section"
            source_url = meta
            rows = validate(rows, f"{args.year} {track}")
        else:
            print(f"  API 无数据，回退 gaokao-pro 真实整理数据：{args.year} {track}")
            rows, data, url = fetch_gaokao_pro(args.year, track)
            rows = validate(rows, f"{args.year} {track}")
            source = data.get("source", "gaokao-pro")
            source_url = data.get("source_url", url)
            meta = data
            time.sleep(1)
        tables[track] = {
            "total": rows[-1]["cumulative"],
            "sections": rows,
        }
        sources[track] = {
            "source": source,
            "source_url": source_url,
            "note": meta.get("note", "") if isinstance(meta, dict) else "",
        }
        # 每年/科类单独输出一份，便于追溯
        year_path = os.path.join(out_dir, f"score_section_yunnan_{args.year}_{track}.json")
        with open(year_path, "w", encoding="utf-8") as f:
            json.dump({
                "province": "云南",
                "year": args.year,
                "track": TRACKS[track][1],
                "total": rows[-1]["cumulative"],
                "sections": rows,
                **sources[track],
            }, f, ensure_ascii=False, indent=2)
        print(f"  ok {track}: {len(rows)} 行，total={rows[-1]['cumulative']}")

    combined = {
        "province": "云南",
        "year": args.year,
        "placeholder": False,
        "note": "物理/历史为真实 2025 一分一段表（省考试院/中国教育在线整理）；"
                "再选科目无官方一分一段，subjects 为估算表，赋分结果仅供参考。",
        "physics": tables["physics"],
        "history": tables["history"],
        "subjects": existing.get("subjects", {}),
        "subjectsEstimated": True,
        "sources": sources,
    }
    with open(combined_path, "w", encoding="utf-8") as f:
        json.dump(combined, f, ensure_ascii=False, indent=2)
    print(f"已写入 {combined_path}")
    if not combined["subjects"]:
        print("警告：无再选科目估算表，赋分工具将回退手动百分比模式", file=sys.stderr)


if __name__ == "__main__":
    main()
