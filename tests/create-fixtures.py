"""Generate the 17 x 3 acceptance-test instance files under tests/验收测试数据."""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parent / "验收测试数据"

CASE_TITLES = {
    "T01": "卖家登录、鉴权和退出",
    "T02": "商品校验、发布、编辑和多商品支持",
    "T03": "购买意向、口令码和先进先出队列",
    "T04": "交易冻结与撤销边界",
    "T05": "交易失败并作废",
    "T06": "交易失败并重新排队",
    "T07": "交易成功后的商品与意向收尾",
    "T08": "卖家主动下架",
    "T09": "图片上传格式大小及多图支持",
    "T10": "旧版多图记录的首轮展示",
    "U01": "买家空状态与无图商品",
    "U02": "买家表单与口令码查询",
    "U03": "队列顺序与队首交易",
    "U04": "冻结期间等待者与交易者的撤销",
    "U05": "服务重启后的数据保留",
    "U06": "页面上传限制与无图发布",
    "U07": "桌面与窄屏页面显示",
}

AUTOMATED = {
    "T01": [
        ("错误密码与未登录拦截", {"wrongPassword": "wrong-01"}, "未登录和错误密码均为 401；正确登录后可访问，退出后失效"),
        ("另一组错误密码", {"wrongPassword": "bad-password-02"}, "错误密码不能创建会话；正确登录及退出流程正常"),
        ("大小写错误密码", {"wrongPassword": "123456A"}, "密码区分大小写，错误登录 401，退出后后台仍为 401"),
    ],
    "T02": [
        ("商品甲发布与编辑", {"productName": "测试商品甲", "updatedName": "更新商品甲", "price": 88}, "必填校验 400；第二件商品发布 201；编辑成功；多个有效商品时 current 接口返回 409"),
        ("商品乙发布与编辑", {"productName": "测试商品乙", "updatedName": "更新商品乙", "price": 99}, "发布状态 ON_SALE；编辑和公开查询一致"),
        ("商品丙发布与编辑", {"productName": "测试商品丙", "updatedName": "更新商品丙", "price": 120}, "第二件商品发布 201；首件编辑成功"),
    ],
    "T03": [
        ("138 号码三人队列", {"phonePrefix": "1380000000"}, "唯一口令码；撤销、重排、作废后排位和有效性正确"),
        ("139 号码三人队列", {"phonePrefix": "1390000000"}, "按提交先后排队；撤销和作废使口令码失效"),
        ("137 号码三人队列", {"phonePrefix": "1370000000"}, "重新排队移至队尾并保留原口令码"),
    ],
    "T04": [
        ("138 号码冻结边界", {"phonePrefix": "1381000000"}, "只选队首；交易者不能撤销，等待者可以撤销"),
        ("139 号码冻结边界", {"phonePrefix": "1391000000"}, "交易中拒绝新意向和直接下架"),
        ("137 号码冻结边界", {"phonePrefix": "1371000000"}, "等待者撤销后后续排位前移"),
    ],
    "T05": [
        ("138 号码失败作废", {"phonePrefix": "1382000000"}, "商品恢复在售；失败买家原码失效；下一位仍排队"),
        ("139 号码失败作废", {"phonePrefix": "1392000000"}, "失败后不自动开始下一笔交易"),
        ("137 号码失败作废", {"phonePrefix": "1372000000"}, "卖家记录为 TRADE_FAILED，等待计数保持"),
    ],
    "T06": [
        ("138 号码失败重排", {"phonePrefix": "1383000000"}, "原码继续有效；原买家回到队尾"),
        ("139 号码失败重排", {"phonePrefix": "1393000000"}, "另一位买家升为队首；商品恢复在售"),
        ("137 号码失败重排", {"phonePrefix": "1373000000"}, "重排后没有进行中交易"),
    ],
    "T07": [
        ("138 号码成交收尾", {"phonePrefix": "1384000000"}, "成交商品下架；其余意向失败；所有口令码失效"),
        ("139 号码成交收尾", {"phonePrefix": "1394000000"}, "成交后可发布下一件商品"),
        ("137 号码成交收尾", {"phonePrefix": "1374000000"}, "卖家记录成功与失败状态正确"),
    ],
    "T08": [
        ("138 号码主动下架", {"phonePrefix": "1385000000"}, "商品不可浏览；等待意向口令码失效"),
        ("139 号码主动下架", {"phonePrefix": "1395000000"}, "下架后可发布下一件商品"),
        ("137 号码主动下架", {"phonePrefix": "1375000000"}, "相关意向记录仍存在于后端数据库"),
    ],
    "T09": [
        ("红色样图上传", {"png": "sample.png", "jpg": "sample.jpg", "invalid": "invalid.txt", "oversized": "oversized.png", "color": "red"}, "JPG/PNG 可上传；错误格式和超 5 MB 返回 400；两张图片可以关联保存"),
        ("绿色样图上传", {"png": "sample.png", "jpg": "sample.jpg", "invalid": "invalid.txt", "oversized": "oversized.png", "color": "green"}, "上传后返回 /uploads/ 地址，双图关联返回 200"),
        ("蓝色样图上传", {"png": "sample.png", "jpg": "sample.jpg", "invalid": "invalid.txt", "oversized": "oversized.png", "color": "blue"}, "JPG/PNG 可上传；错误格式和超 5 MB 返回 400；两张图片可以关联保存"),
    ],
    "T10": [
        ("旧数据两张图片", {"firstImage": "/uploads/old-first-a.jpg", "secondImage": "/uploads/old-second-a.jpg"}, "既有记录的两张图片全部返回，保持原图片顺序"),
        ("旧数据另一组图片", {"firstImage": "/uploads/old-first-b.png", "secondImage": "/uploads/old-second-b.png"}, "接口按原顺序返回两张图片"),
        ("旧数据混合格式", {"firstImage": "/uploads/old-first-c.jpg", "secondImage": "/uploads/old-second-c.png"}, "数据库旧记录保留，读取时完整返回图片列表"),
    ],
}

MANUAL = {
    "U01": [
        ("没有商品的首页", ["清空当前商品后打开买家首页"], "显示空状态，不要求登录"),
        ("无图商品页面", ["卖家发布不带图片的商品", "刷新买家首页"], "显示商品信息和无图占位"),
        ("有图商品页面", ["卖家发布一张图片的商品", "刷新买家首页"], "只显示这一张图片"),
    ],
    "U02": [
        ("缺少姓名", ["姓名留空、电话填写后点击提交"], "页面阻止提交并提示姓名必填"),
        ("缺少电话", ["填写姓名、电话留空后点击提交"], "页面阻止提交并提示电话必填"),
        ("完整提交与查询", ["填写姓名电话并提交", "保存口令码后凭码查询"], "口令码仅在成功提示中显示一次，可查询排位"),
    ],
    "U03": [
        ("两人顺序", ["两位买家依次提交", "卖家查看侧边目录中的队列"], "后台依提交顺序显示两人"),
        ("三人队首交易", ["三位买家依次提交", "卖家点击开始交易"], "只有第一位买家进入交易"),
        ("队首退出后的顺序", ["第二位买家撤销", "卖家查看剩余队列"], "后续买家排位前移"),
    ],
    "U04": [
        ("等待者撤销", ["开始队首交易", "等待买家凭码撤销"], "等待买家退出队列且原口令码失效"),
        ("交易者撤销", ["开始队首交易", "交易中买家尝试撤销"], "撤销被拒绝，交易仍保持"),
        ("新买家提交", ["开始队首交易", "新买家尝试提交意向"], "页面提示商品交易中，不接受新意向"),
    ],
    "U05": [
        ("商品重启保留", ["发布商品", "停止并重启后端", "重新打开买家页"], "当前商品仍存在"),
        ("队列重启保留", ["两位买家提交", "停止并重启后端", "查询两人排位"], "原意向及队列顺序保留"),
        ("口令码重启保留", ["记录有效口令码", "停止并重启后端", "凭原码查询"], "原码仍可查到同一意向"),
    ],
    "U06": [
        ("无图发布", ["卖家不选择图片并发布商品"], "商品可发布，买家页显示无图占位"),
        ("选择第二张图片", ["先选择一张图片，再尝试增加第二张"], "页面只允许保留一张图片"),
        ("非法图片与超限", ["选择 TXT 文件，再选择大于 5 MB 的图片"], "页面分别提示格式和大小错误"),
    ],
    "U07": [
        ("桌面买家页", ["设置浏览器宽度约 1440 像素", "查看买家商品页"], "内容可读、按钮可操作"),
        ("窄屏买家页", ["设置浏览器宽度约 375 像素", "查看买家商品页"], "无横向遮挡，表单可操作"),
        ("窄屏卖家页", ["设置浏览器宽度约 375 像素", "查看卖家登录页和侧边目录"], "登录和菜单可操作，内容不重叠"),
    ],
}


def write_instance(case_id: str, index: int, payload: dict) -> Path:
    folder = ROOT / case_id / f"第{index}个测试实例"
    folder.mkdir(parents=True, exist_ok=True)
    path = folder / "instance.json"
    # 保留人工检查已填写的实际结果；重复运行生成器时只补齐缺失实例。
    if not path.exists():
        path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return folder


def main() -> None:
    count = 0
    for case_id, instances in AUTOMATED.items():
        for index, (title, inputs, expected) in enumerate(instances, 1):
            folder = write_instance(case_id, index, {
                "id": f"{case_id}-{index:02d}", "caseId": case_id,
                "title": title, "mode": "automated", "input": inputs,
                "expected": expected,
            })
            if case_id == "T09":
                color = inputs["color"]
                Image.new("RGB", (16, 16), color).save(folder / inputs["png"], format="PNG")
                Image.new("RGB", (16, 16), color).save(folder / inputs["jpg"], format="JPEG")
                (folder / inputs["invalid"]).write_text("not an image\n", encoding="utf-8")
                with (folder / inputs["oversized"]).open("wb") as stream:
                    stream.write(b"\x89PNG\r\n\x1a\n")
                    stream.truncate(5 * 1024 * 1024 + 1)
            count += 1
    for case_id, instances in MANUAL.items():
        for index, (title, steps, expected) in enumerate(instances, 1):
            write_instance(case_id, index, {
                "id": f"{case_id}-{index:02d}", "caseId": case_id,
                "title": title, "mode": "manual", "steps": steps,
                "expected": expected, "actual": "待执行", "result": "未执行",
            })
            count += 1
    (ROOT / "README.md").write_text(
        "# 首轮验收测试实例\n\n"
        "本目录按 `用例编号/第n个测试实例/instance.json` 保存 17 个用例的 51 个实例。"
        "T01–T10 的 30 个实例由后端 JUnit 集成测试读取和执行；"
        "U01–U07 的 21 个实例用于人工页面与重启检查，执行后填写实际结果。"
        "T09 各实例目录另有真实 JPG、PNG、错误格式及超 5 MB 的上传样本。\n",
        encoding="utf-8",
    )
    print(f"生成 {count} 个实例：{ROOT}")


if __name__ == "__main__":
    main()
