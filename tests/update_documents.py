"""Update the existing Word documents and export complete Wiki Markdown copies."""

from __future__ import annotations

import io
import json
import runpy
from pathlib import Path
from urllib.parse import quote

from PIL import Image
from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt
from docx.table import Table
from docx.text.paragraph import Paragraph


ROOT = Path(__file__).resolve().parent.parent
REQ = ROOT / "requirement" / "在线购物系统需求规格说明书.docx"
TEST = ROOT / "tests" / "在线购物系统首轮验收测试用例.docx"
IMAGES = ROOT / "process-and-state-diagrams"
FIXTURES = ROOT / "tests" / "验收测试数据"
GITHUB_RAW = "https://raw.githubusercontent.com/casf73/Software-Engineering-Practice/main/process-and-state-diagrams/"
CASE_TITLES = runpy.run_path(str(ROOT / "tests" / "create-fixtures.py"))["CASE_TITLES"]

TERMS = [
    ("买家（访客）", "无需注册即可浏览当前商品，并可提交、查询和在允许阶段撤销自己的购买意向。"),
    ("卖家", "唯一的后台操作人员，管理商品、等待队列及线下交易结果。"),
    ("当前商品", "卖家此时正在管理的唯一商品；交易成功或主动下架后才可发布下一件。"),
    ("购买意向", "买家针对当前商品填写姓名与联系电话形成的排队记录。"),
    ("队首", "等待队列中排位第一的有效购买意向；开始交易时由系统自动选取。"),
    ("FIFO", "先到先得；按服务端记录的有效提交时间排队，相同时间按提交顺序稳定排序。"),
    ("口令码", "与单条购买意向绑定的唯一查询凭据；意向结束时按规则失效，不是卖家账号。"),
    ("在售", "商品可展示且可接收新的购买意向的状态。"),
    ("手动冻结", "卖家主动暂停接收新意向，但保留等待队列和既有口令码的业务阶段。"),
    ("交易冻结", "卖家开始交易后，系统选中队首并暂停接收新意向的业务阶段。"),
    ("作废", "卖家使等待意向退出队列，或在交易失败时终止该意向；口令码随之失效。"),
    ("重新排队", "卖家将等待意向或交易失败后的意向移至队尾；同一意向和原口令码保持有效。"),
    ("主动下架", "卖家结束当前商品流程，停止接单并使相关口令码失效。"),
    ("交易流水", "卖家保存的交易结果及失败处置记录，用于追溯同一商品和意向的处理过程。"),
    ("首轮验收", "当前代码和测试优先验证的基础功能范围；完整需求中部分优化功能留待后续迭代。"),
]

FIGURES = [
    ("2 商品与上架管理", "业务流程图-商品与交易主流程.png", "图1 商品与交易主流程"),
    ("2.2 商品展示", "业务流程图-商品发布与媒体处理.png", "图2 商品发布与媒体处理流程"),
    ("4 交易、冻结与商品状态", "业务流程图-买家浏览、排队与口令码操作.png", "图3 买家浏览 排队与口令码操作流程"),
    ("4.2 开始交易", "状态图-商品状态.png", "图4 商品状态图"),
    ("4.4 手动冻结、解冻与主动下架", "状态图-购买意向状态.png", "图5 购买意向状态图"),
    ("6 口令码、个人信息与历史记录", "业务流程图-卖家后台与历史记录.png", "图6 卖家后台与历史记录流程"),
    ("6.2 历史商品及意向流水", "状态图-口令码有效性.png", "图7 口令码有效性状态图"),
]


def find_paragraph(doc: Document, text: str) -> Paragraph:
    for paragraph in doc.paragraphs:
        if paragraph.text.strip() == text:
            return paragraph
    raise ValueError(f"未找到文档位置：{text}")


def insert_paragraph_before(doc: Document, anchor: Paragraph, text: str = "", style: str = "Normal") -> Paragraph:
    paragraph = doc.add_paragraph(text, style=style)
    anchor._p.addprevious(paragraph._p)
    return paragraph


def set_repeat_header(row) -> None:
    trpr = row._tr.get_or_add_trPr()
    header = OxmlElement("w:tblHeader")
    header.set(qn("w:val"), "true")
    trpr.append(header)


def add_table_before(doc: Document, anchor: Paragraph, headers: list[str], rows: list[list[str]], widths: list[float]) -> Table:
    table = doc.add_table(rows=1, cols=len(headers))
    table.autofit = False
    table.style = "Table Grid"
    for index, value in enumerate(headers):
        table.rows[0].cells[index].text = value
        table.rows[0].cells[index].width = Inches(widths[index])
        for run in table.rows[0].cells[index].paragraphs[0].runs:
            run.bold = True
    set_repeat_header(table.rows[0])
    for values in rows:
        cells = table.add_row().cells
        for index, value in enumerate(values):
            cells[index].text = value
            cells[index].width = Inches(widths[index])
            for paragraph in cells[index].paragraphs:
                paragraph.paragraph_format.space_after = Pt(0)
                for run in paragraph.runs:
                    run.font.size = Pt(8.5)
    anchor._p.addprevious(table._tbl)
    return table


def figure_parts(image: Image.Image) -> list[Image.Image]:
    max_height = int(image.width * 1.12)
    if image.height <= max_height:
        return [image.copy()]
    return [image.crop((0, top, image.width, min(image.height, top + max_height)))
            for top in range(0, image.height, max_height)]


def update_requirement() -> None:
    doc = Document(REQ)
    if any(p.text.strip() == "术语表" for p in doc.paragraphs):
        return
    anchor = find_paragraph(doc, "1 项目概述")
    insert_paragraph_before(doc, anchor, "术语表", "Heading 1")
    insert_paragraph_before(doc, anchor, "下表统一本文使用的业务术语；具体状态转换和操作条件以各章节规则为准。")
    add_table_before(doc, anchor, ["术语", "定义"], [[term, meaning] for term, meaning in TERMS], [1.5, 5.2])

    for before_text, filename, caption in FIGURES:
        anchor = find_paragraph(doc, before_text)
        cap = insert_paragraph_before(doc, anchor, caption, "Caption")
        cap.paragraph_format.keep_with_next = True
        with Image.open(IMAGES / filename) as image:
            parts = figure_parts(image)
            for index, part in enumerate(parts, 1):
                if index > 1:
                    continuation = insert_paragraph_before(doc, anchor, f"{caption} 续 {index}/{len(parts)}", "Caption")
                    continuation.paragraph_format.keep_with_next = True
                para = insert_paragraph_before(doc, anchor)
                para.alignment = WD_ALIGN_PARAGRAPH.CENTER
                para.paragraph_format.space_after = Pt(7)
                data = io.BytesIO()
                part.save(data, format="PNG")
                data.seek(0)
                shape = para.add_run().add_picture(data, width=Inches(5.85))
                shape._inline.docPr.set("descr", filename)
    doc.save(REQ)


def update_test() -> None:
    doc = Document(TEST)
    if any(p.text.strip() == "测试实例总览" for p in doc.paragraphs):
        return
    doc.paragraphs[1].text = (
        "本文档列出首轮验收的 17 个测试用例：T01–T10 为后端自动化用例，"
        "U01–U07 为网页和持久化人工检查。现为每个用例建立 3 个独立测试实例，"
        "共 51 个实例；后端自动化执行其中 30 个功能实例，另校验全部 51 个实例文件的结构。"
    )
    doc.paragraphs[3].text = (
        "后端测试使用 MockMvc 和独立内存 H2 数据库。2026 年 10 月 14 日运行 30 个自动化功能实例"
        "及 1 个实例文件完整性检查，共 31 项通过、0 项失败。新增的 21 个 U 类页面与重启检查实例"
        "经用户确认，新增的 21 个 U 类页面与重启检查实例均已人工执行并通过。"
    )
    doc.paragraphs[4].text = (
        "自动化运行：在项目根目录执行 tests\\run-tests.cmd；脚本进入"
        " project\\mvp-v1.0\\backend 后运行 mvn test。"
        "测试实例文件位于 tests\\验收测试数据，测试数据库不会清空日常运行数据。"
    )
    doc.paragraphs[6].text = (
        "T01–T10 对应 StoreFlowIntegrationTest.java 中的 10 个参数化测试方法，"
        "每个方法读取本用例的 3 份 instance.json 并分别执行；每个实例执行前清理独立测试数据库。"
    )

    anchor = find_paragraph(doc, "执行记录说明")
    insert_paragraph_before(doc, anchor, "测试用例总览", "Heading 1")
    insert_paragraph_before(doc, anchor, "T 类为后端自动化，U 类为网页与持久化人工检查；每个编号下均有 3 个具体实例。")
    overview = [[case_id, title] for case_id, title in CASE_TITLES.items()]
    add_table_before(doc, anchor, ["编号", "用例内容"], overview, [0.65, 6.1])
    insert_paragraph_before(doc, anchor, "测试实例总览", "Heading 1")
    insert_paragraph_before(doc, anchor,
        "下表的文件路径以 tests/验收测试数据 为起点。T09 每个实例目录还包含 sample.png、"
        "sample.jpg、invalid.txt 和 oversized.png；U 类实例经用户确认均已测试通过。")
    rows = []
    for case_id in CASE_TITLES:
        for number in range(1, 4):
            relative = Path(case_id) / f"第{number}个测试实例" / "instance.json"
            data = json.loads((FIXTURES / relative).read_text(encoding="utf-8"))
            detail = data["title"]
            if data["mode"] == "automated":
                inputs = data.get("input", {})
                if "phonePrefix" in inputs:
                    detail += f"；电话前缀 {inputs['phonePrefix']}"
                elif "price" in inputs:
                    detail += f"；价格 {inputs['price']}"
                elif "firstImage" in inputs:
                    detail += f"；首图 {inputs['firstImage']}"
            else:
                detail += "；" + "；".join(data["steps"])
            rows.append([data["id"], detail, data["expected"], str(relative).replace("\\", "/")])
    add_table_before(doc, anchor, ["实例", "输入或操作", "预期结果", "实例文件"], rows,
                     [0.58, 2.4, 2.0, 1.85])
    doc.save(TEST)


def finalize_documents() -> None:
    """更新修订日期、人工实例状态，并防止实例表格的单行被分页拆开。"""
    requirement = Document(REQ)
    requirement.tables[0].cell(1, 1).text = "V1.4  |  2026年10月14日"
    for section in requirement.sections:
        for paragraph in section.footer.paragraphs:
            for run in paragraph.runs:
                run.text = run.text.replace("2026年9月29日", "2026年10月14日")
    requirement.save(REQ)

    test = Document(TEST)
    test.tables[0].cell(2, 1).text = "2026 年 10 月 14 日"
    overview = test.tables[1]
    if len(overview.columns) == 3:
        for row in overview.rows:
            row._tr.remove(row.cells[2]._tc)
        overview._tbl.tblGrid.remove(overview._tbl.tblGrid.gridCol_lst[2])
    overview._tbl.tblGrid.gridCol_lst[0].w = Inches(0.65)
    overview._tbl.tblGrid.gridCol_lst[1].w = Inches(6.1)
    for row in overview.rows:
        row.cells[0].width = Inches(0.65)
        row.cells[1].width = Inches(6.1)
    for row in test.tables[2].rows:
        trpr = row._tr.get_or_add_trPr()
        if not trpr.xpath("./w:cantSplit"):
            trpr.append(OxmlElement("w:cantSplit"))
    for paragraph in test.paragraphs:
        if paragraph.text.startswith("后端测试使用 MockMvc"):
            paragraph.text = (
                "后端测试使用 MockMvc 和独立内存 H2 数据库。2026 年 10 月 14 日运行 30 个自动化功能实例"
                "及 1 个实例文件完整性检查，共 31 项通过、0 项失败。经用户确认，新增的 21 个 U 类页面"
                "与重启检查实例也已全部人工执行并通过。"
            )
        elif paragraph.text.startswith("下表的文件路径以 tests/验收测试数据"):
            paragraph.text = (
                "下表的文件路径以 tests/验收测试数据 为起点。T09 每个实例目录还包含 sample.png、"
                "sample.jpg、invalid.txt 和 oversized.png；U 类 21 个实例经用户确认全部测试通过。"
            )
        elif paragraph.text.startswith("U01–U07 对应 tests/") or paragraph.text.startswith("U01–U07 是页面与持久化人工用例"):
            paragraph.text = (
                "U01–U07 是页面与持久化人工用例，每项包含 3 个具体实例。经用户确认，"
                "共 21 个实例已完成测试并全部通过；逐项截图和执行时间未提供。"
            )
        elif paragraph.text.startswith("源文件记录：通过") or paragraph.text.startswith("既有人工记录：通过"):
            paragraph.text = "实际结果：通过（用户确认对应的 3 个实例均已测试通过）。"
    test.save(TEST)


def markdown_table(table: Table) -> list[str]:
    values = [[cell.text.replace("\n", "<br>").replace("|", "\\|").strip() for cell in row.cells]
              for row in table.rows]
    if not values:
        return []
    return ["| " + " | ".join(values[0]) + " |",
            "| " + " | ".join("---" for _ in values[0]) + " |"] + [
            "| " + " | ".join(row) + " |" for row in values[1:]]


def export_markdown(path: Path) -> None:
    doc = Document(path)
    lines = []
    numbered = 0
    seen_images: set[str] = set()
    for child in doc.element.body:
        tag = child.tag.split("}")[-1]
        if tag == "tbl":
            lines.extend(markdown_table(Table(child, doc)))
            lines.append("")
            numbered = 0
            continue
        if tag != "p":
            continue
        para = Paragraph(child, doc)
        pictures = para._p.xpath(".//wp:docPr")
        if pictures:
            filename = pictures[0].get("descr")
            if filename and filename not in seen_images:
                seen_images.add(filename)
                lines.append(f"![{Path(filename).stem}]({GITHUB_RAW}{quote(filename)})")
                lines.append("")
            continue
        content = para.text.strip()
        if not content or content.startswith("图") and " 续 " in content:
            continue
        style = para.style.name
        if style == "Title":
            lines.append(f"# {content}")
        elif style == "Heading 1":
            lines.append(f"## {content}")
        elif style == "Heading 2":
            lines.append(f"### {content}")
        elif style == "Heading 3":
            lines.append(f"#### {content}")
        elif style == "Caption":
            lines.append(f"*{content}*")
        elif style == "List Bullet":
            lines.append(f"- {content}")
        elif style == "List Number":
            numbered += 1
            lines.append(f"{numbered}. {content}")
        else:
            numbered = 0
            lines.append(content)
        if style not in {"List Bullet", "List Number"}:
            lines.append("")
    output = path.with_suffix(".md")
    output.write_text("\n".join(lines).strip() + "\n", encoding="utf-8")
    print(f"Markdown: {output}")


def main() -> None:
    update_requirement()
    update_test()
    finalize_documents()
    export_markdown(REQ)
    export_markdown(TEST)
    print(f"Word: {REQ}")
    print(f"Word: {TEST}")


if __name__ == "__main__":
    main()
