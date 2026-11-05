"""Verify all 215 JUnit results against the checked-in case index (stdlib only)."""
from pathlib import Path
import json,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent
index=json.loads((root/'用例索引.json').read_text(encoding='utf-8'))
expected={case for api in index for case in api['cases']}
assert len(index)==43 and len(expected)==215
reports=root.parents[2]/'project/mvp-v1.0/backend/target/single-api-build/surefire-reports'
actual=set()
for report in reports.glob('TEST-cn.codelyy.store.singleapi.Api*Test.xml'):
 suite=ET.parse(report).getroot()
 assert int(suite.attrib['tests'])==5,report
 assert int(suite.attrib['failures'])==int(suite.attrib['errors'])==int(suite.attrib['skipped'])==0,report
 api=suite.attrib['name'].rsplit('.',1)[1][3:5]
 for case in suite.findall('testcase'):
  assert case.find('failure') is None and case.find('error') is None and case.find('skipped') is None
  actual.add(f'API-{api}-{case.attrib["name"].removeprefix("case")}')
assert actual==expected,(expected-actual,actual-expected)
records=[]
for case in sorted(expected):
 record=json.loads((root/'执行记录'/f'{case}.json').read_text(encoding='utf-8'))
 assert record['id']==case and record['result']=='通过',case
 assert record['requests'] and all(r['expectedStatus']==r['actualStatus'] for r in record['requests']),case
 records.append(record)
summary={'interfaces':43,'tests':215,'passed':215,'failed':0,'errors':0,'skipped':0,'verifiedAt':max(r['executedAt'] for r in records),'source':'JUnit XML + per-case execution records'}
(root/'执行记录/汇总.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False))
