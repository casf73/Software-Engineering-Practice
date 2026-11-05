# 第三轮单接口自动化测试

共43个接口，每接口5个独立JUnit测试，共215例。编号API-01-01～API-43-05，与第三轮Excel及Word中的单接口用例一致。

## 运行

在本目录双击`run-tests.cmd`，或在后端目录执行`mvn -Psingle-api-tests test`。
只运行某接口：`run-tests.cmd Api15Test`。
只运行某实例：`run-tests.cmd Api15Test#case01`。

每个API-xx目录中的Java文件包含5个真正执行HTTP请求并检查结果的测试方法，cases.json记录接口和用例说明。
公共代码SingleApiTestSupport.java负责Spring Boot/MockMvc、隔离数据准备、HTTP请求及执行记录。
Maven的single-api-tests配置直接编译本目录Java文件；无需复制到src/test/java。

## 环境与隔离

使用项目现有Java17、Maven、JUnit5、MockMvc及内存H2，所有请求经过真实控制器、鉴权和业务服务。
每例前清理测试数据，重置独立测试卖家密码和会话版本；预置数据及登录状态仅用于准备前置条件。
不连接日常文件数据库，不需要启动前后端或使用Postman。禁止并行运行同一内存库中的实例。
图片写入后端target/single-api-uploads，Maven构建输出在target/single-api-build。

## 执行记录

每例自动生成执行记录/API-xx-yy.json，含时间、结果、请求路径、预期/实际HTTP状态和响应。
Maven详细测试报告在后端target/single-api-build/surefire-reports。
执行记录只表示最近一次运行结果，失败不会写成通过；未运行的实例不能据此认定通过。
默认全量运行会由JUnit发现43个类、215个@Test方法，数量由verify-results.py进一步核验。

原R301～R308目录现在位于同级“综合接口测试”，由原run-iteration3-tests.cmd运行。

## 本次核验

2026-11-05：215个单接口测试全部通过，0失败、0错误、0跳过。原64个综合接口实例回归也全部通过。
可以从tests目录运行run-single-api-tests.cmd；执行记录与JUnit报告已逐例核对。
